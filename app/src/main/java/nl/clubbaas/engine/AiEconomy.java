package nl.clubbaas.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Financiën en transfers van de AI-clubs.
 *
 * Inkomsten per divisie zijn zo gekozen dat een club met een selectie op het niveau van zijn
 * divisie net iets overhoudt. Gepromoveerde clubs houden dus geld over om te versterken,
 * gedegradeerde clubs met dure spelers lijden verlies en moeten verkopen.
 */
final class AiEconomy {
    private AiEconomy() {}

    private static final int TYPICAL_SQUAD = 18;
    private static final int MAX_PURCHASES = 3;
    private static final int MIN_SELLER_SQUAD = 16;

    /** Boven dit aantal weken inkomsten investeert een club het geld (in faciliteiten, jeugd enz.). */
    private static final int MAX_RESERVE_WEEKS = 40;
    /** Vanaf dit aantal weken inkomsten in kas koopt een club ook als hij al op niveau is. */
    private static final int RICH_WEEKS = 20;

    static long weeklyIncome(int level) {
        return Math.round(1.15 * TYPICAL_SQUAD * Player.wageFor(Game.targetSkill(level), 26));
    }

    static void initClub(Club c) {
        if (c.human || c.aiFinanceInit) return;
        c.money = weeklyIncome(c.level) * 30;
        c.aiFinanceInit = true;
    }

    /** Wekelijks: inkomsten erbij, salarissen eraf. */
    static void weekly(Game g) {
        for (Club c : g.allClubs()) {
            if (c.human) continue;
            initClub(c);
            long income = weeklyIncome(c.level);
            c.money += income - c.weeklyPlayerWages();
            c.money = Math.min(c.money, income * MAX_RESERVE_WEEKS);
        }
    }

    /** Een afgeronde transfer, voor het nieuws. */
    static final class Deal {
        final Player player;
        final Club from;      // null = vrije speler
        final Club to;        // null = buitenlandse club
        final long price;

        Deal(Player player, Club from, Club to, long price) {
            this.player = player;
            this.from = from;
            this.to = to;
            this.price = price;
        }

        String describe() {
            String buyer = to == null ? "een buitenlandse club" : to.name;
            return player.name + " (" + player.skill + ") van " + from.name + " naar " + buyer
                    + " voor " + Fmt.money(price);
        }
    }

    /** Transferperiode na het seizoen: eerst gedwongen verkopen, dan aankopen. */
    static List<Deal> transferWindow(Game g) {
        List<Deal> deals = new ArrayList<>();
        List<Club> ai = new ArrayList<>();
        for (Club c : g.allClubs()) {
            if (c.human) continue;
            initClub(c);
            ai.add(c);
        }

        // 1. Clubs in de schulden verkopen hun beste spelers
        for (Club c : ai) {
            int sold = 0;
            while (c.money < 0 && sold < 3 && c.squad.size() > Game.MIN_SQUAD) {
                Player best = null;
                for (Player p : c.squad) if (!g.isOnLoan(p) && (best == null || p.skill > best.skill)) best = p;
                if (best == null) break;
                long price = best.value();
                Club buyer = null;
                for (int lvl = Math.max(1, c.level - 2); lvl < c.level; lvl++) {
                    for (Club b : g.division(lvl).clubs) {
                        if (b.human || b.squad.size() >= 22 || b.money < price * 1.2) continue;
                        if (buyer == null || b.money > buyer.money) buyer = b;
                    }
                }
                if (buyer == null) price = Math.round(price * 0.7); // verkoop naar het buitenland
                transfer(g, best, c, buyer, price);
                deals.add(new Deal(best, c, buyer, price));
                sold++;
            }
        }

        // 2. Aankopen: hoogste divisie eerst, rijkste clubs eerst
        Collections.sort(ai, (a, b) -> a.level != b.level ? a.level - b.level : Long.compare(b.money, a.money));
        for (Club c : ai) {
            int target = Game.targetSkill(c.level);
            for (int i = 0; i < MAX_PURCHASES; i++) {
                long budget = c.money * 6 / 10;
                if (budget <= 0) break;
                boolean rich = c.money > weeklyIncome(c.level) * RICH_WEEKS;
                List<Player> eleven = MatchEngine.rate(c, c.formation(), false, false).eleven();
                Player weakest = null;
                for (Player p : eleven) if (weakest == null || p.skill < weakest.skill) weakest = p;
                if (weakest == null || weakest.skill >= target + (rich ? 5 : 2)) break; // sterk genoeg

                Player pick = null;
                Club pickFrom = null;
                long pickPrice = 0;
                for (int lvl = c.level + 1; lvl <= Math.min(Game.DIVISIONS, c.level + 2); lvl++) {
                    for (Club seller : g.division(lvl).clubs) {
                        if (seller.human || seller.squad.size() <= MIN_SELLER_SQUAD) continue;
                        for (Player p : seller.squad) {
                            if (g.isOnLoan(p) || p.position != weakest.position || p.age > 31) continue;
                            if (p.skill < weakest.skill + 3) continue;
                            long price = Math.round(p.value() * 1.25 / 1000.0) * 1000;
                            if (price > budget) continue;
                            if (pick == null || p.skill > pick.skill) {
                                pick = p;
                                pickFrom = seller;
                                pickPrice = price;
                            }
                        }
                    }
                }
                if (pick == null) break;
                transfer(g, pick, pickFrom, c, pickPrice);
                deals.add(new Deal(pick, pickFrom, c, pickPrice));
                if (c.squad.size() > 22) releaseWorst(g, c);
            }
        }
        return deals;
    }

    private static void transfer(Game g, Player p, Club from, Club to, long price) {
        from.squad.remove(p);
        from.money += price;
        if (to != null) {
            to.squad.add(p);
            to.money -= price;
            p.wage = Player.wageFor(p.skill, p.age);
            p.contractYears = 2 + g.rng().nextInt(3);
        }
    }

    private static void releaseWorst(Game g, Club c) {
        Player worst = null;
        for (Player p : c.squad) if (!g.isOnLoan(p) && (worst == null || p.skill < worst.skill)) worst = p;
        if (worst != null) c.squad.remove(worst);
    }
}
