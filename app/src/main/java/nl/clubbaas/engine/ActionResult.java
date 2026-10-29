package nl.clubbaas.engine;

/** Uitkomst van een actie van de voorzitter, met een bericht voor de speler. */
public class ActionResult {
    public final boolean ok;
    public final String message;

    private ActionResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static ActionResult ok(String msg) { return new ActionResult(true, msg); }

    public static ActionResult fail(String msg) { return new ActionResult(false, msg); }
}
