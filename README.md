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
│   ├── MatchEngine.java     wedstrijdsimulatie (opstelling, aanval/verdediging, Poisson)
│   ├── Formation, Tactic    formaties en speelwijzen
│   ├── TeamRating.java      opstelling + aanvals- en verdedigingskracht van een team
│   ├── Division.java        clubs, speelschema (cirkelmethode) en stand
│   ├── Cup.java             bekertoernooi met voorronde en byes
│   ├── Club, Player, Staff, Stadium, Ledger, ...
│   └── SaveManager.java     opslaan via Java-serialisatie
└── ui/                      schermen, volledig in code opgebouwd (geen XML-layouts)
    ├── Ui.java              bouwstenen: kaarten, knoppen, chips, dialogen, kleuren
    ├── Screen.java          basisklasse voor schermen
    └── HomeScreen, SquadScreen, TacticsScreen, TransferScreen, TableScreen, FixturesScreen,
        CupScreen, StadiumScreen, StaffScreen, FinanceScreen, NewsScreen, ClubScreen
```

Het principe: de UI roept alleen publieke methodes van `Game` aan (bijv. `buyPlayer`,
`advanceWeek`) en bouwt daarna het scherm opnieuw op met `refresh()`, dat ook meteen opslaat.

## Spelregels in het kort

- **7 divisies × 12 clubs**, 22 speelweken. De nummers 1 en 2 promoveren, de laatste twee degraderen.
- **Beker** met alle 84 clubs in week 2, 5, 8, 11, 14, 17 en 20. Elke gewonnen ronde levert
  een premie op die per ronde verdubbelt.
- **Tactiek**: kies een formatie (4-4-2, 4-3-3, 4-5-1, 3-5-2, 5-3-2) en een speelwijze
  (verdedigend, normaal, aanvallend). AI-clubs kiezen elk seizoen de formatie die bij hun
  selectie past en passen hun speelwijze per wedstrijd aan de tegenstander aan.
- **Bemoeienis met de opstelling**: de manager kiest de basiself, maar je kunt hem vragen één
  voorkeursspeler te laten starten. Jonge spelers (t/m 23) met 15+ wedstrijden ontwikkelen zich
  sneller, en spelers met 8+ wedstrijden staan "in de etalage" (+10% op biedingen). Speelt je
  voorkeursspeler alleen op jouw verzoek, dan ergert de manager zich: +6 plus 3 per ster per
  wedstrijd, −4 bij elke wedstrijd zonder bemoeienis. Bij 100 stapt hij op.
- **Inkomsten**: kaartverkoop, horeca & fanshop, sponsor, tv-geld (hogere divisies), prijzengeld.
- **Uitgaven**: salarissen, staf, uitbreidingen, rente.
- **Fans** groeien bij winst en promotie; ticketprijs bepaalt hoeveel er komen.
- **Einde seizoen**: spelers worden ouder en ontwikkelen zich (trainer + trainingscomplex helpen),
  oude spelers stoppen, aflopende contracten vertrekken, jeugdacademie levert talenten.
- **Faillissement** als je 8 weken achter elkaar rood staat.

## De matchengine

1. **Opstelling**: per formatie de beste fitte spelers op hun eigen positie. Lege plekken
   worden gevuld met de beste reservespeler, die dan voor 70% telt.
2. **Aanval en verdediging** zijn gewogen gemiddelden: keeper en verdedigers tellen vooral mee
   voor de verdediging, spitsen vooral voor de aanval, middenvelders voor beide. Een
   verdedigende formatie schuift 1,5 punt per stap van aanval naar verdediging (en andersom).
   De manager geeft (sterren − 3) × 1,5 bonus op beide.
3. **Verwachte doelpunten**: thuis 1,30 × e^(0,045 × (aanval thuis + 3 − verdediging uit)),
   uit 1,10 × e^(0,045 × (aanval uit − verdediging thuis − 3)). Daarna × de speelwijze:
   verdedigend 0,85 voor / 0,80 tegen, aanvallend 1,15 voor / 1,20 tegen.
4. **Uitslag** via een Poisson-verdeling. Gemiddeld 2,5 goals per wedstrijd, 50% thuiswinst,
   25% gelijkspel.

Vuistregel: als underdog levert verdedigend spelen gemiddeld zo'n 0,08 punt per wedstrijd op,
als favoriet helpt aanvallend spelen.

## Balans aanpassen

Alle knoppen om aan te draaien staan bovenaan `Game.java`: `FAN_BASELINE`, `FAIR_PRICE`,
`SPONSOR`, `TV_WEEKLY`, `PRIZE`, plus `targetSkill()` voor de sterkte per divisie.
Spelerswaarde en salaris staan in `Player.baseValue()` en `Player.wageFor()`.

## Ideeën voor de volgende stap

- Europese toernooien voor de top 3 van de Topdivisie
- Conditie en moraal, zodat roteren loont
- Gele en rode kaarten met schorsingen
- Onderhandelen over transfersommen (bod / tegenbod)
- Bouwtijd voor het stadion (uitbreiding klaar na X weken)
- Prestaties en een highscorelijst
- Een speelweek-animatie of live tekstverslag
