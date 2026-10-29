package nl.clubbaas.engine;

import java.io.Serializable;

/** Een gebeurtenis in een wedstrijd, voor het verslag en de tijdlijn. */
public class MatchEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { GOAL, YELLOW, SECOND_YELLOW, RED, CHANCE, INJURY }

    public final int minute;
    public final Type type;
    public final boolean home;     // gebeurtenis van het thuisteam?
    public final String player;

    public MatchEvent(int minute, Type type, boolean home, String player) {
        this.minute = minute;
        this.type = type;
        this.home = home;
        this.player = player;
    }

    public String icon() {
        switch (type) {
            case GOAL: return "⚽";
            case YELLOW: return "🟨";
            case SECOND_YELLOW: return "🟨🟥";
            case RED: return "🟥";
            case INJURY: return "🩹";
            default: return "•";
        }
    }
}
