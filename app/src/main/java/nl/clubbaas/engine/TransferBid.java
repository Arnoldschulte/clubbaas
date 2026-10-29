package nl.clubbaas.engine;

import java.io.Serializable;

/** Een bod van een andere club op een speler van de eigen club. */
public class TransferBid implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Player player;
    public final Club club;          // null = een buitenlandse club
    public long amount;
    public final boolean solicited;  // true = jij bood de speler zelf aan
    public boolean raised;           // al een keer verhoogd na 'meer vragen'
    final int expiresWeekKey;        // geldig t/m deze week

    TransferBid(Player player, Club club, long amount, boolean solicited, int expiresWeekKey) {
        this.player = player;
        this.club = club;
        this.amount = amount;
        this.solicited = solicited;
        this.expiresWeekKey = expiresWeekKey;
    }

    public String buyerName() {
        return club == null ? "Een buitenlandse club" : club.name;
    }
}
