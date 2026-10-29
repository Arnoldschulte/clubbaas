package nl.clubbaas.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Waar de moraal van een speler naartoe beweegt, en waarom. Elke component is een aantal punten
 * boven of onder de basis van 60. De echte moraal schuift elke week 15% richting 'target'.
 */
public class MoraleBreakdown {
    public int playingTime;
    public int salary;
    public int jealousy;
    public int results;
    public int favorite;
    public int target;
    public final List<String> jealousOf = new ArrayList<>();
    public final List<String> reasons = new ArrayList<>();
}
