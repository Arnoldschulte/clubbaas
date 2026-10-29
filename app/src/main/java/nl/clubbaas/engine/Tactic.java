package nl.clubbaas.engine;

/**
 * Speelwijze. Werkt als vermenigvuldiger op het verwachte aantal doelpunten.
 * Verdedigend maakt wedstrijden doelpuntarm (goed voor de underdog),
 * aanvallend maakt ze doelpuntrijk (goed voor de favoriet).
 */
public enum Tactic {
    VERDEDIGEND("Verdedigend", 0.85, 0.80,
            "Minder doelpunten aan beide kanten. Vergroot de kans op een gelijkspel tegen sterkere teams."),
    NORMAAL("Normaal", 1.0, 1.0,
            "Gebalanceerd spel."),
    AANVALLEND("Aanvallend", 1.15, 1.20,
            "Meer doelpunten aan beide kanten. Helpt om zwakkere teams echt te verslaan.");

    public final String label;
    public final double scoring;    // factor op eigen verwachte doelpunten
    public final double conceding;  // factor op verwachte tegendoelpunten
    public final String description;

    Tactic(String label, double scoring, double conceding, String description) {
        this.label = label;
        this.scoring = scoring;
        this.conceding = conceding;
        this.description = description;
    }
}
