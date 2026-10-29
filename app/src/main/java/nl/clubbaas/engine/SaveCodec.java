package nl.clubbaas.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zet een Game om naar JSON en terug. Alles wordt op naam opgeslagen, dus hernoemen of toevoegen
 * van velden in de code maakt oude savegames niet onbruikbaar: ontbrekende velden krijgen een
 * standaardwaarde. Spelers en clubs worden één keer opgeslagen en daarna via hun id gekoppeld.
 *
 * Nieuw veld toevoegen? Schrijf het in de juiste encode-methode en lees het met een standaardwaarde
 * in de bijbehorende decode-methode. Verandert de betekenis van een veld, verhoog dan VERSION en
 * zet een omzetting in decode.
 */
final class SaveCodec {
    static final String FORMAT = "clubbaas";
    static final int VERSION = 1;

    private SaveCodec() {}

    // =================================================================================
    // Schrijven
    // =================================================================================

    static String encode(Game g) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format", FORMAT);
        root.put("version", VERSION);

        List<Club> clubs = new ArrayList<>();
        for (Division d : g.divisions) clubs.addAll(d.clubs);
        clubs.addAll(g.foreignClubs());
        Map<Integer, Player> players = new LinkedHashMap<>();
        for (Club c : clubs) for (Player p : c.squad) players.put(p.id, p);
        for (Player p : g.market) players.put(p.id, p);

        // Spelwereld
        root.put("rng", g.rng instanceof Rng ? ((Rng) g.rng).state() : g.rng.nextLong());
        root.put("chairman", g.chairmanName);
        root.put("human", g.human.id);
        root.put("seasonYear", g.seasonYear);
        root.put("week", g.week);
        root.put("seasonOver", g.seasonOver);
        root.put("humanCupResult", g.humanCupResult);
        root.put("pendingFanFactor", g.pendingFanFactor);
        root.put("gameOver", g.gameOver);
        root.put("gameOverReason", g.gameOverReason);
        root.put("liveDisabled", g.liveDisabled);
        root.put("nextPlayerId", g.nextPlayerId);
        root.put("nextClubId", g.nextClubId);
        root.put("boardInit", g.boardInit);
        root.put("boardObjective", g.boardObjective == null ? null : g.boardObjective.name());
        root.put("boardConfidence", g.boardConfidence);
        root.put("supporterMood", g.supporterMood);
        root.put("highscoreRecorded", g.highscoreRecorded);
        root.put("humanEuropeResult", g.humanEuropeResult);
        List<Object> ach = new ArrayList<>();
        for (Achievement a : g.achievements()) ach.add(a.name());
        root.put("achievements", ach);
        root.put("usedClubNames", new ArrayList<Object>(g.names.usedClubNames));

        List<Object> playerList = new ArrayList<>();
        for (Player p : players.values()) playerList.add(encodePlayer(p));
        root.put("players", playerList);
        List<Object> clubList = new ArrayList<>();
        for (Club c : clubs) clubList.add(encodeClub(c));
        root.put("clubs", clubList);

        List<Object> divs = new ArrayList<>();
        for (Division d : g.divisions) divs.add(encodeDivision(d));
        root.put("divisions", divs);
        root.put("cup", encodeCup(g.cup));
        root.put("europe", g.europe == null ? null : encodeCup(g.europe));
        root.put("europeQualified", g.europeQualified == null ? null : ids(g.europeQualified));
        root.put("foreignClubs", ids(g.foreignClubs()));

        List<Object> market = new ArrayList<>();
        for (Player p : g.market) market.add(p.id);
        root.put("market", market);

        Map<String, Object> cand = new LinkedHashMap<>();
        for (Map.Entry<StaffRole, List<Staff>> e : g.candidates.entrySet()) {
            List<Object> list = new ArrayList<>();
            for (Staff s : e.getValue()) list.add(encodeStaff(s));
            cand.put(e.getKey().name(), list);
        }
        root.put("candidates", cand);

        List<Object> news = new ArrayList<>();
        for (NewsItem n : g.news) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("s", n.season);
            m.put("w", n.week);
            m.put("t", n.text);
            news.add(m);
        }
        root.put("news", news);

        List<Object> loans = new ArrayList<>();
        for (Loan l : g.loans()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("player", l.player.id);
            m.put("club", l.club.id);
            m.put("pct", l.wageSharePct);
            m.put("apps", l.appearancesAtStart);
            m.put("goals", l.goalsAtStart);
            loans.add(m);
        }
        root.put("loans", loans);

        List<Object> bids = new ArrayList<>();
        for (TransferBid b : g.bids()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("player", b.player.id);
            m.put("club", b.club == null ? null : b.club.id);
            m.put("amount", b.amount);
            m.put("solicited", b.solicited);
            m.put("raised", b.raised);
            m.put("expires", b.expiresWeekKey);
            bids.add(m);
        }
        root.put("bids", bids);
        root.put("lastSummary", g.lastSummary == null ? null : encodeSummary(g.lastSummary));
        return Json.write(root);
    }

    private static List<Object> ids(List<Club> clubs) {
        List<Object> out = new ArrayList<>();
        for (Club c : clubs) out.add(c.id);
        return out;
    }

    private static Map<String, Object> encodePlayer(Player p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.id);
        m.put("name", p.name);
        m.put("pos", p.position.name());
        m.put("age", p.age);
        m.put("skill", p.skill);
        m.put("potential", p.potential);
        m.put("wage", p.wage);
        m.put("contract", p.contractYears);
        m.put("injury", p.injuryWeeks);
        m.put("goals", p.goals);
        m.put("apps", p.appearances);
        m.put("yellow", p.yellowCards);
        m.put("suspension", p.suspension);
        m.put("fatigue", p.fatigue);
        m.put("academy", p.academy);
        m.put("asking", p.askingPrice);
        m.put("moraleDelta", p.moraleDelta);
        m.put("playShare", (double) p.playShare);
        m.put("expectation", (double) p.expectation);
        m.put("moraleInit", p.moraleInit);
        m.put("transferRequested", p.transferRequested);
        m.put("loanOfferWeek", p.loanOfferWeekKey);
        m.put("loanOfferClub", p.loanOfferClubId);
        m.put("loanOfferPct", p.loanOfferPct);
        m.put("negoWeek", p.negoWeekKey);
        m.put("negoMin", p.negoMin);
        m.put("negoCounter", p.negoCounter);
        m.put("negoRounds", p.negoRounds);
        m.put("negoClosed", p.negoClosed);
        return m;
    }

    private static Map<String, Object> encodeStaff(Staff s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", s.name);
        m.put("role", s.role.name());
        m.put("rating", s.rating);
        return m;
    }

    private static Map<String, Object> encodeSponsor(SponsorDeal d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sponsor", d.sponsor);
        m.put("type", d.type.name());
        m.put("perSeason", d.perSeason);
        m.put("winBonus", d.winBonus);
        m.put("promotionBonus", d.promotionBonus);
        m.put("seasonsLeft", d.seasonsLeft);
        return m;
    }

    private static Map<String, Object> encodeClub(Club c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.id);
        m.put("name", c.name);
        m.put("color", c.color);
        m.put("level", c.level);
        m.put("human", c.human);
        List<Object> squad = new ArrayList<>();
        for (Player p : c.squad) squad.add(p.id);
        m.put("squad", squad);
        m.put("formation", c.formation == null ? null : c.formation.name());
        m.put("tactic", c.tactic == null ? null : c.tactic.name());
        m.put("favorite", c.favoriteId);
        m.put("frustration", c.managerFrustration);
        m.put("money", c.money);
        m.put("aiFinanceInit", c.aiFinanceInit);
        m.put("loan", c.loan);
        m.put("fans", c.fans);
        m.put("sponsorPerSeason", c.sponsorPerSeason);
        m.put("lastAttendance", c.lastAttendance);
        m.put("negativeWeeks", c.negativeWeeks);
        if (c.stadium != null) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("capacity", c.stadium.capacity);
            s.put("ticketPrice", c.stadium.ticketPrice);
            s.put("commercial", c.stadium.commercialLevel);
            s.put("training", c.stadium.trainingLevel);
            s.put("youth", c.stadium.youthLevel);
            s.put("buildWeeks", c.stadium.constructionWeeksLeft);
            s.put("buildSeats", c.stadium.constructionSeats);
            m.put("stadium", s);
        }
        Map<String, Object> staff = new LinkedHashMap<>();
        for (Map.Entry<StaffRole, Staff> e : c.staff.entrySet()) staff.put(e.getKey().name(), encodeStaff(e.getValue()));
        m.put("staff", staff);
        m.put("sponsorDeal", c.sponsorDeal == null ? null : encodeSponsor(c.sponsorDeal));
        List<Object> offers = new ArrayList<>();
        for (SponsorDeal d : c.sponsorOffers()) offers.add(encodeSponsor(d));
        m.put("sponsorOffers", offers);
        m.put("ledgerWeek", new LinkedHashMap<String, Object>(c.ledger.week));
        m.put("ledgerSeason", new LinkedHashMap<String, Object>(c.ledger.season));
        List<Object> history = new ArrayList<>();
        for (SeasonRecord r : c.history) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("season", r.season);
            h.put("division", r.division);
            h.put("position", r.position);
            h.put("cup", r.cupResult);
            h.put("topScorer", r.topScorer);
            h.put("outcome", r.outcome);
            h.put("europe", r.europeResult);
            history.add(h);
        }
        m.put("history", history);
        return m;
    }

    private static Map<String, Object> encodeFixture(Fixture f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("h", f.home.id);
        m.put("a", f.away.id);
        m.put("cup", f.cup);
        m.put("wk", f.week);
        m.put("hg", f.homeGoals);
        m.put("ag", f.awayGoals);
        m.put("played", f.played);
        m.put("pen", f.penaltyWinner == null ? null : f.penaltyWinner.id);
        m.put("att", f.attendance);
        m.put("hs", new ArrayList<Object>(f.homeScorers));
        m.put("as", new ArrayList<Object>(f.awayScorers));
        m.put("hset", f.homeSetup);
        m.put("aset", f.awaySetup);
        m.put("ff", f.favoriteForced);
        List<Object> events = new ArrayList<>();
        for (MatchEvent e : f.events()) {
            List<Object> ev = new ArrayList<>();
            ev.add(e.minute);
            ev.add(e.type.name());
            ev.add(e.home);
            ev.add(e.player);
            events.add(ev);
        }
        m.put("ev", events);
        return m;
    }

    private static List<Object> encodeRounds(List<List<Fixture>> rounds) {
        List<Object> out = new ArrayList<>();
        for (List<Fixture> r : rounds) {
            List<Object> round = new ArrayList<>();
            for (Fixture f : r) round.add(encodeFixture(f));
            out.add(round);
        }
        return out;
    }

    private static Map<String, Object> encodeDivision(Division d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", d.level);
        m.put("name", d.name);
        m.put("clubs", ids(d.clubs));
        m.put("rounds", encodeRounds(d.rounds));
        List<Object> table = new ArrayList<>();
        for (TableRow r : d.table.values()) {
            List<Object> row = new ArrayList<>();
            row.add(r.club.id);
            row.add(r.played);
            row.add(r.won);
            row.add(r.drawn);
            row.add(r.lost);
            row.add(r.goalsFor);
            row.add(r.goalsAgainst);
            table.add(row);
        }
        m.put("table", table);
        return m;
    }

    private static Map<String, Object> encodeCup(Cup cup) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("remaining", ids(cup.remaining));
        m.put("roundNames", new ArrayList<Object>(cup.roundNames));
        m.put("rounds", encodeRounds(cup.rounds));
        m.put("winner", cup.winner == null ? null : cup.winner.id);
        return m;
    }

    private static Map<String, Object> encodeSummary(SeasonSummary s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("season", s.season);
        m.put("division", s.divisionName);
        m.put("position", s.position);
        m.put("outcome", s.outcome);
        m.put("cup", s.cupResult);
        m.put("prize", s.prizeMoney);
        m.put("champions", new ArrayList<Object>(s.champions));
        m.put("cupWinner", s.cupWinner);
        m.put("board", s.boardVerdict);
        m.put("europe", s.europeResult);
        m.put("europeQualified", s.europeQualified == null ? new ArrayList<Object>() : new ArrayList<Object>(s.europeQualified));
        m.put("europeWinner", s.europeWinner);
        return m;
    }

    // =================================================================================
    // Lezen
    // =================================================================================

    @SuppressWarnings("unchecked")
    static Game decode(String json) {
        Object parsed = Json.parse(json);
        if (!(parsed instanceof Map)) throw new IllegalArgumentException("Geen geldige savegame");
        Map<String, Object> root = (Map<String, Object>) parsed;
        if (!FORMAT.equals(root.get("format"))) throw new IllegalArgumentException("Geen Clubbaas-savegame");
        int version = i(root, "version", 0);
        if (version > VERSION) {
            throw new IllegalArgumentException("Deze savegame komt uit een nieuwere versie van de app");
        }

        Game g = new Game();
        ((Rng) g.rng).setState(l(root, "rng", 0));
        g.chairmanName = s(root, "chairman", "Voorzitter");
        g.seasonYear = i(root, "seasonYear", 2026);
        g.week = i(root, "week", 0);
        g.seasonOver = b(root, "seasonOver", false);
        g.humanCupResult = s(root, "humanCupResult", "Nog in de race");
        g.pendingFanFactor = d(root, "pendingFanFactor", 1.0);
        g.gameOver = b(root, "gameOver", false);
        g.gameOverReason = s(root, "gameOverReason", null);
        g.liveDisabled = b(root, "liveDisabled", false);
        g.nextPlayerId = i(root, "nextPlayerId", 1);
        g.nextClubId = i(root, "nextClubId", 1);
        g.boardInit = b(root, "boardInit", false);
        g.boardObjective = enumValue(BoardObjective.class, s(root, "boardObjective", null), null);
        g.boardConfidence = i(root, "boardConfidence", Game.BOARD_START);
        g.supporterMood = i(root, "supporterMood", Game.SUPPORTERS_NEUTRAL);
        g.highscoreRecorded = b(root, "highscoreRecorded", false);
        g.humanEuropeResult = s(root, "humanEuropeResult", null);
        for (Object o : list(root, "achievements")) {
            Achievement a = enumValue(Achievement.class, (String) o, null);
            if (a != null) g.achievements().add(a);
        }
        for (Object o : list(root, "usedClubNames")) g.names.usedClubNames.add((String) o);

        // Spelers en clubs
        Map<Integer, Player> players = new HashMap<>();
        for (Object o : list(root, "players")) {
            Player p = decodePlayer((Map<String, Object>) o);
            players.put(p.id, p);
        }
        Map<Integer, Club> clubs = new HashMap<>();
        for (Object o : list(root, "clubs")) {
            Club c = decodeClub((Map<String, Object>) o, players);
            clubs.put(c.id, c);
        }
        g.human = clubs.get(i(root, "human", -1));
        if (g.human == null) throw new IllegalArgumentException("Eigen club ontbreekt in de savegame");

        for (Object o : list(root, "divisions")) {
            Map<String, Object> dm = (Map<String, Object>) o;
            Division d = new Division(i(dm, "level", 1), s(dm, "name", ""));
            for (Object id : list(dm, "clubs")) d.clubs.add(club(clubs, id));
            d.rounds.addAll(decodeRounds(list(dm, "rounds"), clubs));
            for (Object r : list(dm, "table")) {
                List<Object> row = (List<Object>) r;
                Club c = club(clubs, row.get(0));
                TableRow t = new TableRow(c);
                t.played = num(row.get(1));
                t.won = num(row.get(2));
                t.drawn = num(row.get(3));
                t.lost = num(row.get(4));
                t.goalsFor = num(row.get(5));
                t.goalsAgainst = num(row.get(6));
                d.table.put(c.id, t);
            }
            g.divisions.add(d);
        }
        g.cup = decodeCup(map(root, "cup"), clubs);
        Map<String, Object> eu = map(root, "europe");
        g.europe = eu == null ? null : decodeCup(eu, clubs);
        if (root.get("europeQualified") != null) {
            g.europeQualified = new ArrayList<>();
            for (Object id : list(root, "europeQualified")) g.europeQualified.add(club(clubs, id));
        }
        g.foreignClubs = new ArrayList<>();
        for (Object id : list(root, "foreignClubs")) g.foreignClubs.add(club(clubs, id));

        for (Object id : list(root, "market")) {
            Player p = players.get(num(id));
            if (p != null) g.market.add(p);
        }
        Map<String, Object> cand = map(root, "candidates");
        if (cand != null) {
            for (Map.Entry<String, Object> e : cand.entrySet()) {
                StaffRole role = enumValue(StaffRole.class, e.getKey(), null);
                if (role == null) continue;
                List<Staff> list = new ArrayList<>();
                for (Object s : (List<Object>) e.getValue()) list.add(decodeStaff((Map<String, Object>) s));
                g.candidates.put(role, list);
            }
        }
        for (Object o : list(root, "news")) {
            Map<String, Object> n = (Map<String, Object>) o;
            g.news.add(new NewsItem(s(n, "s", ""), i(n, "w", 0), s(n, "t", "")));
        }
        for (Object o : list(root, "loans")) {
            Map<String, Object> m = (Map<String, Object>) o;
            Player p = players.get(i(m, "player", -1));
            Club c = clubs.get(i(m, "club", -1));
            if (p != null && c != null) {
                g.loans().add(new Loan(p, c, i(m, "pct", 50), i(m, "apps", 0), i(m, "goals", 0)));
            }
        }
        for (Object o : list(root, "bids")) {
            Map<String, Object> m = (Map<String, Object>) o;
            Player p = players.get(i(m, "player", -1));
            if (p == null) continue;
            Club c = m.get("club") == null ? null : clubs.get(i(m, "club", -1));
            TransferBid bid = new TransferBid(p, c, l(m, "amount", 0), b(m, "solicited", false), i(m, "expires", 0));
            bid.raised = b(m, "raised", false);
            g.bids().add(bid);
        }
        Map<String, Object> sum = map(root, "lastSummary");
        if (sum != null) g.lastSummary = decodeSummary(sum);
        return g;
    }

    private static Player decodePlayer(Map<String, Object> m) {
        Player p = new Player(i(m, "id", 0), s(m, "name", "?"),
                enumValue(Position.class, s(m, "pos", null), Position.MIDDENVELDER),
                i(m, "age", 25), i(m, "skill", 30), i(m, "potential", 30));
        p.wage = i(m, "wage", p.wage);
        p.contractYears = i(m, "contract", 1);
        p.injuryWeeks = i(m, "injury", 0);
        p.goals = i(m, "goals", 0);
        p.appearances = i(m, "apps", 0);
        p.yellowCards = i(m, "yellow", 0);
        p.suspension = i(m, "suspension", 0);
        p.fatigue = i(m, "fatigue", 0);
        p.academy = b(m, "academy", false);
        p.askingPrice = l(m, "asking", 0);
        p.moraleDelta = i(m, "moraleDelta", 0);
        p.playShare = (float) d(m, "playShare", 0);
        p.expectation = (float) d(m, "expectation", 0);
        p.moraleInit = b(m, "moraleInit", false);
        p.transferRequested = b(m, "transferRequested", false);
        p.loanOfferWeekKey = i(m, "loanOfferWeek", -1);
        p.loanOfferClubId = i(m, "loanOfferClub", 0);
        p.loanOfferPct = i(m, "loanOfferPct", 0);
        p.negoWeekKey = i(m, "negoWeek", -1);
        p.negoMin = l(m, "negoMin", 0);
        p.negoCounter = l(m, "negoCounter", 0);
        p.negoRounds = i(m, "negoRounds", 0);
        p.negoClosed = b(m, "negoClosed", false);
        return p;
    }

    private static Staff decodeStaff(Map<String, Object> m) {
        return new Staff(s(m, "name", "?"), enumValue(StaffRole.class, s(m, "role", null), StaffRole.MANAGER),
                Math.max(1, Math.min(5, i(m, "rating", 1))));
    }

    private static SponsorDeal decodeSponsor(Map<String, Object> m) {
        return new SponsorDeal(s(m, "sponsor", "Sponsor"),
                enumValue(SponsorDeal.Type.class, s(m, "type", null), SponsorDeal.Type.EENJARIG),
                l(m, "perSeason", 0), l(m, "winBonus", 0), l(m, "promotionBonus", 0), i(m, "seasonsLeft", 1));
    }

    @SuppressWarnings("unchecked")
    private static Club decodeClub(Map<String, Object> m, Map<Integer, Player> players) {
        Club c = new Club(i(m, "id", 0), s(m, "name", "?"), i(m, "color", Game.PALETTE[0]),
                i(m, "level", Game.DIVISIONS), b(m, "human", false));
        for (Object id : list(m, "squad")) {
            Player p = players.get(num(id));
            if (p != null) c.squad.add(p);
        }
        c.formation = enumValue(Formation.class, s(m, "formation", null), null);
        c.tactic = enumValue(Tactic.class, s(m, "tactic", null), null);
        c.favoriteId = i(m, "favorite", 0);
        c.managerFrustration = i(m, "frustration", 0);
        c.money = l(m, "money", 0);
        c.aiFinanceInit = b(m, "aiFinanceInit", false);
        c.loan = l(m, "loan", 0);
        c.fans = i(m, "fans", 0);
        c.sponsorPerSeason = l(m, "sponsorPerSeason", 0);
        c.lastAttendance = i(m, "lastAttendance", 0);
        c.negativeWeeks = i(m, "negativeWeeks", 0);
        Map<String, Object> s = map(m, "stadium");
        if (s != null || c.human) {
            Stadium st = new Stadium();
            if (s != null) {
                st.capacity = i(s, "capacity", st.capacity);
                st.ticketPrice = i(s, "ticketPrice", st.ticketPrice);
                st.commercialLevel = i(s, "commercial", 0);
                st.trainingLevel = i(s, "training", 0);
                st.youthLevel = i(s, "youth", 0);
                st.constructionWeeksLeft = i(s, "buildWeeks", 0);
                st.constructionSeats = i(s, "buildSeats", 0);
            }
            c.stadium = st;
        }
        Map<String, Object> staff = map(m, "staff");
        if (staff != null) {
            for (Map.Entry<String, Object> e : staff.entrySet()) {
                StaffRole role = enumValue(StaffRole.class, e.getKey(), null);
                if (role != null) c.staff.put(role, decodeStaff((Map<String, Object>) e.getValue()));
            }
        }
        Map<String, Object> deal = map(m, "sponsorDeal");
        if (deal != null) c.sponsorDeal = decodeSponsor(deal);
        for (Object o : list(m, "sponsorOffers")) c.sponsorOffers().add(decodeSponsor((Map<String, Object>) o));
        Map<String, Object> lw = map(m, "ledgerWeek");
        if (lw != null) for (Map.Entry<String, Object> e : lw.entrySet()) c.ledger.week.put(e.getKey(), ((Number) e.getValue()).longValue());
        Map<String, Object> ls = map(m, "ledgerSeason");
        if (ls != null) for (Map.Entry<String, Object> e : ls.entrySet()) c.ledger.season.put(e.getKey(), ((Number) e.getValue()).longValue());
        for (Object o : list(m, "history")) {
            Map<String, Object> h = (Map<String, Object>) o;
            SeasonRecord r = new SeasonRecord(s(h, "season", ""), s(h, "division", ""), i(h, "position", 0),
                    s(h, "cup", "-"), s(h, "topScorer", "-"), s(h, "outcome", ""));
            r.europeResult = s(h, "europe", null);
            c.history.add(r);
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    private static Fixture decodeFixture(Map<String, Object> m, Map<Integer, Club> clubs) {
        Fixture f = new Fixture(club(clubs, m.get("h")), club(clubs, m.get("a")), b(m, "cup", false), i(m, "wk", 0));
        f.homeGoals = i(m, "hg", 0);
        f.awayGoals = i(m, "ag", 0);
        f.played = b(m, "played", false);
        f.penaltyWinner = m.get("pen") == null ? null : club(clubs, m.get("pen"));
        f.attendance = i(m, "att", 0);
        for (Object o : list(m, "hs")) f.homeScorers.add((String) o);
        for (Object o : list(m, "as")) f.awayScorers.add((String) o);
        f.homeSetup = s(m, "hset", null);
        f.awaySetup = s(m, "aset", null);
        f.favoriteForced = b(m, "ff", false);
        for (Object o : list(m, "ev")) {
            List<Object> ev = (List<Object>) o;
            MatchEvent.Type type = enumValue(MatchEvent.Type.class, (String) ev.get(1), null);
            if (type == null) continue;
            f.events().add(new MatchEvent(num(ev.get(0)), type, Boolean.TRUE.equals(ev.get(2)), (String) ev.get(3)));
        }
        return f;
    }

    @SuppressWarnings("unchecked")
    private static List<List<Fixture>> decodeRounds(List<Object> rounds, Map<Integer, Club> clubs) {
        List<List<Fixture>> out = new ArrayList<>();
        for (Object r : rounds) {
            List<Fixture> round = new ArrayList<>();
            for (Object f : (List<Object>) r) round.add(decodeFixture((Map<String, Object>) f, clubs));
            out.add(round);
        }
        return out;
    }

    private static Cup decodeCup(Map<String, Object> m, Map<Integer, Club> clubs) {
        List<Club> remaining = new ArrayList<>();
        if (m != null) for (Object id : list(m, "remaining")) remaining.add(club(clubs, id));
        Cup cup = new Cup(remaining);
        if (m == null) return cup;
        for (Object o : list(m, "roundNames")) cup.roundNames.add((String) o);
        cup.rounds.addAll(decodeRounds(list(m, "rounds"), clubs));
        cup.winner = m.get("winner") == null ? null : club(clubs, m.get("winner"));
        return cup;
    }

    private static SeasonSummary decodeSummary(Map<String, Object> m) {
        SeasonSummary s = new SeasonSummary();
        s.season = s(m, "season", "");
        s.divisionName = s(m, "division", "");
        s.position = i(m, "position", 0);
        s.outcome = s(m, "outcome", "");
        s.cupResult = s(m, "cup", "");
        s.prizeMoney = l(m, "prize", 0);
        for (Object o : list(m, "champions")) s.champions.add((String) o);
        s.cupWinner = s(m, "cupWinner", null);
        s.boardVerdict = s(m, "board", null);
        s.europeResult = s(m, "europe", null);
        for (Object o : list(m, "europeQualified")) s.europeQualified.add((String) o);
        s.europeWinner = s(m, "europeWinner", null);
        return s;
    }

    // ---- Hulpjes voor robuust lezen ----------------------------------------------------

    private static Club club(Map<Integer, Club> clubs, Object id) {
        Club c = clubs.get(num(id));
        if (c == null) throw new IllegalArgumentException("Onbekende club-id in savegame: " + id);
        return c;
    }

    private static int num(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : -1;
    }

    private static int i(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private static long l(Map<String, Object> m, String k, long def) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).longValue() : def;
    }

    private static double d(Map<String, Object> m, String k, double def) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    private static boolean b(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    private static String s(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v instanceof String ? (String) v : def;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof List ? (List<Object>) v : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String name, E def) {
        if (name == null) return def;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
