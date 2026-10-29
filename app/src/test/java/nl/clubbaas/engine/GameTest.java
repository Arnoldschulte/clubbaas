package nl.clubbaas.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/** Simuleert seizoenen zonder Android, om de spelregels te controleren. */
public class GameTest {

    private static Game newGame() {
        return new Game("Test", "FC Test", Game.PALETTE[0], 2026, 42L);
    }

    private static SeasonSummary playSeason(Game g) {
        SeasonSummary summary = null;
        while (summary == null) summary = g.advanceWeek().seasonSummary;
        return summary;
    }

    @Test
    public void newGameStartsInLowestDivision() {
        Game g = newGame();
        assertEquals(Game.DIVISIONS, g.human().level);
        assertEquals(0, g.week());
        assertTrue(g.human().squad.size() >= Game.MIN_SQUAD);
    }

    @Test
    public void seasonHas22WeeksAndEndsWithSummary() {
        Game g = newGame();
        SeasonSummary s = playSeason(g);
        assertEquals(Game.WEEKS_PER_SEASON, g.week());
        assertTrue(g.isSeasonOver());
        assertNotNull(s.outcome);
        assertTrue(g.cup().isFinished());
    }

    @Test
    public void divisionsStayCompleteAfterPromotionAndRelegation() {
        Game g = newGame();
        for (int season = 0; season < 3; season++) {
            playSeason(g);
            g.advanceWeek(); // start nieuw seizoen
            Set<Club> seen = new HashSet<>();
            for (Division d : g.divisions()) {
                assertEquals(Game.CLUBS_PER_DIVISION, d.clubs.size());
                for (Club c : d.clubs) {
                    assertTrue("Club staat dubbel: " + c.name, seen.add(c));
                    assertEquals(d.level, c.level);
                }
            }
        }
    }

    @Test
    public void buyingAPlayerCostsMoney() {
        Game g = newGame();
        g.human().money = 10_000_000;
        g.setFormation(Formation.F433);
        Player target = g.market().get(0);
        long cost = g.signingCost(target);
        long before = g.human().money;
        assertTrue(g.buyPlayer(target).ok);
        assertEquals(before - cost, g.human().money);
        assertTrue(g.human().squad.contains(target));
        assertFalse(g.market().contains(target));
    }

    @Test
    public void defensiveFormationTradesAttackForDefense() {
        Club me = newGame().human();
        TeamRating defensive = MatchEngine.rate(me, Formation.F532);
        TeamRating attacking = MatchEngine.rate(me, Formation.F433);
        assertTrue(defensive.defense > attacking.defense);
        assertTrue(attacking.attack > defensive.attack);
        assertEquals(11, MatchEngine.rate(me, Formation.F451).slots.size());
    }

    @Test
    public void attackingTacticLeadsToMoreGoalsThanDefensive() {
        Game g = newGame();
        Club me = g.human();
        Club opponent = g.humanDivision().clubs.get(0) == me
                ? g.humanDivision().clubs.get(1) : g.humanDivision().clubs.get(0);
        java.util.Random rng = new java.util.Random(7);
        int attackingGoals = 0, defensiveGoals = 0;
        for (int i = 0; i < 1500; i++) {
            g.setTactic(Tactic.AANVALLEND);
            attackingGoals += playNeutral(me, opponent, rng);
            g.setTactic(Tactic.VERDEDIGEND);
            defensiveGoals += playNeutral(me, opponent, rng);
        }
        assertTrue("aanvallend " + attackingGoals + " vs verdedigend " + defensiveGoals,
                attackingGoals > defensiveGoals * 1.2);
    }

    private static int playNeutral(Club a, Club b, java.util.Random rng) {
        Fixture f = new Fixture(a, b, false, 1);
        MatchEngine.play(f, rng, true);
        for (Player p : a.squad) p.injuryWeeks = 0;
        for (Player p : b.squad) p.injuryWeeks = 0;
        return f.homeGoals + f.awayGoals;
    }

    /** De zwakste fitte speler van de selectie: die zou de manager zelf nooit opstellen. */
    private static Player weakestPlayer(Club c) {
        Player worst = null;
        for (Player p : c.squad) {
            if (p.position != Position.DOELMAN && (worst == null || p.skill < worst.skill)) worst = p;
        }
        return worst;
    }

    @Test
    public void favoriteStartsEvenIfManagerWouldNotPickHim() {
        Game g = newGame();
        Club me = g.human();
        Player bench = weakestPlayer(me);
        bench.skill = 1;
        assertFalse(MatchEngine.rate(me).eleven().contains(bench));
        g.setFavorite(bench);
        TeamRating r = MatchEngine.rate(me);
        assertTrue(r.eleven().contains(bench));
        assertTrue(r.favoriteForced);
        assertEquals(11, r.slots.size());
    }

    @Test
    public void managerResignsAfterTooMuchMeddling() {
        Game g = newGame();
        Club me = g.human();
        Player bench = weakestPlayer(me);
        bench.skill = 1;
        g.setFavorite(bench);
        boolean resigned = false;
        for (int i = 0; i < 20 && !resigned; i++) {
            // Iedereen beschikbaar, zodat de manager de voorkeursspeler zelf nooit zou kiezen
            for (Player p : me.squad) {
                p.injuryWeeks = 0;
                p.suspension = 0;
            }
            g.advanceWeek();
            resigned = me.staff.get(StaffRole.MANAGER) == null;
        }
        assertTrue("manager had moeten opstappen", resigned);
        assertEquals(0, me.managerFrustration());
        assertTrue(me.favorite() == null);
    }

    @Test
    public void noFrustrationWhenFavoriteWouldStartAnyway() {
        Game g = newGame();
        Club me = g.human();
        Player star = MatchEngine.rate(me).eleven().get(0);
        star.skill = 99;
        g.setFavorite(star);
        for (int i = 0; i < 10; i++) {
            star.injuryWeeks = 0;
            g.advanceWeek();
        }
        assertEquals(0, me.managerFrustration());
        assertNotNull(me.staff.get(StaffRole.MANAGER));
    }

    // ---- Moraal ------------------------------------------------------------------------

    @Test
    public void starterPushedOutByFavoriteBecomesUnhappy() {
        Game g = newGame();
        Club me = g.human();
        me.staff.remove(StaffRole.MANAGER); // geen manager die opstapt tijdens de test
        Player bench = weakestPlayer(me);
        bench.skill = 1;
        java.util.List<Player> before = MatchEngine.rate(me).eleven();
        g.setFavorite(bench);
        java.util.List<Player> after = MatchEngine.rate(me).eleven();
        Player pushedOut = null;
        for (Player p : before) if (!after.contains(p)) pushedOut = p;
        assertNotNull(pushedOut);
        for (int i = 0; i < 10; i++) {
            for (Player p : me.squad) p.injuryWeeks = 0;
            g.advanceWeek();
        }
        assertTrue("moraal " + pushedOut.morale(), pushedOut.morale() < 50);
        assertTrue(g.moraleBreakdown(pushedOut).playingTime <= -15);
    }

    @Test
    public void underpaidPlayerIsHappyAgainAfterRenewal() {
        Game g = newGame();
        Player p = MatchEngine.rate(g.human()).eleven().get(1);
        p.wage = 30;
        assertTrue(g.moraleBreakdown(p).salary < 0);
        g.human().money = 1_000_000;
        assertTrue(g.renewContract(p, 2).ok);
        assertTrue(g.moraleBreakdown(p).salary >= 0);
    }

    @Test
    public void higherSalaryIsAcceptedWhenTeammateIsBetter() {
        Game g = newGame();
        Club me = g.human();
        java.util.List<Player> xi = MatchEngine.rate(me).eleven();
        Player a = xi.get(1);
        Player b = null; // een aanvaller: bij hem tellen doelpunten mee als prestatie
        for (Player p : xi) if (p.position == Position.AANVALLER) b = p;
        assertNotNull(b);
        a.age = 26;
        b.wage = a.wage * 3;

        b.skill = a.skill + 5; // beter: salaris gegund
        assertEquals(0, g.moraleBreakdown(a).jealousy);

        b.skill = Math.max(1, a.skill - 12); // duidelijk slechter en scoort niet
        b.goals = 0;
        assertTrue(g.moraleBreakdown(a).jealousy < 0);

        b.goals = 20; // maar als hij volop scoort, wordt het hem eerder gegund
        b.skill = a.skill - 4;
        assertEquals(0, g.moraleBreakdown(a).jealousy);
    }

    @Test
    public void veryUnhappyPlayerAsksForTransferAndRefusesToRenew() {
        Game g = newGame();
        Player p = g.human().squad.get(3);
        p.moraleDelta = -55;
        g.advanceWeek();
        assertTrue(p.wantsTransfer());
        g.human().money = 1_000_000;
        assertFalse(g.renewContract(p, 2).ok);
    }

    @Test
    public void moraleAffectsTeamStrength() {
        Club me = newGame().human();
        double neutral = MatchEngine.strength(me);
        for (Player p : me.squad) p.moraleDelta = 40;
        assertTrue(MatchEngine.strength(me) > neutral);
        for (Player p : me.squad) p.moraleDelta = -50;
        assertTrue(MatchEngine.strength(me) < neutral);
    }

    // ---- Stadion -----------------------------------------------------------------------

    @Test
    public void stadiumExpansionTakesWeeksAndReducesCapacityMeanwhile() {
        Game g = newGame();
        Stadium s = g.human().stadium;
        g.human().money = 1_000_000;
        int before = s.capacity;
        int seats = s.nextExpansionSeats();
        assertTrue(g.expandStadium().ok);
        assertEquals(before, s.capacity);
        assertTrue(s.effectiveCapacity() < before);
        assertFalse(g.expandStadium().ok); // niet twee bouwprojecten tegelijk
        int weeks = Stadium.buildWeeks(seats);
        for (int i = 0; i < weeks - 1; i++) g.advanceWeek();
        assertTrue(s.isUnderConstruction());
        g.advanceWeek();
        assertFalse(s.isUnderConstruction());
        assertEquals(before + seats, s.capacity);
        assertEquals(s.capacity, s.effectiveCapacity());
    }

    // ---- Sponsor ----------------------------------------------------------------------

    @Test
    public void newGameOffersThreeSponsorDealsAndAutoPicksOneYear() {
        Game g = newGame();
        assertEquals(3, g.human().sponsorOffers().size());
        g.advanceWeek();
        assertTrue(g.human().sponsorOffers().isEmpty());
        assertEquals(SponsorDeal.Type.EENJARIG, g.human().sponsorDeal().type);
    }

    @Test
    public void multiYearDealLastsThreeSeasons() {
        Game g = newGame();
        SponsorDeal multi = null;
        for (SponsorDeal d : g.human().sponsorOffers()) if (d.type == SponsorDeal.Type.MEERJARIG) multi = d;
        assertTrue(g.chooseSponsor(multi).ok);
        long amount = multi.perSeason;
        for (int season = 0; season < 2; season++) {
            playSeason(g);
            g.advanceWeek();
            assertTrue(g.human().sponsorOffers().isEmpty());
            assertEquals(amount, g.human().sponsorPerSeason);
        }
        playSeason(g);
        g.advanceWeek(); // na drie seizoenen: nieuwe aanbiedingen
        assertEquals(3, g.human().sponsorOffers().size());
    }

    @Test
    public void performanceDealPaysWinBonus() {
        Game g = newGame();
        SponsorDeal perf = null;
        for (SponsorDeal d : g.human().sponsorOffers()) if (d.type == SponsorDeal.Type.PRESTATIE) perf = d;
        g.chooseSponsor(perf);
        long bonuses = 0;
        for (int i = 0; i < Game.WEEKS_PER_SEASON; i++) {
            g.advanceWeek();
            Long b = g.human().ledger.week().get("Sponsorbonus");
            if (b != null) bonuses += b;
        }
        int wins = g.humanDivision().sortedTable().get(g.humanDivision().positionOf(g.human()) - 1).won;
        assertTrue(bonuses >= wins * perf.winBonus);
    }

    // ---- Kaarten ----------------------------------------------------------------------

    @Test
    public void cardRatesAreRealistic() {
        Game g = newGame();
        Club a = g.division(3).clubs.get(0);
        Club b = g.division(3).clubs.get(1);
        java.util.Random rng = new java.util.Random(3);
        int yellows = 0, reds = 0, matches = 2000;
        for (int i = 0; i < matches; i++) {
            Fixture f = new Fixture(a, b, false, 1);
            MatchEngine.play(f, rng, false);
            for (MatchEvent e : f.events()) {
                if (e.type == MatchEvent.Type.YELLOW) yellows++;
                if (e.type == MatchEvent.Type.RED || e.type == MatchEvent.Type.SECOND_YELLOW) reds++;
            }
            for (Player p : a.squad) p.injuryWeeks = 0;
            for (Player p : b.squad) p.injuryWeeks = 0;
        }
        double y = yellows / (double) matches, r = reds / (double) matches;
        assertTrue("geel per wedstrijd " + y, y > 1.0 && y < 2.5);
        assertTrue("rood per wedstrijd " + r, r > 0.05 && r < 0.4);
    }

    @Test
    public void suspendedPlayerSitsOutOneMatch() {
        Game g = newGame();
        Club me = g.human();
        Player star = MatchEngine.rate(me).eleven().get(0);
        star.suspension = 1;
        assertFalse(MatchEngine.rate(me).eleven().contains(star));
        Club opp = g.humanDivision().clubs.get(0) == me ? g.humanDivision().clubs.get(1) : g.humanDivision().clubs.get(0);
        int apps = star.appearances;
        MatchEngine.play(new Fixture(me, opp, false, 1), new java.util.Random(1), false);
        assertEquals(apps, star.appearances);
        assertEquals(0, star.suspension);
    }

    // ---- Conditie ---------------------------------------------------------------------

    @Test
    public void playingCostsFitnessAndRestRestoresIt() {
        Game g = newGame();
        Club me = g.human();
        Player p = MatchEngine.rate(me).eleven().get(0);
        assertEquals(100, p.fitness());
        Club opp = g.division(7).clubs.get(0) == me ? g.division(7).clubs.get(1) : g.division(7).clubs.get(0);
        MatchEngine.play(new Fixture(me, opp, false, 1), new java.util.Random(2), false);
        assertTrue(p.fitness() < 100);
        for (int i = 0; i < 5; i++) MatchEngine.recover(me);
        assertEquals(100, p.fitness());
    }

    @Test
    public void tiredStarterIsRestedForCloseAlternative() {
        Game g = newGame();
        Club me = g.human();
        TeamRating r = MatchEngine.rate(me);
        Player starter = null, reserve = null;
        for (Player p : r.eleven()) if (p.position == Position.AANVALLER) starter = p;
        for (Player p : me.squad) if (p.position == Position.AANVALLER && !r.eleven().contains(p)) reserve = p;
        assertNotNull(starter);
        assertNotNull(reserve);
        reserve.skill = starter.skill - 2;
        starter.fatigue = 60;
        assertFalse(MatchEngine.rate(me).eleven().contains(starter));
        starter.fatigue = 0;
        assertTrue(MatchEngine.rate(me).eleven().contains(starter));
    }

    // ---- Live verslag -----------------------------------------------------------------

    @Test
    public void ownMatchHasTimelineMatchingTheScore() {
        Game g = newGame();
        for (int w = 0; w < 6; w++) {
            WeekReport r = g.advanceWeek();
            Fixture f = r.leagueMatch;
            int home = 0, away = 0, last = 0;
            for (MatchEvent e : f.events()) {
                assertTrue("tijdlijn op volgorde", e.minute >= last);
                last = e.minute;
                if (e.type == MatchEvent.Type.GOAL) {
                    if (e.home) home++;
                    else away++;
                }
                assertTrue(Commentary.line(f, e, home, away).length() > 5);
            }
            assertEquals(f.homeGoals, home);
            assertEquals(f.awayGoals, away);
        }
    }

    @Test
    public void chancesOnlyGeneratedForOwnMatches() {
        Game g = newGame();
        g.advanceWeek();
        boolean ownHasChances = false;
        for (Division d : g.divisions()) {
            for (Fixture f : d.round(1)) {
                boolean chances = false;
                for (MatchEvent e : f.events()) if (e.type == MatchEvent.Type.CHANCE) chances = true;
                if (f.involves(g.human())) ownHasChances = chances;
                else assertFalse(chances);
            }
        }
        assertTrue(ownHasChances);
    }

    // ---- Uitlenen ---------------------------------------------------------------------

    /** Een jong talent dat bij de eigen club op de bank zit. */
    private static Player benchTalent(Game g) {
        Club me = g.human();
        java.util.List<Player> xi = MatchEngine.rate(me).eleven();
        for (Player p : me.squad) {
            if (!xi.contains(p) && p.position != Position.DOELMAN) {
                p.age = 19;
                return p;
            }
        }
        throw new AssertionError("geen bankspeler gevonden");
    }

    @Test
    public void loanedPlayerPlaysElsewhereAndReturnsAtSeasonEnd() {
        Game g = newGame();
        Club me = g.human();
        Player talent = benchTalent(g);
        LoanOffer offer = g.loanOffer(talent);
        assertNotNull(offer);
        assertTrue(g.loanPlayer(talent).ok);
        assertFalse(me.squad.contains(talent));
        assertTrue(offer.club.squad.contains(talent));
        assertTrue(g.isOnLoan(talent));

        playSeason(g);
        Loan loan = g.loans().get(0);
        assertTrue("speelde " + loan.appearancesOnLoan(), loan.appearancesOnLoan() > 0 || !offer.wouldStart);
        g.advanceWeek(); // nieuw seizoen
        assertTrue(me.squad.contains(talent));
        assertFalse(offer.club.squad.contains(talent));
        assertTrue(g.loans().isEmpty());
    }

    @Test
    public void ownWageShareIsCharged() {
        Game g = newGame();
        Player talent = benchTalent(g);
        talent.wage = 1000;
        assertTrue(g.loanPlayer(talent).ok);
        Loan loan = g.loans().get(0);
        g.advanceWeek();
        Long charged = g.human().ledger.week().get("Salaris huurlingen (eigen deel)");
        if (loan.wageSharePct < 100) assertEquals(-loan.ownWageShare(), (long) charged);
    }

    @Test
    public void loanLimitsAreEnforced() {
        Game g = newGame();
        Club me = g.human();
        int loaned = 0;
        for (Player p : new java.util.ArrayList<>(me.squad)) {
            if (g.loanPlayer(p).ok) loaned++;
        }
        assertTrue(loaned <= Game.MAX_LOANS);
        assertTrue(me.squad.size() >= Game.MIN_SQUAD);
    }

    // ---- AI-economie ------------------------------------------------------------------

    @Test
    public void aiClubsHaveBudgetsThatChangeWeekly() {
        Game g = newGame();
        Club ai = g.division(1).clubs.get(0);
        assertTrue(ai.money > 0);
        long before = ai.money;
        g.advanceWeek();
        assertTrue(ai.money != before);
    }

    @Test
    public void transferWindowMovesPlayersForMoney() {
        Game g = newGame();
        long totalBefore = 0;
        for (Club c : g.allClubs()) if (!c.human) totalBefore += c.money;
        // Een rijke Topdivisie-club met een zwakke basisspeler wil zeker kopen
        Club buyer = g.division(1).clubs.get(0);
        buyer.money = 500_000_000L;
        Player weak = MatchEngine.rate(buyer).eleven().get(5);
        weak.skill = 40;
        java.util.List<AiEconomy.Deal> deals = AiEconomy.transferWindow(g);
        boolean bought = false;
        for (AiEconomy.Deal d : deals) {
            if (d.to == buyer) {
                bought = true;
                assertTrue(buyer.squad.contains(d.player));
                assertFalse(d.from.squad.contains(d.player));
                assertTrue(d.from.level > buyer.level);
            }
        }
        assertTrue(bought);
    }

    @Test
    public void indebtedClubSellsPlayers() {
        Game g = newGame();
        Club broke = g.division(3).clubs.get(0);
        broke.money = -50_000_000L;
        int before = broke.squad.size();
        AiEconomy.transferWindow(g);
        assertTrue(broke.squad.size() < before);
        assertTrue(broke.money > -50_000_000L);
    }

    @Test
    public void saveAndLoadKeepsTheGame() throws Exception {
        Game g = newGame();
        for (int i = 0; i < 5; i++) g.advanceWeek();
        File f = File.createTempFile("clubbaas", ".sav");
        SaveManager.save(g, f);
        Game loaded = SaveManager.load(f);
        assertEquals(g.week(), loaded.week());
        assertEquals(g.human().name, loaded.human().name);
        assertEquals(g.human().money, loaded.human().money);
        f.delete();
    }
}
