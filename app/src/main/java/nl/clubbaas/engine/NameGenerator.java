package nl.clubbaas.engine;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Verzint plaatsnamen, clubnamen en spelersnamen. Alles is fictief. */
public class NameGenerator implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String[] TOWN_START = {
            "Zand", "Berg", "Wes", "Oost", "Noord", "Hoog", "Laag", "Vel", "Brug", "Dijk",
            "Molen", "Eik", "Linde", "Haver", "Roos", "Wit", "Klein", "Groot", "Ever", "Vlie",
            "Ber", "Ter", "Hel", "Lier", "Mark", "Wolde", "Gaas", "Bree", "Dal", "Kreek",
            "Ommer", "Varen", "Hazel", "Meer", "Ven", "Steen", "Gans", "Elst", "Rijs", "Wils"};
    private static final String[] TOWN_END = {
            "dam", "dijk", "horst", "veen", "hoven", "wijk", "broek", "rade", "stein", "berg",
            "sum", "ingen", "lo", "beek", "dorp", "huizen", "kerk", "haven", "zijl", "voort",
            "meer", "wold", "oord", "rode", "werd"};
    private static final String[] CLUB_PREFIX = {
            "FC ", "SV ", "VV ", "SC ", "Sparta ", "Quick ", "Fortuna ", "Rapid ", "Victoria ",
            "DOS ", "Olympia ", "Excelsior ", "Unitas ", "Hercules ", "Velocitas ", "", "", ""};
    private static final String[] CLUB_SUFFIX = {
            "", "", "", "", " '08", " '19", " United", " Boys", " City", " Stars", " '45"};

    private static final String[] FIRST = {
            "Daan", "Sem", "Luuk", "Milan", "Thijs", "Jesse", "Bram", "Lars", "Ruben", "Niels",
            "Tim", "Jens", "Stijn", "Koen", "Mats", "Joep", "Sven", "Bas", "Rick", "Wout",
            "Jelle", "Dirk", "Floris", "Gijs", "Hugo", "Ivo", "Jop", "Kees", "Lucas", "Max",
            "Noah", "Olaf", "Pim", "Quinten", "Roel", "Siem", "Teun", "Vince", "Ward", "Yannick",
            "Mohamed", "Youssef", "Kenneth", "Mateo", "Diego", "Kofi", "Lukas", "Emil", "Jonas", "Rafael",
            "Ilias", "Anouar", "Davy", "Ricardo", "Tobias", "Oscar", "Felix", "Julian", "Aron", "Senna"};
    private static final String[] LAST = {
            "de Vries", "Jansen", "Bakker", "Visser", "Smit", "Meijer", "de Boer", "Mulder", "de Groot",
            "Bos", "Vos", "Peters", "Hendriks", "van Leeuwen", "Dekker", "Brouwer", "de Wit", "Dijkstra",
            "Smits", "de Graaf", "van der Meer", "van der Linden", "Kok", "Jacobs", "de Haan", "Vermeulen",
            "van den Berg", "van Dijk", "Schouten", "Hoekstra", "Kuipers", "Verhoeven", "Postma", "Koster",
            "Prins", "Huisman", "Willems", "Maas", "Blom", "Kramer", "Hermans", "Veenstra", "Wolters",
            "El Amrani", "Bouzid", "Mensah", "Asante", "Costa", "Silva", "Novak", "Kowalski", "Larsen",
            "Nilsson", "Okafor", "Martens", "Claes", "Peeters", "Lambrecht", "Hansen", "Moreau"};

    private final Set<String> usedClubNames = new HashSet<>();

    public String town(Random rng) {
        return TOWN_START[rng.nextInt(TOWN_START.length)] + TOWN_END[rng.nextInt(TOWN_END.length)];
    }

    public String clubName(Random rng) {
        for (int attempt = 0; attempt < 200; attempt++) {
            String name = CLUB_PREFIX[rng.nextInt(CLUB_PREFIX.length)]
                    + town(rng)
                    + CLUB_SUFFIX[rng.nextInt(CLUB_SUFFIX.length)];
            if (usedClubNames.add(name)) return name;
        }
        String fallback = "FC " + town(rng) + " " + usedClubNames.size();
        usedClubNames.add(fallback);
        return fallback;
    }

    public void reserveClubName(String name) {
        usedClubNames.add(name);
    }

    public String personName(Random rng) {
        return FIRST[rng.nextInt(FIRST.length)] + " " + LAST[rng.nextInt(LAST.length)];
    }
}
