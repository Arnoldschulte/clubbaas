package nl.clubbaas.engine;

/** Speelposities. De volgorde bepaalt ook de sortering in de selectie. */
public enum Position {
    DOELMAN("K", "Keeper"),
    VERDEDIGER("V", "Verdediger"),
    MIDDENVELDER("M", "Middenvelder"),
    AANVALLER("A", "Aanvaller");

    public final String code;
    public final String label;

    Position(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
