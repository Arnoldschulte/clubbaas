package nl.clubbaas.engine;

import java.util.ArrayList;
import java.util.List;

/** Wat er deze speelweek is gebeurd, voor de UI. */
public class WeekReport {
    public Fixture leagueMatch;
    public Fixture cupMatch;
    public String cupRoundName;
    public Fixture europeMatch;
    public String europeRoundName;
    public final List<String> messages = new ArrayList<>();
    public SeasonSummary seasonSummary; // gevuld na de laatste speelronde
    public boolean newSeasonStarted;
    public boolean gameOver;
}
