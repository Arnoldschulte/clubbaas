package nl.clubbaas.engine;

import java.io.Serializable;

/** Een uitgeleende speler. Hij speelt tot het eind van het seizoen bij een andere club. */
public class Loan implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Player player;
    public final Club club;
    public final int wageSharePct;   // deel van het salaris dat de huurder betaalt
    public final int appearancesAtStart;
    public final int goalsAtStart;

    Loan(Player player, Club club, int wageSharePct) {
        this(player, club, wageSharePct, player.appearances, player.goals);
    }

    /** Voor het inlezen van een savegame. */
    Loan(Player player, Club club, int wageSharePct, int appearancesAtStart, int goalsAtStart) {
        this.player = player;
        this.club = club;
        this.wageSharePct = wageSharePct;
        this.appearancesAtStart = appearancesAtStart;
        this.goalsAtStart = goalsAtStart;
    }

    /** Wat de eigen club per week nog aan salaris betaalt. */
    public long ownWageShare() {
        return Math.round(player.wage * (100 - wageSharePct) / 100.0);
    }

    public int appearancesOnLoan() {
        return player.appearances - appearancesAtStart;
    }

    public int goalsOnLoan() {
        return player.goals - goalsAtStart;
    }
}
