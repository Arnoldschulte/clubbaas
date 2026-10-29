package nl.clubbaas.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Bekertoernooi met alle clubs. Is het aantal geen macht van 2, dan speelt eerst een
 * voorronde waarin een deel van de clubs een bye krijgt.
 */
public class Cup implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Nationale Beker";

    final List<Club> remaining = new ArrayList<>();
    public final List<String> roundNames = new ArrayList<>();
    public final List<List<Fixture>> rounds = new ArrayList<>();
    public Club winner;

    Cup(List<Club> clubs) {
        remaining.addAll(clubs);
    }

    public boolean isFinished() {
        return winner != null;
    }

    public boolean isStillIn(Club c) {
        return remaining.contains(c);
    }

    public int remainingCount() {
        return remaining.size();
    }

    static String nameForRound(int clubsInRound, boolean preliminary) {
        if (preliminary) return "Voorronde";
        switch (clubsInRound) {
            case 2: return "Finale";
            case 4: return "Halve finale";
            case 8: return "Kwartfinale";
            case 16: return "Achtste finale";
            default: return "Ronde van " + clubsInRound;
        }
    }

    /** Speelt de volgende ronde en geeft de gespeelde wedstrijden terug. */
    List<Fixture> playRound(int week, Random rng) {
        if (isFinished() || remaining.size() < 2) return Collections.emptyList();
        int n = remaining.size();
        int pow = Integer.highestOneBit(n);
        boolean preliminary = pow != n;
        int matches = preliminary ? n - pow : n / 2;

        List<Club> draw = new ArrayList<>(remaining);
        Collections.shuffle(draw, rng);
        List<Fixture> round = new ArrayList<>();
        boolean isFinal = n == 2;
        for (int i = 0; i < matches; i++) {
            Fixture f = new Fixture(draw.get(2 * i), draw.get(2 * i + 1), true, week);
            MatchEngine.play(f, rng, isFinal);
            remaining.remove(f.winner() == f.home ? f.away : f.home);
            round.add(f);
        }
        rounds.add(round);
        roundNames.add(nameForRound(n, preliminary));
        if (remaining.size() == 1) winner = remaining.get(0);
        return round;
    }
}
