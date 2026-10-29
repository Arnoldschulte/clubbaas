package nl.clubbaas.engine;

import java.util.Locale;

/** Opmaakhulpjes voor geldbedragen en getallen (Nederlandse notatie). */
public final class Fmt {
    public static final Locale NL = Locale.forLanguageTag("nl-NL");

    private Fmt() {}

    public static String money(long v) {
        String sign = v < 0 ? "-" : "";
        long a = Math.abs(v);
        if (a >= 1_000_000) {
            return sign + String.format(NL, "€ %.2f mln", a / 1_000_000.0);
        }
        return sign + String.format(NL, "€ %,d", a);
    }

    public static String number(long v) {
        return String.format(NL, "%,d", v);
    }

    public static String seasonLabel(int startYear) {
        return startYear + "/" + String.format(NL, "%02d", (startYear + 1) % 100);
    }
}
