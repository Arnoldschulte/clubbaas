package nl.clubbaas.engine;

import java.io.Serializable;

/** Eén regel in de clubhistorie. */
public class SeasonRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String season;
    public final String division;
    public final int position;
    public final String cupResult;
    public final String topScorer;
    public final String outcome; // "Kampioen", "Promotie", "Degradatie" of ""

    public SeasonRecord(String season, String division, int position, String cupResult,
                        String topScorer, String outcome) {
        this.season = season;
        this.division = division;
        this.position = position;
        this.cupResult = cupResult;
        this.topScorer = topScorer;
        this.outcome = outcome;
    }
}
