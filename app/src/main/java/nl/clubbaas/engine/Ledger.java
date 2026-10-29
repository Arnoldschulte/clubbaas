package nl.clubbaas.engine;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Kasboek: inkomsten (positief) en uitgaven (negatief) per categorie. */
public class Ledger implements Serializable {
    private static final long serialVersionUID = 1L;

    final LinkedHashMap<String, Long> week = new LinkedHashMap<>();
    final LinkedHashMap<String, Long> season = new LinkedHashMap<>();

    void add(String category, long amount) {
        Long w = week.get(category);
        week.put(category, (w == null ? 0 : w) + amount);
        Long s = season.get(category);
        season.put(category, (s == null ? 0 : s) + amount);
    }

    void startWeek() { week.clear(); }

    void startSeason() { season.clear(); }

    public Map<String, Long> week() { return week; }

    public Map<String, Long> season() { return season; }

    public static long total(Map<String, Long> map) {
        long t = 0;
        for (long v : map.values()) t += v;
        return t;
    }
}
