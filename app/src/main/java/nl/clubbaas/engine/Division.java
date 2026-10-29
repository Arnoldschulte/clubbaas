package nl.clubbaas.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Eén divisie met clubs, speelschema en stand. */
public class Division implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int level;
    public final String name;
    public final List<Club> clubs = new ArrayList<>();
    private final List<List<Fixture>> rounds = new ArrayList<>();
    private final Map<Integer, TableRow> table = new HashMap<>();

    public Division(int level, String name) {
        this.level = level;
        this.name = name;
    }

    /** Dubbele halve competitie volgens de 'cirkelmethode'. */
    void createFixtures(Random rng) {
        rounds.clear();
        table.clear();
        for (Club c : clubs) table.put(c.id, new TableRow(c));

        List<Club> list = new ArrayList<>(clubs);
        Collections.shuffle(list, rng);
        int n = list.size();
        List<List<Fixture>> firstHalf = new ArrayList<>();
        for (int r = 0; r < n - 1; r++) {
            List<Fixture> round = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                Club a = list.get(i);
                Club b = list.get(n - 1 - i);
                boolean aHome = (r + i) % 2 == 0;
                round.add(new Fixture(aHome ? a : b, aHome ? b : a, false, r + 1));
            }
            firstHalf.add(round);
            // Roteren: eerste club blijft staan, de laatste schuift naar plek 2
            list.add(1, list.remove(n - 1));
        }
        rounds.addAll(firstHalf);
        for (int r = 0; r < firstHalf.size(); r++) {
            List<Fixture> mirrored = new ArrayList<>();
            for (Fixture f : firstHalf.get(r)) {
                mirrored.add(new Fixture(f.away, f.home, false, n + r));
            }
            rounds.add(mirrored);
        }
    }

    public int weeks() {
        return rounds.size();
    }

    public List<Fixture> round(int week) {
        if (week < 1 || week > rounds.size()) return Collections.emptyList();
        return rounds.get(week - 1);
    }

    public List<Fixture> fixturesOf(Club c) {
        List<Fixture> out = new ArrayList<>();
        for (List<Fixture> r : rounds) for (Fixture f : r) if (f.involves(c)) out.add(f);
        return out;
    }

    void record(Fixture f) {
        table.get(f.home.id).record(f.homeGoals, f.awayGoals);
        table.get(f.away.id).record(f.awayGoals, f.homeGoals);
    }

    public List<TableRow> sortedTable() {
        List<TableRow> rows = new ArrayList<>(table.values());
        Collections.sort(rows, Comparator
                .comparingInt((TableRow t) -> -t.points())
                .thenComparingInt(t -> -t.goalDifference())
                .thenComparingInt(t -> -t.goalsFor)
                .thenComparing(t -> t.club.name));
        return rows;
    }

    public int positionOf(Club c) {
        List<TableRow> rows = sortedTable();
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).club == c) return i + 1;
        return -1;
    }

    public List<Player> topScorers(int max) {
        List<Player> all = new ArrayList<>();
        for (Club c : clubs) for (Player p : c.squad) if (p.goals > 0) all.add(p);
        Collections.sort(all, (a, b) -> b.goals - a.goals);
        return all.subList(0, Math.min(max, all.size()));
    }

    public Club clubOf(Player p) {
        for (Club c : clubs) if (c.squad.contains(p)) return c;
        return null;
    }
}
