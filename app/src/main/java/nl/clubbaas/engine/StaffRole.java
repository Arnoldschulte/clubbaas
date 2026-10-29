package nl.clubbaas.engine;

public enum StaffRole {
    MANAGER("Manager", "Betere wedstrijdresultaten"),
    TRAINER("Trainer", "Spelers ontwikkelen zich sneller"),
    FYSIO("Fysiotherapeut", "Minder en kortere blessures"),
    SCOUT("Scout", "Betere spelers op de transfermarkt"),
    JEUGDTRAINER("Jeugdtrainer", "Betere jeugdspelers per seizoen");

    public final String label;
    public final String effect;

    StaffRole(String label, String effect) {
        this.label = label;
        this.effect = effect;
    }
}
