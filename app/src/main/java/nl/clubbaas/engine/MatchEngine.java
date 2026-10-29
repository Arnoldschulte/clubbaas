package nl.clubbaas.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Simuleert wedstrijden. De sterkte van een team is de gemiddelde vaardigheid van de
 * beste elf fitte spelers in een 1-4-4-2, plus een bonus voor de manager en thuisvoordeel.
 * Doelpunten worden getrokken uit een Poisson-verdeling.
 */
public final class MatchEngine {
    private static final int[] FORMATION = {1, 4, 4, 2}; // K, V, M, A
    private static final double HOME_ADVANTAGE = 3.0;
    private static final double INJURY_CHANCE = 0.012;

    private MatchEngine() {}

    /** Kiest de beste elf. Spelers buiten hun positie tellen voor 70%. */
    public static List<Player> selectEleven(Club club) {
        List<Player> fit = new ArrayList<>();
        for (Player p : club.squad) if (!p.isInjured()) fit.add(p);
        fit.sort(Comparator.comparingInt((Player p) -> -p.skill));

        List<Player> eleven = new ArrayList<>();
        Position[] positions = Position.values();
        for (int i = 0; i < positions.length; i++) {
            int needed = FORMATION[i];
            for (Player p : fit) {
                if (needed == 0) break;
                if (p.position == positions[i] && !eleven.contains(p)) {
                    eleven.add(p);
                    needed--;
                }
            }
        }
        // Gaten vullen met de beste overgebleven spelers
        for (Player p : fit) {
            if (eleven.size() >= 11) break;
            if (!eleven.contains(p)) eleven.add(p);
        }
        return eleven;
    }

    public static double strength(Club club) {
        List<Player> eleven = selectEleven(club);
        int[] slots = FORMATION.clone();
        double total = 0;
        for (Player p : eleven) {
            int idx = p.position.ordinal();
            if (slots[idx] > 0) {
                slots[idx]--;
                total += p.skill;
            } else {
                total += p.skill * 0.7; // speelt uit positie
            }
        }
        total += (11 - eleven.size()) * 10; // noodspelers als er te weinig fitte spelers zijn
        double managerBonus = (club.staffRating(StaffRole.MANAGER) - 3) * 1.5;
        return total / 11.0 + managerBonus;
    }

    static void play(Fixture f, Random rng, boolean neutralGround) {
        double sh = strength(f.home) + (neutralGround ? 0 : HOME_ADVANTAGE);
        double sa = strength(f.away);
        double diff = sh - sa;
        double lambdaHome = clamp(1.30 * Math.exp(diff * 0.045), 0.15, 5.0);
        double lambdaAway = clamp(1.10 * Math.exp(-diff * 0.045), 0.15, 5.0);

        f.homeGoals = poisson(lambdaHome, rng);
        f.awayGoals = poisson(lambdaAway, rng);
        f.played = true;

        List<Player> homeXI = selectEleven(f.home);
        List<Player> awayXI = selectEleven(f.away);
        assignScorers(homeXI, f.homeGoals, f.homeScorers, rng);
        assignScorers(awayXI, f.awayGoals, f.awayScorers, rng);
        for (Player p : homeXI) p.appearances++;
        for (Player p : awayXI) p.appearances++;
        applyInjuries(f.home, homeXI, rng);
        applyInjuries(f.away, awayXI, rng);

        if (f.cup && f.homeGoals == f.awayGoals) {
            // Strafschoppen: lichte voorkeur voor het sterkere team
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
