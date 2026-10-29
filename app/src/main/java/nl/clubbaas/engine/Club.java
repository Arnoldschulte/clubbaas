package nl.clubbaas.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

/** Een club. Alleen de eigen club (human) heeft een stadion, staf en financiën. */
public class Club implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int id;
    public String name;
    public int color;           // ARGB, voor de UI
    public int level;           // divisie 1 (top) t/m 7
    public final boolean human;
    public final List<Player> squad = new ArrayList<>();

    // Tactiek. Kan null zijn in savegames van vóór deze functie, gebruik daarom de getters.
    Formation formation;
    Tactic tactic;

    // Invloed van de voorzitter: één speler die hij graag ziet starten (0 = niemand).
    int favoriteId;
    // Ergernis van de manager over die bemoeienis, 0-100. Bij 100 stapt hij op.
    int managerFrustration;

    // Alleen voor de eigen club
    public Stadium stadium;
    public final EnumMap<StaffRole, Staff> staff = new EnumMap<>(StaffRole.class);
    public long money;
    public long loan;
    public int fans;
    public long sponsorPerSeason;
    // Lopend sponsorcontract en openstaande aanbiedingen (null in oude savegames)
    SponsorDeal sponsorDeal;
    List<SponsorDeal> sponsorOffers;
    public int lastAttendance;
    public int negativeWeeks;
    public final Ledger ledger = new Ledger();
    public final List<SeasonRecord> history = new ArrayList<>();

    public Club(int id, String name, int color, int level, boolean human) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.level = level;
        this.human = human;
    }

    public Formation formation() {
        return formation == null ? Formation.F442 : formation;
    }

    public Tactic tactic() {
        return tactic == null ? Tactic.NORMAAL : tactic;
    }

    /** De voorkeursspeler van de voorzitter, of null (ook als hij inmiddels vertrokken is). */
    public Player favorite() {
        if (favoriteId == 0) return null;
        for (Player p : squad) if (p.id == favoriteId) return p;
        return null;
    }

    public SponsorDeal sponsorDeal() {
        return sponsorDeal;
    }

    /** Openstaande sponsoraanbiedingen (leeg als er een contract loopt). */
    public List<SponsorDeal> sponsorOffers() {
        if (sponsorOffers == null) sponsorOffers = new ArrayList<>();
        return sponsorOffers;
    }

    public int managerFrustration() {
        return managerFrustration;
    }

    /** Sterren van een stafrol. AI-clubs hebben altijd gemiddelde staf (3). Geen staf = 0. */
    public int staffRating(StaffRole role) {
        if (!human) return 3;
        Staff s = staff.get(role);
        return s == null ? 0 : s.rating;
    }

    public long weeklyPlayerWages() {
        long t = 0;
        for (Player p : squad) t += p.wage;
        return t;
    }

    public long weeklyStaffWages() {
        long t = 0;
        for (Staff s : staff.values()) t += s.wage;
        return t;
    }

    public double averageSkill() {
        if (squad.isEmpty()) return 0;
        double t = 0;
        for (Player p : squad) t += p.skill;
        return t / squad.size();
    }

    public List<Player> sortedSquad() {
        List<Player> list = new ArrayList<>(squad);
        Collections.sort(list, Comparator
                .comparing((Player p) -> p.position)
                .thenComparing((Player p) -> -p.skill));
        return list;
    }

    public Player worstPlayer() {
        Player worst = null;
        for (Player p : squad) if (worst == null || p.skill < worst.skill) worst = p;
        return worst;
    }

    public Player topScorer() {
        Player best = null;
        for (Player p : squad) if (p.goals > 0 && (best == null || p.goals > best.goals)) best = p;
        return best;
    }

    public long totalSquadValue() {
        long t = 0;
        for (Player p : squad) t += p.value();
        return t;
    }
}
