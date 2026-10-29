package nl.clubbaas.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Simuleert wedstrijden.
 *
 * 1. Opstelling: per formatie de beste fitte spelers op hun eigen positie; lege plekken worden
 *    gevuld met de beste overgebleven speler, die dan voor 70% telt.
 * 2. Aanval en verdediging zijn gewogen gemiddelden van de vaardigheid, afhankelijk van de rol
 *    (zie WEIGHTS): keeper en verdedigers bepalen vooral de verdediging, spitsen vooral de aanval.
 *    Een verdedigende formatie (bijv. 5-3-2) schuift daarna een paar punten van aanval naar
 *    verdediging, een aanvallende (4-3-3) andersom.
 * 3. Verwachte doelpunten = basis × e^(K × (eigen aanval − verdediging tegenstander)),
 *    vermenigvuldigd met de speelwijze van beide teams. Het echte aantal komt uit een Poisson-verdeling.
 */
public final class MatchEngine {
    private static final double HOME_ADVANTAGE = 3.0;
    private static final double INJURY_CHANCE = 0.012;
    private static final double K = 0.045;
    private static final double OUT_OF_POSITION = 0.7;
    private static final int EMERGENCY_SKILL = 10;

    /** Bijdrage per rol: {verdediging, aanval}. Volgorde = Position: K, V, M, A. */
    private static final double[][] WEIGHTS = {
            {1.00, 0.00},
            {0.90, 0.15},
            {0.35, 0.50},
            {0.05, 1.00}};

    /** Verschuiving per stap van de formatie weg van 4-4-2 (verdedigender of aanvallender). */
    private static final double SHAPE_SHIFT = 1.5;

    private MatchEngine() {}

    // ---- Opstelling en sterkte ---------------------------------------------------------

    public static TeamRating rate(Club club) {
        return rate(club, club.formation());
    }

    public static TeamRating rate(Club club, Formation formation) {
        return rate(club, formation, true);
    }

    /** Effect van moraal op de vaardigheid in een wedstrijd: van ongeveer −7,5% tot +5%. */
    public static double moraleFactor(Player p) {
        return 1.0 + (p.morale() - Player.BASE_MORALE) / 800.0;
    }

    // Conditie
    static final int MATCH_FATIGUE = 16;
    static final int MAX_FATIGUE = 60;
    static final int BASE_RECOVERY = 8;
    static final double RECOVERY_SHARE = 0.3;

    /** Effect van conditie: topfit = 100%, bij 40% conditie nog 91%. */
    public static double fitnessFactor(Player p) {
        return 0.85 + 0.15 * p.fitness() / 100.0;
    }

    /** Wat de manager ziet bij het opstellen: vaardigheid, rekening houdend met conditie. */
    private static double selectionValue(Player p, boolean useFitness) {
        return useFitness ? p.skill * fitnessFactor(p) : p.skill;
    }

    static TeamRating rate(Club club, Formation formation, boolean useFavorite) {
        return rate(club, formation, useFavorite, true);
    }

    /**
     * @param useFavorite false = de opstelling die de manager zelf zou kiezen, zonder bemoeienis
     *                    van de voorzitter (gebruikt om te bepalen wie verwacht te spelen).
     * @param useFitness  false = conditie negeren (wie is op papier basisspeler?)
     */
    static TeamRating rate(Club club, Formation formation, boolean useFavorite, boolean useFitness) {
        List<Player> fit = new ArrayList<>();
        for (Player p : club.squad) if (p.isAvailable()) fit.add(p);
        fit.sort((x, y) -> Double.compare(selectionValue(y, useFitness), selectionValue(x, useFitness)));

        // Voorkeursspeler van de voorzitter: zou de manager hem zelf ook opstellen?
        Player favorite = club.human && useFavorite ? club.favorite() : null;
        if (favorite != null && !favorite.isAvailable()) favorite = null;
        boolean forced = false;
        if (favorite != null) {
            int better = 0;
            for (Player p : fit) {
                if (p == favorite) break;
                if (p.position == favorite.position) better++;
            }
            forced = better >= formation.count(favorite.position);
        }

        // Eerst iedereen op zijn eigen positie
        List<Player> used = new ArrayList<>();
        Player[][] chosen = new Player[Position.values().length][];
        for (Position pos : Position.values()) {
            chosen[pos.ordinal()] = new Player[formation.count(pos)];
            int i = 0;
            if (forced && pos == favorite.position) {
                chosen[pos.ordinal()][i++] = favorite;
                used.add(favorite);
            }
            for (Player p : fit) {
                if (i >= chosen[pos.ordinal()].length) break;
                if (p.position == pos && !used.contains(p)) {
                    chosen[pos.ordinal()][i++] = p;
                    used.add(p);
                }
            }
        }
        // Daarna gaten vullen met de beste overgebleven spelers
        List<TeamRating.Slot> slots = new ArrayList<>();
        double def = 0, att = 0, defWeight = 0, attWeight = 0;
        for (Position pos : Position.values()) {
            for (int i = 0; i < chosen[pos.ordinal()].length; i++) {
                Player p = chosen[pos.ordinal()][i];
                if (p == null) {
                    for (Player candidate : fit) {
                        if (!used.contains(candidate)) {
                            p = candidate;
                            used.add(candidate);
                            break;
                        }
                    }
                }
                double eff = p == null ? EMERGENCY_SKILL
                        : (p.position == pos ? p.skill : p.skill * OUT_OF_POSITION) * moraleFactor(p)
                        * (useFitness ? fitnessFactor(p) : 1.0);
                def += eff * WEIGHTS[pos.ordinal()][0];
                att += eff * WEIGHTS[pos.ordinal()][1];
                defWeight += WEIGHTS[pos.ordinal()][0];
                attWeight += WEIGHTS[pos.ordinal()][1];
                slots.add(new TeamRating.Slot(pos, p));
            }
        }
        // Gewogen gemiddelden: bij gelijke spelers zijn aanval en verdediging gelijk aan hun vaardigheid,
        // in elke formatie. De formatie verschuift daarna een klein, gelijk bedrag tussen beide.
        // 'shape' > 0 is verdedigend (meer verdedigers / minder aanvallers dan 4-4-2), < 0 aanvallend.
        int shape = (formation.defenders - 4) - (formation.attackers - 2);
        double manager = (club.staffRating(StaffRole.MANAGER) - 3) * 1.5;
        double attack = att / attWeight - shape * SHAPE_SHIFT + manager;
        double defense = def / defWeight + shape * SHAPE_SHIFT + manager;
        return new TeamRating(formation, slots, attack, defense, forced);
    }

    /** Algemene teamsterkte (gemiddelde van aanval en verdediging). */
    public static double strength(Club club) {
        return rate(club).overall();
    }

    public static List<Player> selectEleven(Club club) {
        return rate(club).eleven();
    }

    /** De eigen club speelt zoals ingesteld; AI-clubs passen hun speelwijze aan de tegenstander aan. */
    static Tactic tacticFor(Club club, TeamRating own, TeamRating opponent) {
        if (club.human) return club.tactic();
        double diff = own.overall() - opponent.overall();
        if (diff > 5) return Tactic.AANVALLEND;
        if (diff < -5) return Tactic.VERDEDIGEND;
        return Tactic.NORMAAL;
    }

    /** Verwachte doelpunten {thuis, uit}, ook bruikbaar voor een voorspelling in de UI. */
    static double[] expectedGoals(TeamRating h, TeamRating a, Tactic th, Tactic ta, boolean neutral) {
        double adv = neutral ? 0 : HOME_ADVANTAGE;
        double lambdaHome = 1.30 * Math.exp(K * (h.attack + adv - a.defense)) * th.scoring * ta.conceding;
        double lambdaAway = 1.10 * Math.exp(K * (a.attack - h.defense - adv)) * ta.scoring * th.conceding;
        return new double[]{clamp(lambdaHome, 0.15, 5.0), clamp(lambdaAway, 0.15, 5.0)};
    }

    // ---- Wedstrijd ---------------------------------------------------------------------

    // Kaarten
    static final double YELLOW_CHANCE = 0.07;
    static final double SECOND_YELLOW_CHANCE = 0.06;
    static final double RED_CHANCE = 0.006;
    static final int YELLOWS_FOR_SUSPENSION = 5;
    /** Een rode kaart in minuut 1 kost 30% van de eigen kansen (en geeft de tegenstander 30% meer). */
    private static final double RED_CARD_IMPACT = 0.30;

    static void play(Fixture f, Random rng, boolean neutralGround) {
        // Wie nu geschorst is, zit deze wedstrijd uit
        List<Player> servingHome = suspendedPlayers(f.home);
        List<Player> servingAway = suspendedPlayers(f.away);

        TeamRating h = rate(f.home);
        TeamRating a = rate(f.away);
        Tactic th = tacticFor(f.home, h, a);
        Tactic ta = tacticFor(f.away, a, h);
        double[] lambda = expectedGoals(h, a, th, ta, neutralGround);
        f.homeSetup = h.formation.label + ", " + th.label.toLowerCase(Fmt.NL);
        f.awaySetup = a.formation.label + ", " + ta.label.toLowerCase(Fmt.NL);
        f.favoriteForced = (f.home.human && h.favoriteForced) || (f.away.human && a.favoriteForced);

        List<Player> homeXI = h.eleven();
        List<Player> awayXI = a.eleven();

        // Kaarten eerst: een rode kaart verandert de rest van de wedstrijd
        Map<Player, Integer> sentOffHome = new HashMap<>();
        Map<Player, Integer> sentOffAway = new HashMap<>();
        dealCards(f, homeXI, true, sentOffHome, rng);
        dealCards(f, awayXI, false, sentOffAway, rng);
        double homeRed = redPenalty(sentOffHome);
        double awayRed = redPenalty(sentOffAway);
        double lambdaHome = lambda[0] * (1 - homeRed) * (1 + awayRed);
        double lambdaAway = lambda[1] * (1 - awayRed) * (1 + homeRed);

        f.homeGoals = poisson(lambdaHome, rng);
        f.awayGoals = poisson(lambdaAway, rng);
        f.played = true;

        assignGoals(f, homeXI, sentOffHome, f.homeGoals, true, f.homeScorers, rng);
        assignGoals(f, awayXI, sentOffAway, f.awayGoals, false, f.awayScorers, rng);
        for (Player p : homeXI) addMatchLoad(p);
        for (Player p : awayXI) addMatchLoad(p);
        applyInjuries(f, f.home, homeXI, true, rng);
        applyInjuries(f, f.away, awayXI, false, rng);
        f.events().sort((x, y) -> x.minute - y.minute);

        if (f.cup && f.homeGoals == f.awayGoals) {
            // Strafschoppen: lichte voorkeur voor het sterkere team
            double diff = h.overall() + (neutralGround ? 0 : HOME_ADVANTAGE) - a.overall();
            double pHome = clamp(0.5 + diff * 0.01, 0.3, 0.7);
            f.penaltyWinner = rng.nextDouble() < pHome ? f.home : f.away;
        }

        // Schorsing uitgezeten
        for (Player p : servingHome) p.suspension--;
        for (Player p : servingAway) p.suspension--;
    }

    /** Een gespeelde wedstrijd: telt mee als optreden en kost conditie (meer bij oudere spelers). */
    private static void addMatchLoad(Player p) {
        p.appearances++;
        int load = MATCH_FATIGUE + Math.max(0, p.age - 30) / 2;
        p.fatigue = Math.min(MAX_FATIGUE, p.fatigue + load);
    }

    /**
     * Wekelijks herstel: een vast deel plus 30% van de vermoeidheid. Bij één wedstrijd per week
     * blijft een speler zo rond de 90% conditie; extra (beker)wedstrijden hakken erin.
     * Een goede fysio laat spelers sneller herstellen.
     */
    static void recover(Club club) {
        int base = BASE_RECOVERY + club.staffRating(StaffRole.FYSIO);
        for (Player p : club.squad) {
            int recovery = base + (int) Math.round(p.fatigue * RECOVERY_SHARE);
            p.fatigue = Math.max(0, p.fatigue - recovery);
        }
    }

    private static List<Player> suspendedPlayers(Club c) {
        List<Player> out = new ArrayList<>();
        for (Player p : c.squad) if (p.isSuspended()) out.add(p);
        return out;
    }

    /** Verdedigers pakken de meeste kaarten, keepers de minste. */
    private static double cardWeight(Position p) {
        switch (p) {
            case VERDEDIGER: return 1.3;
            case MIDDENVELDER: return 1.0;
            case AANVALLER: return 0.7;
            default: return 0.3;
        }
    }

    private static void dealCards(Fixture f, List<Player> eleven, boolean home,
                                  Map<Player, Integer> sentOff, Random rng) {
        for (Player p : eleven) {
            double w = cardWeight(p.position);
            if (rng.nextDouble() < RED_CHANCE * w) {
                int minute = 1 + rng.nextInt(90);
                f.events().add(new MatchEvent(minute, MatchEvent.Type.RED, home, p.name));
                sentOff.put(p, minute);
                p.suspension += 2;
                continue;
            }
            if (rng.nextDouble() < YELLOW_CHANCE * w) {
                int minute = 1 + rng.nextInt(88);
                f.events().add(new MatchEvent(minute, MatchEvent.Type.YELLOW, home, p.name));
                p.yellowCards++;
                if (p.yellowCards % YELLOWS_FOR_SUSPENSION == 0) p.suspension += 1;
                if (rng.nextDouble() < SECOND_YELLOW_CHANCE) {
                    int second = minute + 1 + rng.nextInt(90 - minute);
                    f.events().add(new MatchEvent(second, MatchEvent.Type.SECOND_YELLOW, home, p.name));
                    sentOff.put(p, second);
                    p.suspension += 1;
                }
            }
        }
    }

    private static double redPenalty(Map<Player, Integer> sentOff) {
        double penalty = 0;
        for (int minute : sentOff.values()) penalty += RED_CARD_IMPACT * (90 - minute) / 90.0;
        return Math.min(0.6, penalty);
    }

    private static double scoringWeight(Player p) {
        double w;
        switch (p.position) {
            case AANVALLER: w = 5; break;
            case MIDDENVELDER: w = 2; break;
            case VERDEDIGER: w = 0.5; break;
            default: w = 0.02;
        }
        return w * (0.5 + p.skill / 100.0);
    }

    /** Verdeelt de doelpunten over minuten en spelers die op dat moment nog op het veld staan. */
    private static void assignGoals(Fixture f, List<Player> eleven, Map<Player, Integer> sentOff,
                                    int goals, boolean home, List<String> out, Random rng) {
        if (eleven.isEmpty()) return;
        int[] minutes = new int[goals];
        for (int g = 0; g < goals; g++) minutes[g] = 1 + rng.nextInt(90);
        java.util.Arrays.sort(minutes);
        for (int minute : minutes) {
            List<Player> onPitch = new ArrayList<>();
            for (Player p : eleven) {
                Integer off = sentOff.get(p);
                if (off == null || off > minute) onPitch.add(p);
            }
            if (onPitch.isEmpty()) onPitch = eleven;
            double sum = 0;
            for (Player p : onPitch) sum += scoringWeight(p);
            double r = rng.nextDouble() * sum;
            Player scorer = onPitch.get(onPitch.size() - 1);
            for (Player p : onPitch) {
                r -= scoringWeight(p);
                if (r <= 0) {
                    scorer = p;
                    break;
                }
            }
            scorer.goals++;
            out.add(scorer.name);
            f.events().add(new MatchEvent(minute, MatchEvent.Type.GOAL, home, scorer.name));
        }
    }

    private static void applyInjuries(Fixture f, Club club, List<Player> eleven, boolean home, Random rng) {
        int physio = club.staffRating(StaffRole.FYSIO);
        double chance = INJURY_CHANCE * (1.3 - physio * 0.12);
        for (Player p : eleven) {
            // Vermoeide spelers raken eerder geblesseerd
            if (rng.nextDouble() < chance * (1 + p.fatigue / 40.0)) {
                int weeks = 1 + rng.nextInt(6) - (physio - 1) / 2;
                p.injuryWeeks = Math.max(1, weeks) + 1; // +1 omdat de teller vóór de volgende ronde zakt
                f.events().add(new MatchEvent(1 + rng.nextInt(90), MatchEvent.Type.INJURY, home, p.name));
            }
        }
    }

    static int poisson(double lambda, Random rng) {
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1;
        do {
            k++;
            p *= rng.nextDouble();
        } while (p > l);
        return k - 1;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
