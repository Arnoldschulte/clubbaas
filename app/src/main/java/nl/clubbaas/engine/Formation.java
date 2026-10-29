package nl.clubbaas.engine;

/** Formaties. Meer verdedigers = sterkere verdediging, meer aanvallers = sterkere aanval. */
public enum Formation {
    F442("4-4-2", 4, 4, 2),
    F433("4-3-3", 4, 3, 3),
    F451("4-5-1", 4, 5, 1),
    F352("3-5-2", 3, 5, 2),
    F532("5-3-2", 5, 3, 2);

    public final String label;
    public final int defenders;
    public final int midfielders;
    public final int attackers;

    Formation(String label, int defenders, int midfielders, int attackers) {
        this.label = label;
        this.defenders = defenders;
        this.midfielders = midfielders;
        this.attackers = attackers;
    }

    /** Aantal spelers op een positie in deze formatie (altijd 1 keeper). */
    public int count(Position p) {
        switch (p) {
            case DOELMAN: return 1;
            case VERDEDIGER: return defenders;
            case MIDDENVELDER: return midfielders;
            default: return attackers;
        }
    }
}
