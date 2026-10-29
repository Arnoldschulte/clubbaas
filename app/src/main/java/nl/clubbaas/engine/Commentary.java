package nl.clubbaas.engine;

/** Commentaarregels voor het live verslag. Variatie hangt af van minuut en speler, dus is herhaalbaar. */
public final class Commentary {
    private Commentary() {}

    private static final String[] GOAL = {
            "GOAL! %s schiet raak!",
            "GOAL! %s kopt hem binnen!",
            "GOAL! Wat een treffer van %s!",
            "GOAL! %s rondt koelbloedig af.",
            "GOAL! %s tikt de rebound binnen."};
    private static final String[] CHANCE = {
            "Kans voor %s, maar de bal gaat net naast.",
            "%s raakt de paal!",
            "Prima redding op een schot van %s.",
            "%s kopt over.",
            "%s komt alleen voor de keeper... maar mist!",
            "Schot van %s, recht in de handen van de keeper.",
            "%s schiet van afstand, ruim over."};
    private static final String[] YELLOW = {
            "Gele kaart voor %s.",
            "%s gaat in het boekje na een late tackle.",
            "De scheidsrechter trekt geel voor %s."};
    private static final String[] SECOND_YELLOW = {
            "Tweede gele kaart voor %s, hij moet eraf!",
            "%s krijgt nog een keer geel en mag gaan douchen."};
    private static final String[] RED = {
            "ROOD! %s moet direct naar de kant.",
            "Een keiharde overtreding van %s: direct rood!"};
    private static final String[] INJURY = {
            "%s grijpt naar zijn been en moet behandeld worden.",
            "Blessure voor %s, dat ziet er niet goed uit."};

    /** Eén regel, inclusief clubnaam en (bij een doelpunt) de nieuwe stand. */
    public static String line(Fixture f, MatchEvent e, int homeScore, int awayScore) {
        String club = e.home ? f.home.name : f.away.name;
        String[] options;
        switch (e.type) {
            case GOAL: options = GOAL; break;
            case CHANCE: options = CHANCE; break;
            case YELLOW: options = YELLOW; break;
            case SECOND_YELLOW: options = SECOND_YELLOW; break;
            case RED: options = RED; break;
            default: options = INJURY;
        }
        int pick = Math.abs((e.minute * 31 + e.player.hashCode()) % options.length);
        String text = String.format(options[pick], e.player) + " (" + club + ")";
        if (e.type == MatchEvent.Type.GOAL) text += "  " + homeScore + "-" + awayScore;
        return text;
    }

    public static String kickOff(Fixture f) {
        return "De scheidsrechter fluit, " + f.home.name + " tegen " + f.away.name + " is begonnen.";
    }

    public static String halfTime(int homeScore, int awayScore) {
        return "Rust. Tussenstand: " + homeScore + "-" + awayScore + ".";
    }

    public static String fullTime(Fixture f) {
        String s = "Einde wedstrijd: " + f.home.name + " " + f.homeGoals + "-" + f.awayGoals + " " + f.away.name + ".";
        if (f.penaltyWinner != null) s += " " + f.penaltyWinner.name + " wint na strafschoppen!";
        return s;
    }
}
