package nl.clubbaas.engine;

import java.io.Serializable;

public class TableRow implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Club club;
    public int played, won, drawn, lost, goalsFor, goalsAgainst;

    public TableRow(Club club) {
        this.club = club;
    }

    void record(int scored, int conceded) {
        played++;
        goalsFor += scored;
        goalsAgainst += conceded;
        if (scored > conceded) won++;
        else if (scored == conceded) drawn++;
        else lost++;
    }

    public int points() {
        return won * 3 + drawn;
    }

    public int goalDifference() {
        return goalsFor - goalsAgainst;
    }
}
