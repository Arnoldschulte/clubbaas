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
        Player target = g.market().get(0);
        long cost = g.signingCost(target);
        long before = g.human().money;
        assertTrue(g.buyPlayer(target).ok);
        assertEquals(before - cost, g.human().money);
        assertTrue(g.human().squad.contains(target));
        assertFalse(g.market().contains(target));
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
