package nl.clubbaas.engine;

/** Mijlpalen in een carrière als voorzitter. */
public enum Achievement {
    EERSTE_ZEGE("Eerste overwinning", "Win je eerste competitiewedstrijd."),
    GROTE_ZEGE("Doelpuntenfestijn", "Win een wedstrijd met vijf of meer doelpunten verschil."),
    EERSTE_PROMOTIE("Omhoog!", "Promoveer voor het eerst."),
    KAMPIOEN("Kampioen", "Word kampioen van een divisie."),
    ONGESLAGEN("Ongeslagen", "Speel een heel competitieseizoen zonder nederlaag."),
    TOPDIVISIE("De top bereikt", "Speel in de Topdivisie."),
    LANDSKAMPIOEN("Landskampioen", "Word kampioen van de Topdivisie."),
    BEKER("Bekerwinnaar", "Win de Nationale Beker."),
    EUROPA_DEBUUT("Europese avonden", "Plaats je voor de Europese Beker."),
    EUROPA("Europese held", "Win de Europese Beker."),
    MILJONAIR("Miljonair", "Heb een miljoen euro in kas."),
    TIEN_MILJOEN("Rijke club", "Heb tien miljoen euro in kas."),
    TRANSFERKONING("Transferkoning", "Verkoop een speler voor een miljoen euro of meer."),
    STADION("Echt stadion", "Breid je stadion uit tot 5.000 plaatsen."),
    ARENA("Arena", "Breid je stadion uit tot 25.000 plaatsen."),
    UITVERKOCHT("Uitverkocht", "Speel een thuiswedstrijd voor een vol stadion van minstens 1.000 plaatsen."),
    GOALGETTER("Goalgetter", "Laat een eigen speler 20 competitiedoelpunten maken in één seizoen."),
    TALENTENFABRIEK("Talentenfabriek", "Laat een speler uit je eigen jeugdopleiding vaardigheid 60 halen."),
    TOPSTAF("Topstaf", "Heb in alle vijf de stafrollen iemand met minstens vier sterren.");

    public final String title;
    public final String description;

    Achievement(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
