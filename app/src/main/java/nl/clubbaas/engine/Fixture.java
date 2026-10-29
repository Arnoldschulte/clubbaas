package nl.clubbaas.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Eén wedstrijd (competitie of beker). */
public class Fixture implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Club home;
    public final Club away;
    public final boolean cup;
    public final int week;
    public int homeGoals;
    public int awayGoals;
    public boolean played;
    public Club penaltyWinner;  // alleen bij een gelijkspel in de beker
    public int attendance;      // alleen ingevuld bij thuiswedstrijden van de eigen club
    public String homeSetup;    // bijv. "4-3-3, aanvallend"
    public String awaySetup;
    public final List<String> homeScorers = new ArrayList<>();
    public final List<String> awayScorers = new ArrayList<>();

    public Fixture(Club home, Club away, boolean cup, int week) {
        this.home = home;
        this.away = away;
        this.cup = cup;
        this.week = week;
    }

    public boolean involves(Club c) {
        return home == c || away == c;
    }

    public Club opponentOf(Club c) {
        return home == c ? away : home;
    }

    public Club winner() {
        if (!played) return null;
        if (homeGoals > awayGoals) return home;
        if (awayGoals > homeGoals) return away;
        return penaltyWinner;
    }

    public String scoreText() {
        if (!played) return "-";
        String s = homeGoals + " - " + awayGoals;
        if (penaltyWinner != null) s += " (pen.)";
        return s;
    }

    /** "W", "G" of "V" vanuit het perspectief van club c. */
    public String resultFor(Club c) {
        if (!played) return "";
        Club w = winner();
        if (w == null) return "G";
        return w == c ? "W" : "V";
    }

    public String describe() {
        return home.name + " " + scoreText() + " " + away.name;
    }
}
