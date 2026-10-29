# Clubbaas

Een voetbalvoorzittersspel voor Android in puur Java. Je begint met een amateurclub in de
laagste van zeven divisies en probeert door slim te kopen, verkopen en investeren de
Topdivisie te bereiken en landskampioen te worden.

## Openen en draaien

1. Pak de zip uit en kies in Android Studio **File → Open** → de map `Clubbaas`.
2. Laat Gradle synchroniseren. Stelt Android Studio een upgrade van de Android Gradle Plugin
   voor, dan kun je die gewoon accepteren.
3. Kies een emulator of telefoon en druk op **Run**.

Unittests draaien: rechtsklik op `app/src/test/java` → **Run Tests**, of `./gradlew test`.

Vereisten: Android Studio met SDK 35. De app werkt op Android 8.0 (API 26) en hoger.

## Structuur

```
app/src/main/java/nl/clubbaas/
├── MainActivity.java        enige Activity: kopbalk, inhoud, navigatie, opslaan/laden
├── engine/                  spellogica, géén Android-code (los te testen)
│   ├── Game.java            de hele wereld + alle regels (week spelen, seizoenswissel,
│   │                        financiën, transfers, staf, stadion, bank)
│   ├── MatchEngine.java     wedstrijdsimulatie (beste elf in 1-4-4-2, Poisson-doelpunten)
│   ├── Division.java        clubs, speelschema (cirkelmethode) en stand
│   ├── Cup.java             bekertoernooi met voorronde en byes
│   ├── Club, Player, Staff, Stadium, Ledger, ...
│   └── SaveManager.java     opslaan via Java-serialisatie
└── ui/                      schermen, volledig in code opgebouwd (geen XML-layouts)
    ├── Ui.java              bouwstenen: kaarten, knoppen, chips, dialogen, kleuren
    ├── Screen.java          basisklasse voor schermen
    └── HomeScreen, SquadScreen, TransferScreen, TableScreen, FixturesScreen,
        CupScreen, StadiumScreen, StaffScreen, FinanceScreen, NewsScreen, ClubScreen
```

Het principe: de UI roept alleen publieke methodes van `Game` aan (bijv. `buyPlayer`,
`advanceWeek`) en bouwt daarna het scherm opnieuw op met `refresh()`, dat ook meteen opslaat.

## Spelregels in het kort

- **7 divisies × 12 clubs**, 22 speelweken. De nummers 1 en 2 promoveren, de laatste twee degraderen.
- **Beker** met alle 84 clubs in week 2, 5, 8, 11, 14, 17 en 20. Elke gewonnen ronde levert
  een premie op die per ronde verdubbelt.
- **Teamsterkte** = gemiddelde vaardigheid van de beste elf + managerbonus (+3 thuisvoordeel).
- **Inkomsten**: kaartverkoop, horeca & fanshop, sponsor, tv-geld (hogere divisies), prijzengeld.
- **Uitgaven**: salarissen, staf, uitbreidingen, rente.
- **Fans** groeien bij winst en promotie; ticketprijs bepaalt hoeveel er komen.
- **Einde seizoen**: spelers worden ouder en ontwikkelen zich (trainer + trainingscomplex helpen),
  oude spelers stoppen, aflopende contracten vertrekken, jeugdacademie levert talenten.
- **Faillissement** als je 8 weken achter elkaar rood staat.

## Balans aanpassen

Alle knoppen om aan te draaien staan bovenaan `Game.java`: `FAN_BASELINE`, `FAIR_PRICE`,
`SPONSOR`, `TV_WEEKLY`, `PRIZE`, plus `targetSkill()` voor de sterkte per divisie.
Spelerswaarde en salaris staan in `Player.baseValue()` en `Player.wageFor()`.

## Ideeën voor de volgende stap

- Europese toernooien voor de top 3 van de Topdivisie
- Opstelling en formatie zelf kiezen
- Onderhandelen over transfersommen (bod / tegenbod)
- Bouwtijd voor het stadion (uitbreiding klaar na X weken)
- Prestaties en een highscorelijst
- Een speelweek-animatie of live tekstverslag
