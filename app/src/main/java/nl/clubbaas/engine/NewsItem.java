package nl.clubbaas.engine;

import java.io.Serializable;

public class NewsItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String season;
    public final int week;
    public final String text;

    public NewsItem(String season, int week, String text) {
        this.season = season;
        this.week = week;
        this.text = text;
    }
}
