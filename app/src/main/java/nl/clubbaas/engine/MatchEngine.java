package nl.clubbaas.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        List<Player> fit = new ArrayList<>();
        for (Player p : club.squad) if (!p.isInjured()) fit.add(p);
        fit.sort(Comparator.comparingInt((Player p) -> -p.skill));

        // Voorkeursspeler van de voorzitter: zou de manager hem zelf ook opstellen?
        Player favorite = club.human ? club.favorite() : null;
        if (favorite != null && favorite.isInjured()) favorite = null;
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
                        : p.position == pos ? p.skill : p.skill * OUT_OF_POSITION;
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

    static void play(Fixture f, Random rng, boolean neutralGround) {
        TeamRating h = rate(f.home);
        TeamRating a = rate(f.away);
        Tactic th = tacticFor(f.home, h, a);
        Tactic ta = tacticFor(f.away, a, h);
        double[] lambda = expectedGoals(h, a, th, ta, neutralGround);

        f.homeGoals = poisson(lambda[0], rng);
        f.awayGoals = poisson(lambda[1], rng);
        f.homeSetup = h.formation.label + ", " + th.label.toLowerCase(Fmt.NL);
        f.awaySetup = a.formation.label + ", " + ta.label.toLowerCase(Fmt.NL);
        f.played = true;
        f.favoriteForced = (f.home.human && h.favoriteForced) || (f.away.human && a.favoriteForced);

        List<Player> homeXI = h.eleven();
        List<Player> awayXI = a.eleven();
        assignScorers(homeXI, f.homeGoals, f.homeScorers, rng);
        assignScorers(awayXI, f.awayGoals, f.awayScorers, rng);
        for (Player p : homeXI) p.appearances++;
        for (Player p : awayXI) p.appearances++;
        applyInjuries(f.home, homeXI, rng);
        applyInjuries(f.away, awayXI, rng);

        if (f.cup && f.homeGoals == f.awayGoals) {
            // Strafschoppen: lichte voorkeur voor het sterkere team
            double diff = h.overall() + (neutralGround ? 0 : HOME_ADVANTAGE) - a.overall();
            double pHome = clamp(0.5 + diff * 0.01, 0.3, 0.7);
            f.penaltyWinner = rng.nextDouble() < pHome ? f.home : f.away;
        }
    }

    private static void assignScorers(List<Player> eleven, int goals, List<String> out, Random rng) {
        if (eleven.isEmpty()) return;
        double[] weights = new double[eleven.size()];
        double sum = 0;
        for (int i = 0; i < eleven.size(); i++) {
            Player p = eleven.get(i);
            double w;
            switch (p.position) {
                case AANVALLER: w = 5; break;
                case MIDDENVELDER: w = 2; break;
                case VERDEDIGER: w = 0.5; break;
                default: w = 0.02;
            }
            weights[i] = w * (0.5 + p.skill / 100.0);
            sum += weights[i];
        }
        for (int g = 0; g < goals; g++) {
            double r = rng.nextDouble() * sum;
            int i = 0;
            while (i < weights.length - 1 && r > weights[i]) {
                r -= weights[i];
                i++;
            }
            Player scorer = eleven.get(i);
            scorer.goals++;
            out.add(scorer.name);
        }
    }

    private static void applyInjuries(Club club, List<Player> eleven, Random rng) {
        int physio = club.staffRating(StaffRole.FYSIO);
        double chance = INJURY_CHANCE * (1.3 - physio * 0.12);
        for (Player p : eleven) {
            if (rng.nextDouble() < chance) {
                int weeks = 1 + rng.nextInt(6) - (physio - 1) / 2;
                p.injuryWeeks = Math.max(1, weeks) + 1; // +1 omdat de teller vóór de volgende ronde zakt
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
