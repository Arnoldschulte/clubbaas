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
            bench.injuryWeeks = 0;
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
