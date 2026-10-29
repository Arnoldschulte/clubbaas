package nl.clubbaas.engine;

/** Het seizoensdoel van het bestuur, met de positie die minimaal gehaald moet worden. */
public enum BoardObjective {
    KAMPIOEN("Kampioen worden", 1),
    PROMOTIE("Promotie", Game.PROMOTION_SPOTS),
    TOP3("Bij de top 3 eindigen", 3),
    BOVENSTE_HELFT("Bovenste helft", Game.CLUBS_PER_DIVISION / 2),
    HANDHAVING("Handhaving", Game.CLUBS_PER_DIVISION - Game.PROMOTION_SPOTS);

    public final String label;
    public final int targetPosition;

    BoardObjective(String label, int targetPosition) {
        this.label = label;
        this.targetPosition = targetPosition;
    }
}
