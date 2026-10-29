package nl.clubbaas.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * De volledige spelwereld en alle spelregels. Deze klasse heeft geen Android-afhankelijkheden,
 * zodat je hem los kunt testen. De UI roept alleen publieke methodes aan.
 */
public class Game implements Serializable {
    private static final long serialVersionUID = 1L;

    // ---- Spelconstanten ---------------------------------------------------------------
    public static final int DIVISIONS = 7;
    public static final int CLUBS_PER_DIVISION = 12;
    public static final int WEEKS_PER_SEASON = (CLUBS_PER_DIVISION - 1) * 2; // 22
    public static final int PROMOTION_SPOTS = 2;
    public static final int[] CUP_WEEKS = {2, 5, 8, 11, 14, 17, 20};
    public static final String[] DIVISION_NAMES = {
            "Topdivisie", "Hoofddivisie", "Eerste Klasse", "Tweede Klasse",
            "Derde Klasse", "Vierde Klasse", "Vijfde Klasse"};

    // Index = divisie - 1
    private static final int[] FAN_BASELINE = {40_000, 20_000, 10_000, 5_000, 2_000, 800, 300};
    private static final int[] FAIR_PRICE = {38, 28, 22, 17, 13, 10, 8};
    private static final long[] SPONSOR = {7_000_000, 3_000_000, 1_200_000, 500_000, 260_000, 120_000, 60_000};
    private static final long[] TV_WEEKLY = {300_000, 100_000, 30_000, 8_000, 2_000, 500, 0};
    private static final long[] PRIZE = {5_000_000, 1_500_000, 500_000, 150_000, 50_000, 15_000, 5_000};

    /** Vanaf zoveel wedstrijden per seizoen krijgt een speler t/m 23 jaar extra ontwikkeling. */
    public static final int PLAYING_TIME_BONUS_APPS = 15;
    /** Vanaf zoveel wedstrijden staat een speler 'in de etalage' en levert hij meer op. */
    public static final int SHOWCASE_APPS = 8;
    public static final int MAX_SQUAD = 25;
    public static final int MIN_SQUAD = 14;
    private static final int AI_SQUAD = 18;
    private static final int MARKET_SIZE = 24;
    private static final double LOAN_INTEREST_WEEKLY = 0.0025;
    private static final int BANKRUPTCY_WEEKS = 8;
    public static final int[] PALETTE = {
            0xFFD32F2F, 0xFF1976D2, 0xFF388E3C, 0xFFFBC02D, 0xFF7B1FA2, 0xFFF57C00,
            0xFF0097A7, 0xFF5D4037, 0xFF455A64, 0xFFC2185B, 0xFF212121, 0xFF00796B};

    // ---- Toestand ------------------------------------------------------------------------
    private final Random rng;
    private final NameGenerator names = new NameGenerator();
    private final List<Division> divisions = new ArrayList<>();
    private final List<NewsItem> news = new ArrayList<>();
    private final List<Player> market = new ArrayList<>();
    private final EnumMap<StaffRole, List<Staff>> candidates = new EnumMap<>(StaffRole.class);
    private final String chairmanName;
    private Club human;
    private Cup cup;
    private int seasonYear;
    private int week;
    private boolean seasonOver;
    private SeasonSummary lastSummary;
    private String humanCupResult;
    private double pendingFanFactor = 1.0;
    private boolean gameOver;
    private String gameOverReason;
    private boolean liveDisabled; // instelling: live verslag uit (standaard aan)
    private List<Loan> loans;     // uitgeleende spelers (null in oude savegames)

    // Prestaties en highscore (null/false in oude savegames)
    private java.util.EnumSet<Achievement> achievements;
    private boolean highscoreRecorded;
    private transient List<String> unlockMessages;

    // Europees voetbal (null in oude savegames)
    public static final String EUROPE_NAME = "Europese Beker";
    public static final int[] EUROPE_WEEKS = {4, 10, 16, 21};
    public static final int EUROPE_SPOTS = 3;
    private static final int EUROPE_CLUBS = 16;
    private static final long EUROPE_PARTICIPATION = 1_500_000;
    private static final long EUROPE_ROUND_PRIZE = 1_000_000;
    private Cup europe;
    List<Club> europeQualified;       // package-private voor tests
    private List<Club> foreignClubs;
    private String humanEuropeResult;

    // Bestuur en supporters (boardInit = false in oude savegames)
    private boolean boardInit;
    BoardObjective boardObjective;    // package-private voor tests
    int boardConfidence;
    int supporterMood;
    private int nextPlayerId = 1;
    private int nextClubId = 1;

    public Game(String chairmanName, String clubName, int clubColor, int startYear, long seed) {
        this.chairmanName = chairmanName;
        this.rng = new Random(seed);
        this.seasonYear = startYear;
        createWorld(clubName.trim().isEmpty() ? "FC Clubbaas" : clubName.trim(), clubColor);
        startSeasonCommon();
        generateSponsorOffers();
        ensureBoard();
        addNews("Doel van het bestuur dit seizoen: " + boardObjective.label.toLowerCase(Fmt.NL) + ".");
        addNews("Welkom, " + chairmanName + "! Je bent de nieuwe voorzitter van " + human.name
                + ". Het doel: van de " + DIVISION_NAMES[DIVISIONS - 1] + " naar de top.");
    }

    // ---- Opbouw van de wereld ----------------------------------------------------------

    public static int targetSkill(int level) {
        return 30 + (DIVISIONS - level) * 8;
    }

    private void createWorld(String clubName, int clubColor) {
        names.reserveClubName(clubName);
        for (int level = 1; level <= DIVISIONS; level++) {
            Division d = new Division(level, DIVISION_NAMES[level - 1]);
            for (int i = 0; i < CLUBS_PER_DIVISION; i++) {
                Club c = new Club(nextClubId++, names.clubName(rng),
                        PALETTE[rng.nextInt(PALETTE.length)], level, false);
                fillAiSquad(c);
                pickAiFormation(c);
                AiEconomy.initClub(c);
                d.clubs.add(c);
            }
            divisions.add(d);
        }

        // De eigen club vervangt een willekeurige club in de laagste divisie
        Division lowest = division(DIVISIONS);
        human = new Club(nextClubId++, clubName, clubColor, DIVISIONS, true);
        human.stadium = new Stadium();
        human.money = 75_000;
        human.fans = 250;
        human.staff.put(StaffRole.MANAGER, new Staff(names.personName(rng), StaffRole.MANAGER, 2));
        int[] template = {2, 5, 5, 4};
        int target = targetSkill(DIVISIONS) - 1;
        for (int pos = 0; pos < template.length; pos++) {
            for (int i = 0; i < template[pos]; i++) {
                Player p = newPlayer(Position.values()[pos], target, 4, 18, 32);
                p.contractYears = 1 + rng.nextInt(2);
                human.squad.add(p);
            }
        }
        lowest.clubs.set(rng.nextInt(lowest.clubs.size()), human);
        human.sponsorPerSeason = sponsorOffer();
    }

    private Player newPlayer(Position pos, int target, int spread, int minAge, int maxAge) {
        int skill = clamp((int) Math.round(target + rng.nextGaussian() * spread), 5, 95);
        int age = minAge + rng.nextInt(maxAge - minAge + 1);
        int growth = age <= 21 ? 5 + rng.nextInt(20) : age <= 25 ? rng.nextInt(10) : 0;
        Player p = new Player(nextPlayerId++, names.personName(rng), pos, age, skill, skill + growth);
        p.contractYears = 1 + rng.nextInt(3);
        return p;
    }

    /** Vult een AI-selectie aan tot 18 spelers in de verhouding 2 K / 6 V / 6 M / 4 A. */
    private void fillAiSquad(Club c) {
        int[] template = {2, 6, 6, 4};
        int target = targetSkill(c.level);
        while (c.squad.size() < AI_SQUAD) {
            int[] count = new int[4];
            for (Player p : c.squad) count[p.position.ordinal()]++;
            int pos = 0;
            double worstRatio = Double.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                double ratio = count[i] / (double) template[i];
                if (ratio < worstRatio) {
                    worstRatio = ratio;
                    pos = i;
                }
            }
            c.squad.add(newPlayer(Position.values()[pos], target, 5, 18, 33));
        }
    }

    private void startSeasonCommon() {
        for (Division d : divisions) d.createFixtures(rng);
        List<Club> all = allClubs();
        cup = new Cup(all);
        humanCupResult = "Nog in de race";
        for (Club c : all) {
            for (Player p : c.squad) {
                p.goals = 0;
                p.appearances = 0;
                p.yellowCards = 0;
                p.fatigue = 0; // fris na de zomerstop
            }
        }
        human.ledger.startSeason();
        market.clear();
        for (int i = 0; i < MARKET_SIZE; i++) market.add(newMarketPlayer());
        refreshCandidates();
        week = 0;
        seasonOver = false;
        createEurope();
    }

    // ---- Publieke getters voor de UI --------------------------------------------------

    public Club human() { return human; }
    public String chairmanName() { return chairmanName; }
    public List<Division> divisions() { return divisions; }
    public Division division(int level) { return divisions.get(level - 1); }
    public Division humanDivision() { return division(human.level); }
    public Cup cup() { return cup; }
    public int week() { return week; }
    public String seasonLabel() { return Fmt.seasonLabel(seasonYear); }
    public boolean isSeasonOver() { return seasonOver; }
    public SeasonSummary lastSummary() { return lastSummary; }
    public String humanCupResult() { return humanCupResult; }
    public boolean isGameOver() { return gameOver; }
    public String gameOverReason() { return gameOverReason; }
    public List<Player> market() { return market; }
    public boolean isLiveEnabled() { return !liveDisabled; }
    public void setLiveEnabled(boolean enabled) { liveDisabled = !enabled; }

    public List<Staff> candidates(StaffRole role) {
        List<Staff> list = candidates.get(role);
        return list == null ? Collections.<Staff>emptyList() : list;
    }

    /** Nieuws, nieuwste eerst. */
    public List<NewsItem> news() {
        List<NewsItem> copy = new ArrayList<>(news);
        Collections.reverse(copy);
        return copy;
    }

    public List<Club> allClubs() {
        List<Club> all = new ArrayList<>();
        for (Division d : divisions) all.addAll(d.clubs);
        return all;
    }

    public Fixture nextLeagueFixture() {
        if (seasonOver) return null;
        for (Fixture f : humanDivision().round(week + 1)) if (f.involves(human)) return f;
        return null;
    }

    public static boolean isCupWeek(int w) {
        for (int cw : CUP_WEEKS) if (cw == w) return true;
        return false;
    }

    public boolean nextWeekHasCupMatch() {
        return !seasonOver && isCupWeek(week + 1) && cup.isStillIn(human) && !cup.isFinished();
    }

    public int fairPrice() { return FAIR_PRICE[human.level - 1]; }

    public long tvMoneyWeekly() { return TV_WEEKLY[human.level - 1]; }

    public long loanLimit() { return 50_000 + human.sponsorPerSeason; }

    /** Verwachte toeschouwers bij een bepaalde ticketprijs. */
    public int expectedAttendance(int price) {
        double factor = Math.pow(FAIR_PRICE[human.level - 1] / (double) Math.max(1, price), 1.5);
        factor = Math.max(0.15, Math.min(1.4, factor));
        int demand = (int) (human.fans * factor * supporterAttendanceFactor());
        return Math.min(human.stadium.effectiveCapacity(), demand);
    }

    // ---- De speelweek ------------------------------------------------------------------

    /**
     * Speelt de volgende week. Na de laatste speelronde staat het seizoen op 'over';
     * de eerstvolgende aanroep start dan het nieuwe seizoen (promotie/degradatie enz.).
     */
    public WeekReport advanceWeek() {
        WeekReport rep = new WeekReport();
        if (gameOver) {
            rep.gameOver = true;
            return rep;
        }
        if (seasonOver) {
            startNewSeason();
            rep.newSeasonStarted = true;
            rep.messages.add("Seizoen " + seasonLabel() + " is begonnen! Je speelt in de "
                    + humanDivision().name + ".");
            return rep;
        }

        if (!human.sponsorOffers().isEmpty()) {
            SponsorDeal pick = null;
            for (SponsorDeal d : human.sponsorOffers()) if (d.type == SponsorDeal.Type.EENJARIG) pick = d;
            if (pick == null) pick = human.sponsorOffers().get(0);
            chooseSponsor(pick);
            rep.messages.add("Je hebt geen sponsor gekozen. Het bestuur tekende het eenjarige aanbod van "
                    + pick.sponsor + ".");
        }

        week++;
        human.ledger.startWeek();
        Map<Integer, Integer> appearancesBefore = new HashMap<>();
        for (Player p : human.squad) appearancesBefore.put(p.id, p.appearances);

        // Blessures genezen
        List<Club> inPlay = allClubs();
        inPlay.addAll(foreignClubs());
        for (Club c : inPlay) for (Player p : c.squad) if (p.injuryWeeks > 0) p.injuryWeeks--;

        // Herstel van vermoeidheid
        for (Club c : inPlay) MatchEngine.recover(c);

        // Competitie
        for (Division d : divisions) {
            for (Fixture f : d.round(week)) {
                MatchEngine.play(f, rng, false);
                d.record(f);
                if (f.involves(human)) {
                    rep.leagueMatch = f;
                    if (f.home == human) collectGate(f);
                    updateFans(f);
                    supportersAfterMatch(f);
                    checkMatchAchievements(f);
                    managerReaction(f, rep);
                    reportCards(f, rep);
                    if (f.winner() == human && human.sponsorDeal != null && human.sponsorDeal.winBonus > 0) {
                        addMoney("Sponsorbonus", human.sponsorDeal.winBonus);
                    }
                }
            }
        }

        // Beker
        if (isCupWeek(week) && !cup.isFinished()) {
            boolean wasIn = cup.isStillIn(human);
            List<Fixture> round = cup.playRound(week, rng);
            String roundName = cup.roundNames.isEmpty() ? "" : cup.roundNames.get(cup.roundNames.size() - 1);
            for (Fixture f : round) {
                if (f.involves(human)) {
                    rep.cupMatch = f;
                    rep.cupRoundName = roundName;
                    managerReaction(f, rep);
                    reportCards(f, rep);
                    checkMatchAchievements(f);
                    if (f.home == human) collectGate(f);
                    if (f.winner() == human) {
                        long prize = 5_000L << (cup.rounds.size() - 1);
                        addMoney("Prijzengeld", prize);
                        rep.messages.add("Door in de beker! Premie: " + Fmt.money(prize));
                    } else {
                        humanCupResult = "Uitgeschakeld in " + roundName.toLowerCase(Fmt.NL);
                        addNews(human.name + " ligt uit de beker (" + roundName.toLowerCase(Fmt.NL) + ").");
                    }
                }
            }
            if (wasIn && cup.winner == human) {
                humanCupResult = "Bekerwinnaar!";
                unlock(Achievement.BEKER);
                addNews("FEEST! " + human.name + " wint de " + Cup.NAME + "!");
            }
            if (cup.isFinished() && cup.winner != human) {
                addNews(cup.winner.name + " wint de " + Cup.NAME + ".");
            }
        }

        playEuropeRound(rep);

        updateMorale(appearancesBefore);
        progressConstruction(rep);
        weeklyFinances();
        AiEconomy.weekly(this);
        weeklyBids(rep);
        weeklyMarketRefresh();
        if (week % 4 == 1) refreshCandidates();

        if (week == WEEKS_PER_SEASON - 4) {
            int expiring = 0;
            for (Player p : human.squad) if (p.contractYears == 0) expiring++;
            if (expiring > 0) addNews(expiring + " speler(s) hebben een aflopend contract. "
                    + "Verleng op tijd, anders vertrekken ze aan het eind van het seizoen.");
        }

        checkBankruptcy(rep);
        if (!gameOver) weeklyBoard(rep);

        if (week == WEEKS_PER_SEASON && !gameOver) {
            seasonOver = true;
            lastSummary = buildSeasonSummary();
            rep.seasonSummary = lastSummary;
            if (boardConfidence <= 0) {
                fire(rep, "Na dit seizoen heeft het bestuur genoeg gezien: je bent ontslagen.");
            }
        }
        checkWeeklyAchievements(rep);
        return rep;
    }

    /** Meldingen over rode kaarten en schorsingen van de eigen spelers. */
    private void reportCards(Fixture f, WeekReport rep) {
        boolean weAreHome = f.home == human;
        for (MatchEvent e : f.events()) {
            if (e.home != weAreHome) continue;
            Player p = findOwnPlayer(e.player);
            if (p == null) continue;
            String msg = null;
            if (e.type == MatchEvent.Type.RED) {
                msg = p.name + " kreeg een directe rode kaart en is " + p.suspension + " wedstrijden geschorst.";
            } else if (e.type == MatchEvent.Type.SECOND_YELLOW) {
                msg = p.name + " kreeg twee keer geel en is " + p.suspension + " wedstrijd(en) geschorst.";
            } else if (e.type == MatchEvent.Type.YELLOW && p.yellowCards % MatchEngine.YELLOWS_FOR_SUSPENSION == 0
                    && p.isSuspended()) {
                msg = p.name + " pakte zijn " + p.yellowCards + "e gele kaart en is de volgende wedstrijd geschorst.";
            }
            if (msg != null) {
                addNews(msg);
                rep.messages.add(msg);
            }
        }
    }

    private Player findOwnPlayer(String name) {
        for (Player p : human.squad) if (p.name.equals(name)) return p;
        return null;
    }

    private void collectGate(Fixture f) {
        int attendance = expectedAttendance(human.stadium.ticketPrice);
        if (f.cup) attendance = Math.min(human.stadium.effectiveCapacity(), (int) (attendance * 1.15));
        f.attendance = attendance;
        human.lastAttendance = attendance;
        int capacity = human.stadium.effectiveCapacity();
        if (attendance >= capacity && capacity >= 1_000) unlock(Achievement.UITVERKOCHT);
        addMoney("Kaartverkoop", (long) attendance * human.stadium.ticketPrice);
        addMoney("Horeca & fanshop", Math.round(attendance * human.stadium.spendPerFan()));
    }

    private void updateFans(Fixture f) {
        String r = f.resultFor(human);
        double factor = r.equals("W") ? 1.02 : r.equals("G") ? 1.003 : 0.99;
        double fans = human.fans * factor;
        fans += (FAN_BASELINE[human.level - 1] - fans) * 0.01;
        human.fans = capFans((int) Math.round(fans));
    }

    /** Fans: minimaal 100, maximaal 3x het niveau van de divisie. */
    private int capFans(int fans) {
        return Math.max(100, Math.min(FAN_BASELINE[human.level - 1] * 3, fans));
    }

    private void weeklyFinances() {
        addMoney("Spelerssalarissen", -human.weeklyPlayerWages());
        if (!human.staff.isEmpty()) addMoney("Stafsalarissen", -human.weeklyStaffWages());
        long loanWages = 0;
        for (Loan l : loans()) loanWages += l.ownWageShare();
        if (loanWages > 0) addMoney("Salaris huurlingen (eigen deel)", -loanWages);
        addMoney("Sponsor", human.sponsorPerSeason / WEEKS_PER_SEASON);
        long tv = TV_WEEKLY[human.level - 1];
        if (tv > 0) addMoney("Tv-gelden", tv);
        if (human.loan > 0) {
            addMoney("Rente lening", -Math.max(1, Math.round(human.loan * LOAN_INTEREST_WEEKLY)));
        }
    }

    private void checkBankruptcy(WeekReport rep) {
        if (human.money < 0) {
            human.negativeWeeks++;
            int left = BANKRUPTCY_WEEKS - human.negativeWeeks;
            if (left <= 0) {
                gameOver = true;
                gameOverReason = human.name + " is failliet verklaard na " + BANKRUPTCY_WEEKS
                        + " weken rood staan. Je carrière als voorzitter is voorbij.";
                addNews(gameOverReason);
                rep.gameOver = true;
            } else {
                String warn = "Waarschuwing van de bank: je staat rood. Nog " + left
                        + " week/weken om dit op te lossen, anders volgt faillissement.";
                addNews(warn);
                rep.messages.add(warn);
            }
        } else {
            human.negativeWeeks = 0;
        }
    }

    void addMoney(String category, long amount) {
        human.money += amount;
        human.ledger.add(category, amount);
    }

    private void addNews(String text) {
        news.add(new NewsItem(seasonLabel(), week, text));
        while (news.size() > 150) news.remove(0);
    }

    // ---- Einde seizoen -----------------------------------------------------------------

    private SeasonSummary buildSeasonSummary() {
        SeasonSummary s = new SeasonSummary();
        Division d = humanDivision();
        int pos = d.positionOf(human);
        s.season = seasonLabel();
        s.divisionName = d.name;
        s.position = pos;
        if (human.level == 1 && pos == 1) {
            s.outcome = "LANDSKAMPIOEN! De hoogste prijs is binnen.";
            pendingFanFactor = 1.15;
        } else if (human.level > 1 && pos <= PROMOTION_SPOTS) {
            s.outcome = (pos == 1 ? "Kampioen! " : "") + "Promotie naar de " + DIVISION_NAMES[human.level - 2] + "!";
            pendingFanFactor = 1.20;
        } else if (human.level < DIVISIONS && pos > CLUBS_PER_DIVISION - PROMOTION_SPOTS) {
            s.outcome = "Degradatie naar de " + DIVISION_NAMES[human.level] + ".";
            pendingFanFactor = 0.85;
        } else {
            s.outcome = "Handhaving in de " + d.name + ".";
            pendingFanFactor = 1.0;
        }
        s.cupResult = humanCupResult;
        s.prizeMoney = PRIZE[human.level - 1] * (CLUBS_PER_DIVISION + 1 - pos) / CLUBS_PER_DIVISION;
        addMoney("Prijzengeld", s.prizeMoney);
        boolean success = pendingFanFactor > 1.0; // promotie of landstitel
        SponsorDeal deal = human.sponsorDeal;
        if (success && deal != null && deal.promotionBonus > 0) {
            addMoney("Sponsorbonus", deal.promotionBonus);
            addNews(deal.sponsor + " keert een bonus uit van " + Fmt.money(deal.promotionBonus) + "!");
        }
        for (Division div : divisions) {
            s.champions.add(div.name + ": " + div.sortedTable().get(0).club.name);
        }
        s.cupWinner = cup.winner == null ? null : cup.winner.name;
        boolean promoted = human.level > 1 && pos <= PROMOTION_SPOTS;
        s.boardVerdict = boardSeasonVerdict(pos, promoted, cup.winner == human);
        if (promoted) unlock(Achievement.EERSTE_PROMOTIE);
        if (pos == 1) unlock(Achievement.KAMPIOEN);
        if (pos == 1 && human.level == 1) unlock(Achievement.LANDSKAMPIOEN);
        if (d.sortedTable().get(pos - 1).lost == 0) unlock(Achievement.ONGESLAGEN);

        // Europa: wie plaatst zich voor volgend seizoen?
        s.europeResult = humanEuropeResult;
        s.europeWinner = europe != null && europe.winner != null ? europe.winner.name : null;
        europeQualified = new ArrayList<>();
        List<TableRow> topDivision = division(1).sortedTable();
        for (int i = 0; i < EUROPE_SPOTS; i++) {
            europeQualified.add(topDivision.get(i).club);
            s.europeQualified.add(topDivision.get(i).club.name);
        }
        if (europeQualified.contains(human)) {
            addNews(human.name + " plaatst zich voor de " + EUROPE_NAME + "!");
        }

        Player top = human.topScorer();
        String shortOutcome = s.outcome.startsWith("LANDS") ? "Landskampioen"
                : s.outcome.startsWith("Kampioen") ? "Kampioen"
                : s.outcome.startsWith("Promotie") ? "Promotie"
                : s.outcome.startsWith("Degradatie") ? "Degradatie" : "";
        SeasonRecord record = new SeasonRecord(s.season, d.name, pos, humanCupResult,
                top == null ? "-" : top.name + " (" + top.goals + ")", shortOutcome);
        record.europeResult = humanEuropeResult;
        human.history.add(record);
        addNews("Einde seizoen: " + pos + "e plaats. " + s.outcome);
        return s;
    }

    private void startNewSeason() {
        // 0. Huurlingen terug (vóór de ontwikkeling, zodat hun speeltijd meetelt met jouw faciliteiten)
        returnLoans();

        // 1. Promotie en degradatie (eerst bepalen, dan pas verplaatsen)
        List<List<Club>> up = new ArrayList<>();
        List<List<Club>> down = new ArrayList<>();
        for (Division d : divisions) {
            List<TableRow> table = d.sortedTable();
            List<Club> u = new ArrayList<>();
            List<Club> dn = new ArrayList<>();
            if (d.level > 1) for (int i = 0; i < PROMOTION_SPOTS; i++) u.add(table.get(i).club);
            if (d.level < DIVISIONS) {
                for (int i = 0; i < PROMOTION_SPOTS; i++) dn.add(table.get(table.size() - 1 - i).club);
            }
            up.add(u);
            down.add(dn);
        }
        for (int i = 0; i < divisions.size(); i++) {
            Division d = divisions.get(i);
            for (Club c : up.get(i)) {
                d.clubs.remove(c);
                divisions.get(i - 1).clubs.add(c);
                c.level--;
            }
            for (Club c : down.get(i)) {
                d.clubs.remove(c);
                divisions.get(i + 1).clubs.add(c);
                c.level++;
            }
        }

        // 2. Spelers: ontwikkeling, leeftijd, pensioen, contracten
        for (Club c : allClubs()) agePlayers(c);

        // 3. Transferperiode van de AI-clubs, daarna aanvullen tot een volledige selectie
        reportTransferWindow(AiEconomy.transferWindow(this));
        for (Club c : allClubs()) {
            if (c.human) continue;
            maintainAiSquad(c);
            pickAiFormation(c);
        }

        // 4. Eigen club: jeugd, sponsor, fans
        youthIntake();
        if (human.squad.size() < MIN_SQUAD) {
            int added = 0;
            while (human.squad.size() < MIN_SQUAD) {
                Player p = newPlayer(neededPosition(human), targetSkill(human.level) - 10, 4, 17, 20);
                p.contractYears = 1;
                human.squad.add(p);
                added++;
            }
            addNews(added + " jeugdspeler(s) bij de selectie gehaald om tot " + MIN_SQUAD + " spelers te komen.");
        }
        human.fans = capFans((int) (human.fans * pendingFanFactor));
        pendingFanFactor = 1.0;

        seasonYear++;
        startSeasonCommon();
        ensureBoard();
        setBoardObjective();
        addNews("Doel van het bestuur dit seizoen: " + boardObjective.label.toLowerCase(Fmt.NL)
                + ". Vertrouwen: " + confidenceLabel(boardConfidence).toLowerCase(Fmt.NL) + ".");
        SponsorDeal deal = human.sponsorDeal;
        if (deal != null && deal.seasonsLeft > 1) {
            deal.seasonsLeft--;
            human.sponsorPerSeason = deal.perSeason;
            addNews("Nieuw seizoen " + seasonLabel() + ". " + deal.sponsor + " blijft sponsor ("
                    + Fmt.money(deal.perSeason) + ", nog " + deal.seasonsLeft + " seizoen(en)).");
        } else {
            human.sponsorDeal = null;
            generateSponsorOffers();
            addNews("Nieuw seizoen " + seasonLabel() + ". Er liggen drie sponsoraanbiedingen klaar: "
                    + "kies er een bij Financiën.");
        }
    }

    private void agePlayers(Club c) {
        int trainer = c.staffRating(StaffRole.TRAINER);
        int facility = c.human ? c.stadium.trainingLevel : 1;
        double bonus = c.human ? (trainer - 2) * 0.6 + facility * 0.4 : 0;
        List<Player> leaving = new ArrayList<>();
        for (Player p : c.squad) {
            int d;
            if (p.age <= 21) d = 2 + rng.nextInt(6);
            else if (p.age <= 24) d = rng.nextInt(5);
            else if (p.age <= 28) d = rng.nextInt(4) - 1;
            else if (p.age <= 31) d = rng.nextInt(5) - 3;
            else d = -1 - rng.nextInt(6);

            // Jonge spelers die veel spelen, ontwikkelen zich sneller
            if (p.age <= 23 && p.appearances >= PLAYING_TIME_BONUS_APPS) d += 2;

            double extra = p.age <= 28 ? bonus : bonus / 2;
            d += (int) Math.floor(extra) + (rng.nextDouble() < extra - Math.floor(extra) ? 1 : 0);

            if (d > 0) p.skill = Math.max(p.skill, Math.min(p.skill + d, p.potential));
            else p.skill = Math.max(5, p.skill + d);
            p.age++;
            p.wage = c.human ? p.wage : Player.wageFor(p.skill, p.age);

            double retireChance = p.age >= 37 ? 1.0 : p.age >= 34 ? (p.age - 33) * 0.25 : 0;
            if (rng.nextDouble() < retireChance) {
                leaving.add(p);
                if (c.human) addNews(p.name + " (" + p.age + ") stopt met voetballen.");
                continue;
            }

            if (p.contractYears <= 0) {
                if (c.human) {
                    leaving.add(p);
                    addNews(p.name + " vertrekt transfervrij: zijn contract is afgelopen.");
                } else {
                    p.contractYears = 1 + rng.nextInt(3);
                }
            } else {
                p.contractYears--;
            }
        }
        c.squad.removeAll(leaving);
    }

    /**
     * Vangnet na de transferperiode: aanvullen met transfervrije spelers tot 18, en alleen
     * ingrijpen als een club ver van het niveau van zijn divisie af zit.
     */
    private void maintainAiSquad(Club c) {
        int target = targetSkill(c.level);
        fillAiSquad(c);
        if (c.averageSkill() < target - 2) {
            // Transfervrije spelers op niveau aantrekken voor de zwakste plekken
            for (int i = 0; i < 3; i++) {
                Player worst = null;
                for (Player p : c.squad) if (!isOnLoan(p) && (worst == null || p.skill < worst.skill)) worst = p;
                if (worst == null) break;
                c.squad.remove(worst);
                c.squad.add(newPlayer(worst.position, target, 4, 20, 30));
            }
        } else if (c.averageSkill() > target + 8) {
            // Veel te sterk (bijv. net gedegradeerd): toppers vertrekken naar het buitenland
            for (int i = 0; i < 2; i++) {
                Player best = null;
                for (Player p : c.squad) if (!isOnLoan(p) && (best == null || p.skill > best.skill)) best = p;
                if (best == null) break;
                c.squad.remove(best);
                c.money += best.value();
                c.squad.add(newPlayer(best.position, target, 4, 20, 30));
            }
        }
    }

    private void reportTransferWindow(List<AiEconomy.Deal> deals) {
        if (deals.isEmpty()) return;
        List<AiEconomy.Deal> sorted = new ArrayList<>(deals);
        sorted.sort((a, b) -> Long.compare(b.price, a.price));
        StringBuilder top = new StringBuilder();
        for (int i = 0; i < Math.min(3, sorted.size()); i++) top.append("\n• ").append(sorted.get(i).describe());
        addNews("Transferperiode: " + deals.size() + " transfers tussen clubs. Duurste:" + top);
        int ownDivision = 0;
        for (AiEconomy.Deal d : deals) {
            boolean relevant = d.from.level == human.level || (d.to != null && d.to.level == human.level);
            if (relevant && ownDivision < 5 && !sorted.subList(0, Math.min(3, sorted.size())).contains(d)) {
                addNews("Transfer: " + d.describe() + ".");
                ownDivision++;
            }
        }
    }

    Random rng() {
        return rng;
    }

    private Position neededPosition(Club c) {
        int[] min = {2, 4, 4, 2};
        int[] count = new int[4];
        for (Player p : c.squad) count[p.position.ordinal()]++;
        for (int i = 0; i < 4; i++) if (count[i] < min[i]) return Position.values()[i];
        return Position.values()[1 + rng.nextInt(3)];
    }

    private void youthIntake() {
        int academy = human.stadium.youthLevel;
        int coach = human.staffRating(StaffRole.JEUGDTRAINER);
        int count = 1 + (academy + 1) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (human.squad.size() >= MAX_SQUAD) break;
            int skill = targetSkill(human.level) - 16 + academy * 2 + (coach - 2) * 2 + rng.nextInt(7) - 3;
            skill = clamp(skill, 5, 80);
            int potential = skill + 10 + rng.nextInt(21) + academy * 2 + coach * 2;
            Player p = new Player(nextPlayerId++, names.personName(rng), neededPosition(human),
                    16 + rng.nextInt(2), skill, potential);
            p.contractYears = 3;
            p.academy = true;
            human.squad.add(p);
            if (sb.length() > 0) sb.append(", ");
            sb.append(p.name).append(" (").append(p.position.code).append(")");
        }
        if (sb.length() > 0) addNews("Nieuwe talenten uit de jeugdopleiding: " + sb + ".");
    }

    private void generateSponsorOffers() {
        long base = sponsorOffer();
        List<SponsorDeal> offers = human.sponsorOffers();
        offers.clear();
        offers.add(new SponsorDeal(names.sponsorName(rng), SponsorDeal.Type.MEERJARIG,
                round1000(base * 0.95), 0, 0, 3));
        offers.add(new SponsorDeal(names.sponsorName(rng), SponsorDeal.Type.EENJARIG, base, 0, 0, 1));
        offers.add(new SponsorDeal(names.sponsorName(rng), SponsorDeal.Type.PRESTATIE,
                round1000(base * 0.6), round1000(base * 0.035), round1000(base * 0.5), 1));
        human.sponsorPerSeason = base; // indicatie tot er gekozen is
    }

    private static long round1000(double v) {
        return Math.round(v / 1000.0) * 1000;
    }

    public ActionResult chooseSponsor(SponsorDeal deal) {
        if (!human.sponsorOffers().contains(deal)) return ActionResult.fail("Dit aanbod is niet meer geldig.");
        human.sponsorDeal = deal;
        human.sponsorPerSeason = deal.perSeason;
        human.sponsorOffers().clear();
        addNews(deal.sponsor + " is de nieuwe sponsor (" + deal.type.label.toLowerCase(Fmt.NL) + ").");
        return ActionResult.ok(deal.sponsor + " is je nieuwe sponsor!");
    }

    private long sponsorOffer() {
        int idx = human.level - 1;
        double ratio = Math.sqrt(human.fans / (double) FAN_BASELINE[idx]);
        ratio = Math.max(0.6, Math.min(1.5, ratio));
        return Math.round(SPONSOR[idx] * ratio / 1000.0) * 1000;
    }

    // ---- Transfermarkt ---------------------------------------------------------------

    private Player newMarketPlayer() {
        int target = targetSkill(human.level);
        int scout = human.staffRating(StaffRole.SCOUT);
        boolean free = rng.nextDouble() < 0.15;
        int center = target + (scout - 2) * 2 - (free ? 6 : 0);
        Position pos = Position.values()[weightedPosition()];
        Player p = newPlayer(pos, center, 7, 18, free ? 34 : 32);
        if (free) {
            p.askingPrice = 0;
        } else {
            double markup = 1.05 + rng.nextDouble() * 0.3;
            p.askingPrice = Math.max(1000, Math.round(p.value() * markup / 500.0) * 500);
        }
        return p;
    }

    private int weightedPosition() {
        int r = rng.nextInt(18);
        if (r < 2) return 0;
        if (r < 8) return 1;
        if (r < 14) return 2;
        return 3;
    }

    private void weeklyMarketRefresh() {
        for (int i = 0; i < 4 && !market.isEmpty(); i++) market.remove(rng.nextInt(market.size()));
        while (market.size() < MARKET_SIZE) market.add(newMarketPlayer());
    }

    /** Tekengeld voor transfervrije spelers: 6 weken salaris. */
    public long signingCost(Player p) {
        return p.isFreeAgent() ? p.wage * 6L : p.askingPrice;
    }

    /** Koopt een speler voor de vraagprijs (of tekengeld bij een transfervrije speler). */
    public ActionResult buyPlayer(Player p) {
        if (p.isFreeAgent() && market.contains(p)) return completePurchase(p, null, signingCost(p));
        return completePurchase(p, sellerOf(p), askingPrice(p));
    }

    // ---- Onderhandelen bij aankoop ------------------------------------------------------

    /** Bij welke AI-club speelt deze speler nu? Null = transfermarkt. */
    public Club sellerOf(Player p) {
        if (market.contains(p)) return null;
        for (Club c : allClubs()) if (!c.human && c.squad.contains(p)) return c;
        return null;
    }

    /** Vraagprijs: marktspelers hebben een vaste vraagprijs, clubs vragen meer voor basisspelers. */
    public long askingPrice(Player p) {
        if (market.contains(p)) return p.isFreeAgent() ? signingCost(p) : p.askingPrice;
        Club seller = sellerOf(p);
        if (seller == null) return p.value();
        double f = MatchEngine.rate(seller, seller.formation(), false, false).eleven().contains(p) ? 1.4 : 1.15;
        if (seller.level < human.level) f *= 1.2;
        return Math.round(p.value() * f / 1000.0) * 1000;
    }

    /** Spelers van andere clubs die je kunt proberen te kopen (divisie hoger, gelijk of lager). */
    public List<Player> scoutablePlayers(Position filter, int max) {
        List<Player> out = new ArrayList<>();
        for (int lvl = Math.max(1, human.level - 1); lvl <= Math.min(DIVISIONS, human.level + 1); lvl++) {
            for (Club c : division(lvl).clubs) {
                if (c.human || c.squad.size() <= 16) continue;
                for (Player p : c.squad) {
                    if (isOnLoan(p) || p.age > 33) continue;
                    if (filter == null || p.position == filter) out.add(p);
                }
            }
        }
        out.sort((a, b) -> b.skill - a.skill);
        return out.subList(0, Math.min(max, out.size()));
    }

    private void startNegotiation(Player p) {
        int key = seasonYear * 100 + week;
        if (p.negoWeekKey == key) return;
        p.negoWeekKey = key;
        p.negoMin = Math.round(askingPrice(p) * (0.75 + rng.nextDouble() * 0.2) / 500.0) * 500;
        p.negoCounter = 0;
        p.negoRounds = 0;
        p.negoClosed = false;
    }

    /**
     * Doe een bod. Genoeg = direct rond. In de buurt = tegenbod (max. 3 rondes). Te laag =
     * de verkoper stopt deze week met praten.
     */
    public NegotiationResult bid(Player p, long amount) {
        Club seller = sellerOf(p);
        if (seller == null && !market.contains(p)) {
            return new NegotiationResult(NegotiationResult.Outcome.REFUSED, 0, "Deze speler is niet meer beschikbaar.");
        }
        if (seller != null && seller.squad.size() <= 16) {
            return new NegotiationResult(NegotiationResult.Outcome.REFUSED, 0,
                    seller.name + " heeft te weinig spelers en verkoopt niet.");
        }
        startNegotiation(p);
        if (p.negoClosed) {
            return new NegotiationResult(NegotiationResult.Outcome.REFUSED, 0,
                    "De verkoper wil deze week niet meer met je praten.");
        }
        long asking = askingPrice(p);
        p.negoRounds++;
        if (amount >= p.negoMin) {
            ActionResult r = completePurchase(p, seller, amount);
            return new NegotiationResult(r.ok ? NegotiationResult.Outcome.ACCEPTED : NegotiationResult.Outcome.REFUSED,
                    0, r.ok ? "Bod geaccepteerd! " + r.message : r.message);
        }
        if (amount >= asking * 0.6 && p.negoRounds < 3) {
            long previous = p.negoCounter == 0 ? asking : p.negoCounter;
            long counter = Math.max(p.negoMin, Math.round((amount + previous) / 2.0 / 500.0) * 500);
            p.negoCounter = counter;
            return new NegotiationResult(NegotiationResult.Outcome.COUNTER, counter,
                    "De verkoper komt met een tegenbod: " + Fmt.money(counter) + ".");
        }
        p.negoClosed = true;
        return new NegotiationResult(NegotiationResult.Outcome.REFUSED, 0,
                amount < asking * 0.6 ? "Een beledigend bod. De verkoper wil deze week niet meer praten."
                        : "Geen akkoord. De verkoper wil deze week niet meer praten.");
    }

    /** Het laatste tegenbod accepteren. */
    public ActionResult acceptCounter(Player p) {
        int key = seasonYear * 100 + week;
        if (p.negoWeekKey != key || p.negoCounter == 0 || p.negoClosed) {
            return ActionResult.fail("Er ligt geen tegenbod meer.");
        }
        return completePurchase(p, sellerOf(p), p.negoCounter);
    }

    private ActionResult completePurchase(Player p, Club seller, long price) {
        if (seller == null && !market.contains(p)) return ActionResult.fail("Deze speler is niet meer beschikbaar.");
        if (human.squad.size() >= MAX_SQUAD) return ActionResult.fail("Je selectie zit vol (max " + MAX_SQUAD + ").");
        if (human.money < price) return ActionResult.fail("Te weinig geld. Nodig: " + Fmt.money(price));
        boolean free = seller == null && p.isFreeAgent();
        addMoney(free ? "Tekengeld" : "Transfers (aankoop)", -price);
        if (seller != null) {
            seller.squad.remove(p);
            seller.money += price;
            p.wage = Player.wageFor(p.skill, p.age);
        } else {
            market.remove(p);
        }
        p.askingPrice = 0;
        p.contractYears = 1 + rng.nextInt(3);
        p.goals = 0;
        p.appearances = 0;
        p.yellowCards = 0;
        p.moraleDelta = 10;       // enthousiast over zijn nieuwe club
        p.moraleInit = false;     // speeltijd begint bij de verwachting voor zijn rol
        p.transferRequested = false;
        p.negoWeekKey = -1;
        // Een grote aankoop maakt de supporters blij
        long avgValue = human.squad.isEmpty() ? 0 : human.totalSquadValue() / human.squad.size();
        if (price > 0 && price >= avgValue * 3) adjustSupporters(3);
        human.squad.add(p);
        addNews(p.name + " tekent bij " + human.name + (seller != null ? " (over van " + seller.name + ")" : "")
                + " voor " + Fmt.money(price) + ".");
        return ActionResult.ok(p.name + " is jouw nieuwe aanwinst!");
    }

    // ---- Biedingen op eigen spelers -----------------------------------------------------

    private List<TransferBid> bids; // null in oude savegames

    public List<TransferBid> bids() {
        if (bids == null) bids = new ArrayList<>();
        return bids;
    }

    /** Ongevraagde biedingen die nog openstaan. */
    public List<TransferBid> openUnsolicitedBids() {
        List<TransferBid> out = new ArrayList<>();
        for (TransferBid b : bids()) if (!b.solicited) out.add(b);
        return out;
    }

    private int weekKey() {
        return seasonYear * 100 + week;
    }

    private long offerAmount(Player p, double low, double high) {
        double f = low + rng.nextDouble() * (high - low);
        if (isShowcased(p)) f *= 1.10;
        if (p.transferRequested) f *= 0.90; // iedereen weet dat hij weg wil
        return Math.max(500, Math.round(p.value() * f / 500.0) * 500);
    }

    /** Bod als je een speler zelf aanbiedt. Per speler één bod per week. */
    public TransferBid bidFor(Player p) {
        for (TransferBid b : bids()) if (b.player == p && b.solicited && b.expiresWeekKey == weekKey()) return b;
        bids().removeIf(b -> b.player == p && b.solicited);
        long amount = offerAmount(p, 0.7, 1.15);
        TransferBid b = new TransferBid(p, findBuyer(amount), amount, true, weekKey());
        bids().add(b);
        return b;
    }

    /** Kort voor bidFor(p).amount, voor de UI en bestaande code. */
    public long saleOffer(Player p) {
        return bidFor(p).amount;
    }

    /** Speelt genoeg om op te vallen bij andere clubs: +10% op verkoopbiedingen. */
    public static boolean isShowcased(Player p) {
        return p.appearances >= SHOWCASE_APPS;
    }

    public ActionResult sellPlayer(Player p) {
        return acceptBid(bidFor(p));
    }

    public ActionResult acceptBid(TransferBid b) {
        if (!bids().contains(b)) return ActionResult.fail("Dit bod is niet meer geldig.");
        if (!human.squad.contains(b.player)) return ActionResult.fail("Speler zit niet in je selectie.");
        if (human.squad.size() <= MIN_SQUAD) return ActionResult.fail("Je hebt minimaal " + MIN_SQUAD + " spelers nodig.");
        Club buyer = b.club != null && b.club.money >= b.amount ? b.club : null;
        completeSale(b.player, buyer, b.amount);
        String to = buyer == null ? "een buitenlandse club" : buyer.name;
        return ActionResult.ok(b.player.name + " vertrekt naar " + to + " voor " + Fmt.money(b.amount) + ".");
    }

    /** Meer vragen: de koper verhoogt zijn bod (eenmalig) of haakt af. */
    public ActionResult askMore(TransferBid b) {
        if (!bids().contains(b)) return ActionResult.fail("Dit bod is niet meer geldig.");
        if (b.raised) return ActionResult.fail(b.buyerName() + " heeft al verhoogd: dit is het laatste bod.");
        if (rng.nextDouble() < 0.5) {
            long old = b.amount;
            long raisedAmount = Math.round(b.amount * (1.08 + rng.nextDouble() * 0.12) / 500.0) * 500;
            b.amount = Math.max(old + 500, raisedAmount); // ook bij kleine bedragen een echte verhoging
            b.raised = true;
            return ActionResult.ok(b.buyerName() + " verhoogt van " + Fmt.money(old) + " naar " + Fmt.money(b.amount) + ".");
        }
        bids().remove(b);
        return ActionResult.fail(b.buyerName() + " haakt af.");
    }

    public ActionResult rejectBid(TransferBid b) {
        if (!bids().remove(b)) return ActionResult.fail("Dit bod is niet meer geldig.");
        if (b.player.transferRequested && human.squad.contains(b.player)) {
            b.player.moraleDelta -= 5; // hij wilde juist weg
            return ActionResult.ok("Bod afgewezen. " + b.player.name + " baalt: hij wilde graag weg.");
        }
        return ActionResult.ok("Bod afgewezen.");
    }

    /** Wekelijks: verlopen biedingen opruimen en soms een ongevraagd bod op een van je toppers. */
    private void weeklyBids(WeekReport rep) {
        int key = weekKey();
        bids().removeIf(b -> b.expiresWeekKey < key || !human.squad.contains(b.player));
        List<Player> candidates = new ArrayList<>();
        for (Player p : human.squad) {
            if (p.age > 32) continue;
            boolean already = false;
            for (TransferBid b : bids()) if (b.player == p && !b.solicited) already = true;
            if (!already) candidates.add(p);
        }
        if (candidates.isEmpty()) return;
        candidates.sort((a, b) -> Long.compare(b.value(), a.value()));
        List<Player> top = new ArrayList<>(candidates.subList(0, Math.min(5, candidates.size())));
        for (Player p : candidates) if (p.transferRequested && !top.contains(p)) top.add(p);
        for (Player p : top) {
            double chance = p.transferRequested ? 0.25 : 0.04;
            if (rng.nextDouble() >= chance) continue;
            long amount = offerAmount(p, 0.85, 1.3);
            Club buyer = null;
            List<Club> options = new ArrayList<>();
            for (int lvl = Math.max(1, human.level - 2); lvl <= human.level; lvl++) {
                for (Club c : division(lvl).clubs) if (!c.human && c.money >= amount * 1.1) options.add(c);
            }
            if (!options.isEmpty()) buyer = options.get(rng.nextInt(options.size()));
            TransferBid b = new TransferBid(p, buyer, amount, false, key + 1);
            bids().add(b);
            String msg = b.buyerName() + " biedt " + Fmt.money(amount) + " op " + p.name + ". Het bod geldt twee weken.";
            addNews(msg);
            rep.messages.add(msg);
        }
    }

    /** Een club uit dezelfde of een hogere divisie die het bedrag kan betalen; null = buitenland. */
    private Club findBuyer(long price) {
        List<Club> options = new ArrayList<>();
        for (int lvl = Math.max(1, human.level - 1); lvl <= human.level; lvl++) {
            for (Club c : division(lvl).clubs) if (!c.human && c.money >= price) options.add(c);
        }
        return options.isEmpty() ? null : options.get(rng.nextInt(options.size()));
    }

    private void completeSale(Player p, Club buyer, long price) {
        boolean wasBest = true;
        for (Player q : human.squad) if (q != p && q.skill > p.skill) wasBest = false;
        if (wasBest) {
            adjustSupporters(-6);
            addNews("De supporters zijn boos dat hun beste speler, " + p.name + ", wordt verkocht.");
        }
        human.squad.remove(p);
        bids().removeIf(b -> b.player == p);
        if (human.favorite() == null && human.favoriteId == p.id) human.favoriteId = 0;
        if (buyer != null) {
            p.contractYears = 2 + rng.nextInt(2);
            p.wage = Player.wageFor(p.skill, p.age);
            buyer.squad.add(p);
            buyer.money -= price;
            if (buyer.squad.size() > 22) {
                Player worst = null;
                for (Player q : buyer.squad) {
                    if (isOnLoan(q)) continue; // nooit een huurling van de eigen club wegdoen
                    if (worst == null || q.skill < worst.skill) worst = q;
                }
                if (worst != null) buyer.squad.remove(worst);
            }
        }
        addMoney("Transfers (verkoop)", price);
        if (price >= 1_000_000) unlock(Achievement.TRANSFERKONING);
        addNews(p.name + " verkocht aan " + (buyer == null ? "een buitenlandse club" : buyer.name)
                + " voor " + Fmt.money(price) + ".");
    }

    /** Tevreden spelers verlengen goedkoper, ontevreden spelers vragen meer. */
    public int renewalWage(Player p) {
        int m = p.morale();
        double factor = m >= 75 ? 1.0 : m < 40 ? 1.25 : 1.1;
        return (int) (Math.round(Player.wageFor(p.skill, p.age) * factor / 10.0) * 10);
    }

    public long renewalBonus(Player p) {
        return renewalWage(p) * 4L;
    }

    public ActionResult renewContract(Player p, int years) {
        if (!human.squad.contains(p)) return ActionResult.fail("Speler zit niet in je selectie.");
        if (p.transferRequested) return ActionResult.fail(p.name + " wil weg en weigert te verlengen.");
        if (p.age >= 34 && years > 1) return ActionResult.fail(p.name + " wil op zijn leeftijd alleen nog voor 1 jaar tekenen.");
        long bonus = renewalBonus(p);
        if (human.money < bonus) return ActionResult.fail("Te weinig geld voor de tekenpremie (" + Fmt.money(bonus) + ").");
        addMoney("Tekengeld", -bonus);
        p.wage = renewalWage(p);
        p.contractYears = years;
        p.moraleDelta = Math.min(40, p.moraleDelta + 10); // nieuw contract = blije speler
        return ActionResult.ok(p.name + " verlengt met " + years + " jaar, salaris " + Fmt.money(p.wage) + " p/w.");
    }

    public long releaseCost(Player p) {
        return p.wage * 10L * (p.contractYears + 1);
    }

    public ActionResult releasePlayer(Player p) {
        if (!human.squad.contains(p)) return ActionResult.fail("Speler zit niet in je selectie.");
        if (human.squad.size() <= MIN_SQUAD) return ActionResult.fail("Je hebt minimaal " + MIN_SQUAD + " spelers nodig.");
        long cost = releaseCost(p);
        if (human.money < cost) return ActionResult.fail("Te weinig geld voor de afkoopsom (" + Fmt.money(cost) + ").");
        addMoney("Afkoopsommen", -cost);
        human.squad.remove(p);
        addNews("Contract van " + p.name + " ontbonden.");
        return ActionResult.ok(p.name + " is vertrokken.");
    }

    // ---- Prestaties en score ----------------------------------------------------------

    public java.util.Set<Achievement> achievements() {
        if (achievements == null) achievements = java.util.EnumSet.noneOf(Achievement.class);
        return achievements;
    }

    public boolean isHighscoreRecorded() { return highscoreRecorded; }

    public void markHighscoreRecorded() { highscoreRecorded = true; }

    private void unlock(Achievement a) {
        if (!achievements().add(a)) return;
        String msg = "🏅 Prestatie behaald: " + a.title + " (" + a.description.toLowerCase(Fmt.NL) + ")";
        addNews(msg);
        if (unlockMessages == null) unlockMessages = new ArrayList<>();
        unlockMessages.add(msg);
    }

    /** Na elke eigen wedstrijd. */
    private void checkMatchAchievements(Fixture f) {
        if (f.winner() != human) return;
        if (!f.cup) unlock(Achievement.EERSTE_ZEGE);
        int margin = Math.abs(f.homeGoals - f.awayGoals);
        if (margin >= 5) unlock(Achievement.GROTE_ZEGE);
    }

    /** Aan het eind van elke week. */
    private void checkWeeklyAchievements(WeekReport rep) {
        if (human.money >= 1_000_000) unlock(Achievement.MILJONAIR);
        if (human.money >= 10_000_000) unlock(Achievement.TIEN_MILJOEN);
        if (human.stadium.capacity >= 5_000) unlock(Achievement.STADION);
        if (human.stadium.capacity >= 25_000) unlock(Achievement.ARENA);
        if (human.level == 1) unlock(Achievement.TOPDIVISIE);
        boolean topStaff = true;
        for (StaffRole r : StaffRole.values()) if (human.staffRating(r) < 4) topStaff = false;
        if (topStaff) unlock(Achievement.TOPSTAF);
        for (Player p : human.squad) {
            if (p.goals >= 20) unlock(Achievement.GOALGETTER);
            if (p.academy && p.skill >= 60) unlock(Achievement.TALENTENFABRIEK);
        }
        if (unlockMessages != null) {
            rep.messages.addAll(unlockMessages);
            unlockMessages.clear();
        }
    }

    /**
     * Score voor de highscorelijst: elk seizoen punten naar divisie en eindpositie,
     * plus bonussen voor promoties, titels, bekers, Europa en prestaties.
     */
    public int score() {
        int score = 0;
        for (SeasonRecord r : human.history) {
            int level = Highscores.levelOf(r.division);
            score += (Game.DIVISIONS + 1 - level) * 10 + (CLUBS_PER_DIVISION + 1 - r.position);
            if (r.outcome.equals("Promotie") || r.outcome.equals("Kampioen")) score += 20;
            if (r.outcome.contains("ampioen")) score += 50;
            if (r.cupResult.startsWith("Bekerwinnaar")) score += 40;
            if (r.europeResult != null) score += "Winnaar!".equals(r.europeResult) ? 115 : 15;
        }
        score += achievements().size() * 5;
        return score;
    }

    // ---- Europees voetbal -------------------------------------------------------------

    public Cup europe() { return europe; }

    /** Null als de eigen club dit seizoen niet meedoet. */
    public String humanEuropeResult() { return humanEuropeResult; }

    public List<Club> foreignClubs() {
        return foreignClubs == null ? Collections.<Club>emptyList() : foreignClubs;
    }

    public static boolean isEuropeWeek(int w) {
        for (int ew : EUROPE_WEEKS) if (ew == w) return true;
        return false;
    }

    public boolean nextWeekHasEuropeMatch() {
        return !seasonOver && europe != null && isEuropeWeek(week + 1) && europe.isStillIn(human) && !europe.isFinished();
    }

    /** Nieuw toernooi: de gekwalificeerde clubs plus sterke buitenlandse clubs. */
    private void createEurope() {
        List<Club> qualified = new ArrayList<>();
        if (europeQualified != null) {
            for (Club c : europeQualified) if (c != null) qualified.add(c);
        }
        if (qualified.isEmpty()) {
            // Eerste seizoen (of oude savegame): de sterkste clubs van de Topdivisie
            List<Club> top = new ArrayList<>(division(1).clubs);
            top.sort((a, b) -> Double.compare(MatchEngine.strength(b), MatchEngine.strength(a)));
            qualified.addAll(top.subList(0, Math.min(EUROPE_SPOTS, top.size())));
        }
        foreignClubs = new ArrayList<>();
        java.util.Set<String> taken = new java.util.HashSet<>();
        for (Club c : allClubs()) taken.add(c.name);
        int target = targetSkill(1);
        while (qualified.size() + foreignClubs.size() < EUROPE_CLUBS) {
            Club f = new Club(nextClubId++, names.foreignClubName(rng, taken),
                    PALETTE[rng.nextInt(PALETTE.length)], 0, false);
            int strength = target - 2 + rng.nextInt(13); // -2 tot +10 boven de Topdivisie
            int[] template = {2, 6, 6, 4};
            for (int pos = 0; pos < template.length; pos++) {
                for (int i = 0; i < template[pos]; i++) {
                    f.squad.add(newPlayer(Position.values()[pos], strength, 4, 21, 32));
                }
            }
            pickAiFormation(f);
            foreignClubs.add(f);
        }
        List<Club> all = new ArrayList<>(qualified);
        all.addAll(foreignClubs);
        europe = new Cup(all);
        humanEuropeResult = qualified.contains(human) ? "Nog in de race" : null;
        if (humanEuropeResult != null) {
            unlock(Achievement.EUROPA_DEBUUT);
            addMoney("Prijzengeld", EUROPE_PARTICIPATION);
            addNews(human.name + " speelt dit seizoen in de " + EUROPE_NAME + "! Startpremie: "
                    + Fmt.money(EUROPE_PARTICIPATION) + ".");
        }
        for (Club c : qualified) if (!c.human) c.money += EUROPE_PARTICIPATION;
    }

    private void playEuropeRound(WeekReport rep) {
        if (europe == null || europe.isFinished() || !isEuropeWeek(week)) return;
        boolean wasIn = europe.isStillIn(human);
        List<Fixture> round = europe.playRound(week, rng);
        String roundName = europe.roundNames.get(europe.roundNames.size() - 1);
        long prize = EUROPE_ROUND_PRIZE << (europe.rounds.size() - 1);
        for (Fixture f : round) {
            Club winner = f.winner();
            if (winner != null && !winner.human && winner.level > 0) winner.money += prize;
            if (!f.involves(human)) continue;
            rep.europeMatch = f;
            rep.europeRoundName = roundName;
            if (f.home == human) collectGate(f);
            managerReaction(f, rep);
            reportCards(f, rep);
            checkMatchAchievements(f);
            if (winner == human) {
                addMoney("Prijzengeld", prize);
                rep.messages.add("Door in de " + EUROPE_NAME + "! Premie: " + Fmt.money(prize));
            } else {
                humanEuropeResult = "Uitgeschakeld in " + roundName.toLowerCase(Fmt.NL);
                addNews(human.name + " ligt uit de " + EUROPE_NAME + " (" + roundName.toLowerCase(Fmt.NL) + ").");
            }
        }
        if (wasIn && europe.winner == human) {
            humanEuropeResult = "Winnaar!";
            unlock(Achievement.EUROPA);
            addNews("HISTORISCH! " + human.name + " wint de " + EUROPE_NAME + "!");
            adjustSupporters(15);
            changeConfidence(15);
        } else if (europe.isFinished()) {
            addNews(europe.winner.name + " wint de " + EUROPE_NAME + ".");
        }
    }

    // ---- Bestuur en supporters --------------------------------------------------------

    public static final int BOARD_START = 65;
    public static final int BOARD_WARNING = 30;
    public static final int SUPPORTERS_NEUTRAL = 60;

    private void ensureBoard() {
        if (boardInit) return;
        boardInit = true;
        boardConfidence = BOARD_START;
        supporterMood = SUPPORTERS_NEUTRAL;
        setBoardObjective();
    }

    public BoardObjective boardObjective() {
        ensureBoard();
        return boardObjective;
    }

    public int boardConfidence() {
        ensureBoard();
        return boardConfidence;
    }

    public int supporterMood() {
        ensureBoard();
        return supporterMood;
    }

    public static String confidenceLabel(int c) {
        if (c >= 80) return "Groot vertrouwen";
        if (c >= 55) return "Tevreden";
        if (c >= BOARD_WARNING) return "Twijfels";
        return "Je positie staat op het spel";
    }

    public static String supporterLabel(int m) {
        if (m >= 80) return "Euforisch";
        if (m >= 60) return "Positief";
        if (m >= 40) return "Kritisch";
        return "Boos";
    }

    /** Het doel hangt af van hoe sterk je selectie is vergeleken met de rest van de divisie. */
    private void setBoardObjective() {
        List<Club> clubs = new ArrayList<>(humanDivision().clubs);
        clubs.sort((a, b) -> Double.compare(MatchEngine.strength(b), MatchEngine.strength(a)));
        int rank = clubs.indexOf(human) + 1;
        if (rank <= 2) {
            boardObjective = human.level > 1 ? BoardObjective.PROMOTIE
                    : rank == 1 ? BoardObjective.KAMPIOEN : BoardObjective.TOP3;
        } else if (rank <= 6) {
            boardObjective = BoardObjective.BOVENSTE_HELFT;
        } else {
            boardObjective = BoardObjective.HANDHAVING;
        }
    }

    private void changeConfidence(int delta) {
        // In je eerste seizoen geeft het bestuur je de tijd
        int floor = human.history.isEmpty() ? 25 : 0;
        boardConfidence = Math.max(Math.min(floor, boardConfidence), Math.min(100, boardConfidence + delta));
    }

    /** Toeschouwersfactor door de stemming van de supporters: 0,85 tot 1,15. */
    double supporterAttendanceFactor() {
        return 0.85 + 0.3 * supporterMood() / 100.0;
    }

    /** Na een eigen competitiewedstrijd: supporters reageren op het resultaat. */
    private void supportersAfterMatch(Fixture f) {
        ensureBoard();
        String r = f.resultFor(human);
        supporterMood += r.equals("W") ? 4 : r.equals("V") ? -4 : 0;
        supporterMood = Math.max(0, Math.min(100, supporterMood));
    }

    private void adjustSupporters(int delta) {
        ensureBoard();
        supporterMood = Math.max(0, Math.min(100, supporterMood + delta));
    }

    /** Wekelijks: ticketprijs, terugveren naar neutraal, en het bestuur kijkt naar de stand. */
    private void weeklyBoard(WeekReport rep) {
        ensureBoard();
        int price = human.stadium.ticketPrice;
        int fair = fairPrice();
        if (price > fair * 1.3) supporterMood -= 2;
        else if (price < fair * 0.8) supporterMood += 1;
        supporterMood += (int) Math.round((SUPPORTERS_NEUTRAL - supporterMood) * 0.05);
        supporterMood = Math.max(0, Math.min(100, supporterMood));

        int before = boardConfidence;
        if (week >= 6) {
            int pos = humanDivision().positionOf(human);
            int target = boardObjective.targetPosition;
            if (pos <= target) changeConfidence(1);
            else if (pos > target + 3) changeConfidence(-1);
        }
        if (human.money < 0) changeConfidence(-2);
        if (supporterMood >= 75) changeConfidence(1);
        else if (supporterMood < 30 && week % 2 == 0) changeConfidence(-1);

        if (before >= BOARD_WARNING && boardConfidence < BOARD_WARNING) {
            String msg = "Het bestuur maakt zich zorgen. Doel: " + boardObjective.label.toLowerCase(Fmt.NL)
                    + ". Zonder verbetering kost dit je je baan.";
            addNews(msg);
            rep.messages.add(msg);
        }
        if (boardConfidence <= 0) fire(rep, "Het bestuur heeft het vertrouwen in je opgezegd en je ontslagen.");
    }

    /** Einde seizoen: het bestuur beoordeelt het resultaat. Geeft het oordeel terug. */
    private String boardSeasonVerdict(int pos, boolean promoted, boolean cupWon) {
        ensureBoard();
        int target = boardObjective.targetPosition;
        String verdict;
        if (pos <= target) {
            int bonus = 15 + (target - pos >= 3 || (promoted && boardObjective != BoardObjective.PROMOTIE) ? 10 : 0);
            changeConfidence(bonus);
            verdict = "Doel gehaald (" + boardObjective.label.toLowerCase(Fmt.NL) + "). Het bestuur is tevreden.";
        } else {
            changeConfidence(-Math.min(30, 4 * (pos - target)));
            verdict = "Doel niet gehaald (" + boardObjective.label.toLowerCase(Fmt.NL) + ", " + pos + "e geworden).";
        }
        if (cupWon) changeConfidence(10);
        verdict += " Vertrouwen: " + confidenceLabel(boardConfidence).toLowerCase(Fmt.NL) + ".";
        return verdict;
    }

    private void fire(WeekReport rep, String reason) {
        if (gameOver) return;
        gameOver = true;
        gameOverReason = reason + " Je was " + human.history.size() + " seizoen(en) voorzitter van " + human.name + ".";
        addNews(gameOverReason);
        if (rep != null) rep.gameOver = true;
    }

    // ---- Uitlenen ---------------------------------------------------------------------

    public static final int MAX_LOANS = 4;
    /** Na deze week kan er niet meer worden uitgeleend. */
    public static final int LOAN_DEADLINE_WEEK = WEEKS_PER_SEASON - 4;

    public List<Loan> loans() {
        if (loans == null) loans = new ArrayList<>();
        return loans;
    }

    public boolean isOnLoan(Player p) {
        for (Loan l : loans()) if (l.player == p) return true;
        return false;
    }

    private Club clubById(int id) {
        for (Club c : allClubs()) if (c.id == id) return c;
        return null;
    }

    /** Zou deze speler in de basis staan bij die club? Laat de club echt een opstelling maken. */
    private static boolean wouldStartAt(Player p, Club c) {
        boolean alreadyThere = c.squad.contains(p);
        if (!alreadyThere) c.squad.add(p);
        boolean starts = MatchEngine.rate(c, c.formation(), false, false).eleven().contains(p);
        if (!alreadyThere) c.squad.remove(p);
        return starts;
    }

    /**
     * Een club uit dezelfde of een lagere divisie die de speler wil huren. Bij voorkeur een club
     * waar hij basisspeler zou zijn. Per speler één aanbod per week. Null = geen belangstelling.
     */
    public LoanOffer loanOffer(Player p) {
        int key = seasonYear * 100 + week;
        if (p.loanOfferWeekKey != key) {
            p.loanOfferWeekKey = key;
            List<Club> starters = new ArrayList<>();
            List<Club> others = new ArrayList<>();
            for (int lvl = human.level; lvl <= Math.min(DIVISIONS, human.level + 2); lvl++) {
                for (Club c : division(lvl).clubs) {
                    if (c == human) continue;
                    if (wouldStartAt(p, c)) starters.add(c);
                    else others.add(c);
                }
            }
            List<Club> pool = starters.isEmpty() ? others : starters;
            if (pool.isEmpty()) {
                p.loanOfferClubId = 0;
            } else {
                Club c = pool.get(rng.nextInt(pool.size()));
                p.loanOfferClubId = c.id;
                p.loanOfferPct = (starters.isEmpty() ? 30 : 50) + rng.nextInt(51);
            }
        }
        Club c = p.loanOfferClubId == 0 ? null : clubById(p.loanOfferClubId);
        if (c == null) return null;
        return new LoanOffer(c, Math.min(100, p.loanOfferPct), wouldStartAt(p, c));
    }

    public ActionResult loanPlayer(Player p) {
        if (!human.squad.contains(p)) return ActionResult.fail("Speler zit niet in je selectie.");
        if (seasonOver || week > LOAN_DEADLINE_WEEK) return ActionResult.fail("De uitleenperiode is voorbij voor dit seizoen.");
        if (loans().size() >= MAX_LOANS) return ActionResult.fail("Je kunt maximaal " + MAX_LOANS + " spelers uitlenen.");
        if (human.squad.size() <= MIN_SQUAD) return ActionResult.fail("Je hebt minimaal " + MIN_SQUAD + " spelers nodig.");
        LoanOffer offer = loanOffer(p);
        if (offer == null) return ActionResult.fail("Geen enkele club wil " + p.name + " huren.");
        if (human.favorite() == p) human.favoriteId = 0;
        human.squad.remove(p);
        offer.club.squad.add(p);
        loans().add(new Loan(p, offer.club, offer.wageSharePct));
        addNews(p.name + " wordt tot het eind van het seizoen verhuurd aan " + offer.club.name
                + " (zij betalen " + offer.wageSharePct + "% van zijn salaris).");
        return ActionResult.ok(p.name + " vertrekt op huurbasis naar " + offer.club.name + ".");
    }

    /** Aan het eind van het seizoen keren alle huurlingen terug. */
    private void returnLoans() {
        for (Loan l : loans()) {
            l.club.squad.remove(l.player);
            human.squad.add(l.player);
            l.player.moraleInit = false; // rol bij de eigen club opnieuw bepalen
            if (l.appearancesOnLoan() >= PLAYING_TIME_BONUS_APPS) {
                l.player.moraleDelta = Math.min(40, l.player.moraleDelta + 5);
            }
            addNews(l.player.name + " keert terug van " + l.club.name + ": " + l.appearancesOnLoan()
                    + " wedstrijden, " + l.goalsOnLoan() + " doelpunten.");
        }
        loans().clear();
    }

    // ---- Invloed van de voorzitter ----------------------------------------------------

    public static final int FRUSTRATION_WARNING = 50;
    public static final int FRUSTRATION_DANGER = 80;
    public static final int FRUSTRATION_RESIGN = 100;

    /** Vraag de manager om deze speler te laten starten (null = niemand meer voortrekken). */
    public void setFavorite(Player p) {
        human.favoriteId = p == null ? 0 : p.id;
    }

    /** Hoeveel ergernis het kost als de voorkeursspeler alleen op verzoek speelt. */
    public int frustrationPerForcedMatch() {
        Staff m = human.staff.get(StaffRole.MANAGER);
        return m == null ? 0 : 6 + 3 * m.rating;
    }

    public static String managerMoodLabel(int frustration) {
        if (frustration < 25) return "Uitstekend";
        if (frustration < FRUSTRATION_WARNING) return "Goed";
        if (frustration < FRUSTRATION_DANGER) return "Gespannen";
        return "Op knappen";
    }

    /**
     * Na elke eigen wedstrijd: speelde de voorkeursspeler alleen op verzoek, dan groeit de ergernis
     * van de manager (een betere manager heeft een groter ego). Anders zakt die langzaam weer.
     */
    private void managerReaction(Fixture f, WeekReport rep) {
        Staff manager = human.staff.get(StaffRole.MANAGER);
        if (manager == null) {
            human.managerFrustration = 0;
            return;
        }
        int before = human.managerFrustration;
        if (f.favoriteForced) {
            human.managerFrustration = Math.min(FRUSTRATION_RESIGN, before + frustrationPerForcedMatch());
        } else {
            human.managerFrustration = Math.max(0, before - 4);
        }
        int now = human.managerFrustration;
        if (now >= FRUSTRATION_RESIGN) {
            human.staff.remove(StaffRole.MANAGER);
            human.managerFrustration = 0;
            human.favoriteId = 0;
            String msg = manager.name + " stapt op als manager: \"Ik laat me niet de opstelling voorschrijven.\" "
                    + "Zoek snel een nieuwe manager.";
            addNews(msg);
            rep.messages.add(msg);
        } else if (before < FRUSTRATION_DANGER && now >= FRUSTRATION_DANGER) {
            String msg = manager.name + " dreigt op te stappen als je je met de opstelling blijft bemoeien.";
            addNews(msg);
            rep.messages.add(msg);
        } else if (before < FRUSTRATION_WARNING && now >= FRUSTRATION_WARNING) {
            String msg = manager.name + " ergert zich aan je bemoeienis met de opstelling.";
            addNews(msg);
            rep.messages.add(msg);
        }
    }

    // ---- Moraal -----------------------------------------------------------------------

    /** Hoeveel procent van het verschil met de doelmoraal er per week wordt ingelopen. */
    private static final double MORALE_ADJUST_RATE = 0.15;
    public static final int TRANSFER_REQUEST_BELOW = 25;
    private static final int TRANSFER_REQUEST_WITHDRAW_ABOVE = 45;

    /** Gegevens die voor alle spelers gelijk zijn, één keer per week berekend. */
    private static final class MoraleContext {
        final Set<Player> naturalEleven = new HashSet<>();
        final Map<Player, Integer> rank = new HashMap<>();
        Player favorite;
        double pointsPerMatch = -1; // -1 = nog geen wedstrijden
    }

    private MoraleContext moraleContext() {
        MoraleContext ctx = new MoraleContext();
        // Op papier, zonder rotatie door vermoeidheid en zonder bemoeienis van de voorzitter
        ctx.naturalEleven.addAll(MatchEngine.rate(human, human.formation(), false, false).eleven());
        List<Player> bySkill = new ArrayList<>(human.squad);
        bySkill.sort((a, b) -> b.skill - a.skill);
        for (int i = 0; i < bySkill.size(); i++) ctx.rank.put(bySkill.get(i), i + 1);
        ctx.favorite = human.favorite();
        List<Fixture> played = new ArrayList<>();
        for (Fixture f : humanDivision().fixturesOf(human)) if (f.played) played.add(f);
        if (!played.isEmpty()) {
            int points = 0, n = 0;
            for (int i = Math.max(0, played.size() - 5); i < played.size(); i++, n++) {
                String r = played.get(i).resultFor(human);
                points += r.equals("W") ? 3 : r.equals("G") ? 1 : 0;
            }
            ctx.pointsPerMatch = points / (double) n;
        }
        return ctx;
    }

    /**
     * Welk deel van de wedstrijden zijn huidige rol rechtvaardigt. De manager speelt altijd met
     * zijn beste elf, dus reserves weten dat ze weinig spelen en verwachten dat ook niet.
     */
    private static double roleExpectation(Player p, MoraleContext ctx) {
        double expected;
        if (ctx.naturalEleven.contains(p)) expected = 0.9;
        else if (ctx.rank.getOrDefault(p, 99) <= 16) expected = 0.1;
        else expected = 0.05;
        if (p.age <= 20) expected = Math.min(expected, 0.25); // talenten zijn blij met elke kans
        return expected;
    }

    private static double share(Player p, MoraleContext ctx) {
        return p.moraleInit ? p.playShare : roleExpectation(p, ctx);
    }

    private static double expectation(Player p, MoraleContext ctx) {
        return p.moraleInit ? p.expectation : roleExpectation(p, ctx);
    }

    /** Hoe goed een ploeggenoot presteert, gebruikt om te bepalen of een hoger salaris 'verdiend' is. */
    private static double performance(Player t, MoraleContext ctx) {
        double perf = t.skill;
        if (share(t, ctx) >= 0.6) perf += 3; // vaste basisspeler
        if (t.position == Position.AANVALLER || t.position == Position.MIDDENVELDER) {
            perf += Math.min(5, t.goals * 0.5);
        }
        return perf;
    }

    public MoraleBreakdown moraleBreakdown(Player p) {
        return moraleBreakdown(p, moraleContext());
    }

    private MoraleBreakdown moraleBreakdown(Player p, MoraleContext ctx) {
        MoraleBreakdown b = new MoraleBreakdown();

        // Speeltijd vergeleken met wat hij verwacht
        double diff = share(p, ctx) - expectation(p, ctx);
        b.playingTime = (int) Math.round(Math.max(-25, Math.min(10, diff * 40)));
        if (b.playingTime <= -5) b.reasons.add("Speelt minder dan hij verwacht");
        else if (b.playingTime >= 3) b.reasons.add("Blij met zijn speeltijd");

        // Salaris vergeleken met wat hij nu waard is (pas bij flinke onderbetaling)
        double ratio = p.wage / (double) Player.wageFor(p.skill, p.age);
        double s = 0;
        if (ratio < 0.7) s = -Math.min(15, (0.7 - ratio) * 40);
        else if (ratio > 1.3) s = Math.min(4, (ratio - 1.3) * 8);
        if (p.age <= 20 && s < 0) s /= 2;
        b.salary = (int) Math.round(s);
        if (b.salary <= -2) b.reasons.add("Voelt zich onderbetaald");
        else if (b.salary >= 2) b.reasons.add("Tevreden met zijn salaris");

        // Jaloezie, bewust streng: alleen vaste basisspelers die ook echt veel spelen, en alleen
        // als een ploeggenoot minstens 1,75x zoveel verdient én duidelijk minder presteert.
        // Een ploeggenoot die beter is, vast speelt of scoort, krijgt zijn salaris gegund.
        // Talenten t/m 20 jaar zijn vooral blij dat ze spelen en kijken (nog) niet naar salarissen.
        if (p.age > 20 && ctx.naturalEleven.contains(p) && share(p, ctx) >= 0.6) {
            for (Player t : human.squad) {
                if (t == p) continue;
                if (t.wage >= p.wage * 1.75 && performance(t, ctx) + 2 < p.skill) b.jealousOf.add(t.name);
            }
            b.jealousy = -Math.min(4, 2 * b.jealousOf.size());
            if (!b.jealousOf.isEmpty()) {
                b.reasons.add("Jaloers op " + String.join(", ", b.jealousOf)
                        + " (verdient veel meer, presteert niet beter)");
            }
        }

        // Teamresultaten van de laatste vijf competitiewedstrijden
        if (ctx.pointsPerMatch >= 0) {
            b.results = (int) Math.round(Math.max(-7, Math.min(8, (ctx.pointsPerMatch - 1.4) * 5)));
            if (b.results <= -2) b.reasons.add("Slechte resultaten drukken de sfeer");
            else if (b.results >= 2) b.reasons.add("Goede resultaten geven vertrouwen");
        }

        if (p == ctx.favorite) {
            b.favorite = 5;
            b.reasons.add("Voelt het vertrouwen van de voorzitter");
        }

        int t = Player.BASE_MORALE + b.playingTime + b.salary + b.jealousy + b.results + b.favorite;
        b.target = Math.max(5, Math.min(95, t));
        if (b.reasons.isEmpty()) b.reasons.add("Niets bijzonders");
        return b;
    }

    /** Wekelijks: speeltijd bijwerken en moraal 15% richting de doelwaarde schuiven. */
    private void updateMorale(Map<Integer, Integer> appearancesBefore) {
        MoraleContext ctx = moraleContext();
        for (Player p : human.squad) {
            double role = roleExpectation(p, ctx);
            if (!p.moraleInit) {
                p.playShare = (float) role;
                p.expectation = (float) role;
                p.moraleInit = true;
            } else if (role > p.expectation) {
                p.expectation = (float) role;                          // nieuwe basisplaats: meteen
            } else {
                p.expectation += (float) ((role - p.expectation) * 0.1); // basisplaats kwijt: langzaam wennen
            }
            Integer before = appearancesBefore.get(p.id);
            if (before != null && p.isAvailable()) {
                boolean played = p.appearances > before;
                p.playShare = 0.8f * p.playShare + 0.2f * (played ? 1f : 0f);
            }
        }
        for (Player p : human.squad) {
            MoraleBreakdown b = moraleBreakdown(p, ctx);
            int m = p.morale();
            int next = m + (int) Math.round((b.target - m) * MORALE_ADJUST_RATE);
            p.moraleDelta = next - Player.BASE_MORALE;

            if (!p.transferRequested && p.morale() < TRANSFER_REQUEST_BELOW) {
                p.transferRequested = true;
                addNews(p.name + " is ontevreden en vraagt om een transfer. Reden: "
                        + b.reasons.get(0).toLowerCase(Fmt.NL) + ".");
            } else if (p.transferRequested && p.morale() > TRANSFER_REQUEST_WITHDRAW_ABOVE) {
                p.transferRequested = false;
                addNews(p.name + " trekt zijn transferverzoek in.");
            }
        }
    }

    /** Gemiddelde moraal van de selectie. */
    public int squadMorale() {
        if (human.squad.isEmpty()) return Player.BASE_MORALE;
        int t = 0;
        for (Player p : human.squad) t += p.morale();
        return Math.round(t / (float) human.squad.size());
    }

    // ---- Tactiek ----------------------------------------------------------------------

    public void setFormation(Formation f) {
        human.formation = f;
    }

    public void setTactic(Tactic t) {
        human.tactic = t;
    }

    /** AI-clubs kiezen elk seizoen de formatie die het best bij hun selectie past. */
    private void pickAiFormation(Club c) {
        Formation best = Formation.F442;
        double bestScore = -1;
        for (Formation f : Formation.values()) {
            double score = MatchEngine.rate(c, f).overall() + rng.nextDouble() * 0.5;
            if (score > bestScore) {
                bestScore = score;
                best = f;
            }
        }
        c.formation = best;
    }

    /**
     * Verwachte doelpunten in de volgende competitiewedstrijd, vanuit de eigen club gezien:
     * {voor, tegen}. Null als er geen wedstrijd meer is.
     */
    public double[] expectedGoalsNextMatch() {
        Fixture f = nextLeagueFixture();
        if (f == null) return null;
        TeamRating h = MatchEngine.rate(f.home);
        TeamRating a = MatchEngine.rate(f.away);
        double[] e = MatchEngine.expectedGoals(h, a,
                MatchEngine.tacticFor(f.home, h, a), MatchEngine.tacticFor(f.away, a, h), false);
        return f.home == human ? e : new double[]{e[1], e[0]};
    }

    /** Welke speelwijze de volgende tegenstander waarschijnlijk kiest. */
    public Tactic expectedOpponentTactic() {
        Fixture f = nextLeagueFixture();
        if (f == null) return Tactic.NORMAAL;
        Club opp = f.opponentOf(human);
        return MatchEngine.tacticFor(opp, MatchEngine.rate(opp), MatchEngine.rate(human));
    }

    // ---- Staf -------------------------------------------------------------------------

    private void refreshCandidates() {
        for (StaffRole role : StaffRole.values()) {
            List<Staff> list = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                int r = rng.nextInt(100);
                int rating = r < 30 ? 1 : r < 60 ? 2 : r < 80 ? 3 : r < 93 ? 4 : 5;
                list.add(new Staff(names.personName(rng), role, rating));
            }
            candidates.put(role, list);
        }
    }

    public ActionResult hireStaff(Staff s) {
        List<Staff> list = candidates.get(s.role);
        if (list == null || !list.contains(s)) return ActionResult.fail("Deze kandidaat is niet meer beschikbaar.");
        Staff current = human.staff.get(s.role);
        long cost = s.signingFee() + (current == null ? 0 : current.severance());
        if (human.money < cost) return ActionResult.fail("Te weinig geld. Nodig: " + Fmt.money(cost));
        if (current != null) addMoney("Afkoopsommen", -current.severance());
        addMoney("Tekengeld", -s.signingFee());
        human.staff.put(s.role, s);
        if (s.role == StaffRole.MANAGER) human.managerFrustration = 0;
        list.remove(s);
        addNews(s.name + " is aangesteld als " + s.role.label.toLowerCase(Fmt.NL) + " " + s.stars() + ".");
        return ActionResult.ok(s.name + " is aangesteld.");
    }

    public ActionResult fireStaff(StaffRole role) {
        Staff current = human.staff.get(role);
        if (current == null) return ActionResult.fail("Er is geen " + role.label.toLowerCase(Fmt.NL) + ".");
        if (human.money < current.severance()) return ActionResult.fail("Te weinig geld voor de afkoopsom.");
        addMoney("Afkoopsommen", -current.severance());
        human.staff.remove(role);
        if (role == StaffRole.MANAGER) human.managerFrustration = 0;
        addNews(current.name + " is ontslagen als " + role.label.toLowerCase(Fmt.NL) + ".");
        return ActionResult.ok(current.name + " is ontslagen.");
    }

    // ---- Stadion ----------------------------------------------------------------------

    /** Start de bouw van een nieuwe tribune. Je betaalt direct; de plaatsen komen er na de bouwtijd bij. */
    public ActionResult expandStadium() {
        Stadium s = human.stadium;
        if (s.isUnderConstruction()) {
            return ActionResult.fail("Er wordt al gebouwd, nog " + s.constructionWeeksLeft + " week/weken.");
        }
        long cost = s.expansionCost();
        if (human.money < cost) return ActionResult.fail("Te weinig geld. Nodig: " + Fmt.money(cost));
        int seats = s.nextExpansionSeats();
        addMoney("Stadion", -cost);
        s.constructionSeats = seats;
        s.constructionWeeksLeft = Stadium.buildWeeks(seats);
        addNews("Bouw gestart: " + Fmt.number(seats) + " extra plaatsen, klaar over "
                + s.constructionWeeksLeft + " weken. Tijdens de bouw is een deel van het stadion dicht.");
        return ActionResult.ok("Bouw gestart, klaar over " + s.constructionWeeksLeft + " weken.");
    }

    private void progressConstruction(WeekReport rep) {
        Stadium s = human.stadium;
        if (!s.isUnderConstruction()) return;
        s.constructionWeeksLeft--;
        if (s.constructionWeeksLeft == 0) {
            s.capacity += s.constructionSeats;
            String msg = "De nieuwe tribune is klaar! Capaciteit: " + Fmt.number(s.capacity) + " plaatsen.";
            s.constructionSeats = 0;
            addNews(msg);
            rep.messages.add(msg);
        }
    }

    public enum Facility { COMMERCIEEL, TRAINING, JEUGD }

    public long facilityCost(Facility f) {
        Stadium s = human.stadium;
        switch (f) {
            case COMMERCIEEL: return s.commercialCost();
            case TRAINING: return s.trainingCost();
            default: return s.youthCost();
        }
    }

    public int facilityLevel(Facility f) {
        Stadium s = human.stadium;
        switch (f) {
            case COMMERCIEEL: return s.commercialLevel;
            case TRAINING: return s.trainingLevel;
            default: return s.youthLevel;
        }
    }

    public ActionResult upgradeFacility(Facility f) {
        if (facilityLevel(f) >= Stadium.MAX_LEVEL) return ActionResult.fail("Al op het maximale niveau.");
        long cost = facilityCost(f);
        if (human.money < cost) return ActionResult.fail("Te weinig geld. Nodig: " + Fmt.money(cost));
        addMoney("Faciliteiten", -cost);
        Stadium s = human.stadium;
        switch (f) {
            case COMMERCIEEL: s.commercialLevel++; break;
            case TRAINING: s.trainingLevel++; break;
            default: s.youthLevel++;
        }
        return ActionResult.ok("Niveau " + facilityLevel(f) + " bereikt!");
    }

    public void setTicketPrice(int price) {
        human.stadium.ticketPrice = clamp(price, 1, 150);
    }

    // ---- Bank -------------------------------------------------------------------------

    public ActionResult borrow(long amount) {
        if (amount <= 0) return ActionResult.fail("Ongeldig bedrag.");
        if (human.loan + amount > loanLimit()) {
            return ActionResult.fail("De bank leent maximaal " + Fmt.money(loanLimit()) + " in totaal.");
        }
        human.loan += amount;
        addMoney("Lening", amount);
        return ActionResult.ok(Fmt.money(amount) + " geleend.");
    }

    public ActionResult repay(long amount) {
        amount = Math.min(amount, human.loan);
        if (amount <= 0) return ActionResult.fail("Je hebt geen lening.");
        if (human.money < amount) return ActionResult.fail("Te weinig geld om af te lossen.");
        human.loan -= amount;
        addMoney("Aflossing", -amount);
        return ActionResult.ok(Fmt.money(amount) + " afgelost.");
    }

    // ---- Hulpjes ----------------------------------------------------------------------

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
