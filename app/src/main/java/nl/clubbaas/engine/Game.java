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
    private int nextPlayerId = 1;
    private int nextClubId = 1;

    public Game(String chairmanName, String clubName, int clubColor, int startYear, long seed) {
        this.chairmanName = chairmanName;
        this.rng = new Random(seed);
        this.seasonYear = startYear;
        createWorld(clubName.trim().isEmpty() ? "FC Clubbaas" : clubName.trim(), clubColor);
        startSeasonCommon();
        generateSponsorOffers();
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
            }
        }
        human.ledger.startSeason();
        market.clear();
        for (int i = 0; i < MARKET_SIZE; i++) market.add(newMarketPlayer());
        refreshCandidates();
        week = 0;
        seasonOver = false;
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
        int demand = (int) (human.fans * factor);
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
        for (Club c : allClubs()) for (Player p : c.squad) if (p.injuryWeeks > 0) p.injuryWeeks--;

        // Competitie
        for (Division d : divisions) {
            for (Fixture f : d.round(week)) {
                MatchEngine.play(f, rng, false);
                d.record(f);
                if (f.involves(human)) {
                    rep.leagueMatch = f;
                    if (f.home == human) collectGate(f);
                    updateFans(f);
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
                addNews("FEEST! " + human.name + " wint de " + Cup.NAME + "!");
            }
            if (cup.isFinished() && cup.winner != human) {
                addNews(cup.winner.name + " wint de " + Cup.NAME + ".");
            }
        }

        updateMorale(appearancesBefore);
        progressConstruction(rep);
        weeklyFinances();
        weeklyMarketRefresh();
        if (week % 4 == 1) refreshCandidates();

        if (week == WEEKS_PER_SEASON - 4) {
            int expiring = 0;
            for (Player p : human.squad) if (p.contractYears == 0) expiring++;
            if (expiring > 0) addNews(expiring + " speler(s) hebben een aflopend contract. "
                    + "Verleng op tijd, anders vertrekken ze aan het eind van het seizoen.");
        }

        checkBankruptcy(rep);

        if (week == WEEKS_PER_SEASON) {
            seasonOver = true;
            lastSummary = buildSeasonSummary();
            rep.seasonSummary = lastSummary;
        }
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

        Player top = human.topScorer();
        String shortOutcome = s.outcome.startsWith("LANDS") ? "Landskampioen"
                : s.outcome.startsWith("Kampioen") ? "Kampioen"
                : s.outcome.startsWith("Promotie") ? "Promotie"
                : s.outcome.startsWith("Degradatie") ? "Degradatie" : "";
        human.history.add(new SeasonRecord(s.season, d.name, pos, humanCupResult,
                top == null ? "-" : top.name + " (" + top.goals + ")", shortOutcome));
        addNews("Einde seizoen: " + pos + "e plaats. " + s.outcome);
        return s;
    }

    private void startNewSeason() {
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

        // 3. AI-clubs op sterkte houden
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

    private void maintainAiSquad(Club c) {
        int target = targetSkill(c.level);
        fillAiSquad(c);
        if (c.averageSkill() < target - 3) {
            for (int i = 0; i < 3; i++) {
                Player worst = c.worstPlayer();
                c.squad.remove(worst);
                c.squad.add(newPlayer(worst.position, target, 4, 20, 30));
            }
        } else if (c.averageSkill() > target + 4) {
            // Te sterke club (bijv. net gedegradeerd): verkoopt zijn twee beste spelers
            for (int i = 0; i < 2; i++) {
                Player best = null;
                for (Player p : c.squad) if (best == null || p.skill > best.skill) best = p;
                c.squad.remove(best);
                c.squad.add(newPlayer(best.position, target, 4, 20, 30));
            }
        }
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

    public ActionResult buyPlayer(Player p) {
        if (!market.contains(p)) return ActionResult.fail("Deze speler is niet meer beschikbaar.");
        if (human.squad.size() >= MAX_SQUAD) return ActionResult.fail("Je selectie zit vol (max " + MAX_SQUAD + ").");
        long cost = signingCost(p);
        if (human.money < cost) return ActionResult.fail("Te weinig geld. Nodig: " + Fmt.money(cost));
        addMoney(p.isFreeAgent() ? "Tekengeld" : "Transfers (aankoop)", -cost);
        market.remove(p);
        p.askingPrice = 0;
        p.contractYears = 1 + rng.nextInt(3);
        p.goals = 0;
        p.appearances = 0;
        p.moraleDelta = 10;       // enthousiast over zijn nieuwe club
        p.moraleInit = false;     // speeltijd begint bij de verwachting voor zijn rol
        p.transferRequested = false;
        human.squad.add(p);
        addNews(p.name + " tekent bij " + human.name + " voor " + Fmt.money(cost) + ".");
        return ActionResult.ok(p.name + " is jouw nieuwe aanwinst!");
    }

    /** Bod van een andere club. Per speler één bod per week. */
    public long saleOffer(Player p) {
        int key = seasonYear * 100 + week;
        if (p.saleOfferWeekKey != key) {
            double f = 0.7 + rng.nextDouble() * 0.45;
            if (isShowcased(p)) f *= 1.10;
            if (p.transferRequested) f *= 0.90; // iedereen weet dat hij weg wil
            p.saleOffer = Math.max(500, Math.round(p.value() * f / 500.0) * 500);
            p.saleOfferWeekKey = key;
        }
        return p.saleOffer;
    }

    /** Speelt genoeg om op te vallen bij andere clubs: +10% op verkoopbiedingen. */
    public static boolean isShowcased(Player p) {
        return p.appearances >= SHOWCASE_APPS;
    }

    public ActionResult sellPlayer(Player p) {
        if (!human.squad.contains(p)) return ActionResult.fail("Speler zit niet in je selectie.");
        if (human.squad.size() <= MIN_SQUAD) return ActionResult.fail("Je hebt minimaal " + MIN_SQUAD + " spelers nodig.");
        long offer = saleOffer(p);
        // Koper: een club uit dezelfde of een hogere divisie
        int buyerLevel = Math.max(1, human.level - rng.nextInt(2));
        List<Club> options = new ArrayList<>(division(buyerLevel).clubs);
        options.remove(human);
        Club buyer = options.get(rng.nextInt(options.size()));
        human.squad.remove(p);
        p.contractYears = 2 + rng.nextInt(2);
        p.wage = Player.wageFor(p.skill, p.age);
        buyer.squad.add(p);
        if (buyer.squad.size() > 22) buyer.squad.remove(buyer.worstPlayer());
        addMoney("Transfers (verkoop)", offer);
        addNews(p.name + " verkocht aan " + buyer.name + " voor " + Fmt.money(offer) + ".");
        return ActionResult.ok(p.name + " vertrekt naar " + buyer.name + ".");
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
        ctx.naturalEleven.addAll(MatchEngine.rate(human, human.formation(), false).eleven());
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
