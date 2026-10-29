package nl.clubbaas.engine;

/** Een aanbod van een club om een speler te huren. */
public class LoanOffer {
    public final Club club;
    public final int wageSharePct;
    public final boolean wouldStart;

    LoanOffer(Club club, int wageSharePct, boolean wouldStart) {
        this.club = club;
        this.wageSharePct = wageSharePct;
        this.wouldStart = wouldStart;
    }
}
