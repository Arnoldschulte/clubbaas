package nl.clubbaas.engine;

/** Antwoord van een verkopende club op een bod. */
public class NegotiationResult {
    public enum Outcome { ACCEPTED, COUNTER, REFUSED }

    public final Outcome outcome;
    public final long counter;   // alleen bij COUNTER
    public final String message;

    NegotiationResult(Outcome outcome, long counter, String message) {
        this.outcome = outcome;
        this.counter = counter;
        this.message = message;
    }
}
