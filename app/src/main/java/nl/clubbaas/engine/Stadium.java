package nl.clubbaas.engine;

import java.io.Serializable;

/** Stadion en faciliteiten van de eigen club. */
public class Stadium implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_LEVEL = 5;

    public int capacity = 500;
    public int ticketPrice = 8;
    public int commercialLevel = 0; // horeca & fanshop
    public int trainingLevel = 0;   // trainingscomplex
    public int youthLevel = 0;      // jeugdacademie

    // Bouw van een nieuwe tribune (0 = niets in aanbouw)
    public int constructionWeeksLeft;
    public int constructionSeats;
    /** Tijdens de bouw is een deel van het stadion afgesloten. */
    public static final double CONSTRUCTION_CAPACITY_FACTOR = 0.9;

    public boolean isUnderConstruction() {
        return constructionWeeksLeft > 0;
    }

    /** Capaciteit die nu echt beschikbaar is. */
    public int effectiveCapacity() {
        return isUnderConstruction() ? (int) (capacity * CONSTRUCTION_CAPACITY_FACTOR) : capacity;
    }

    /** Bouwtijd in weken: grotere tribunes duren langer. */
    public static int buildWeeks(int seats) {
        return 3 + seats / 1000;
    }

    /** Hoeveel stoelen de volgende uitbreiding oplevert. */
    public int nextExpansionSeats() {
        if (capacity < 5_000) return 500;
        if (capacity < 20_000) return 2_500;
        return 5_000;
    }

    public long expansionCost() {
        long perSeat = 60 + capacity / 250;
        return nextExpansionSeats() * perSeat;
    }

    public static long levelCost(long base, int currentLevel) {
        return base * (1L << currentLevel);
    }

    public long commercialCost() { return levelCost(25_000, commercialLevel); }
    public long trainingCost()   { return levelCost(40_000, trainingLevel); }
    public long youthCost()      { return levelCost(35_000, youthLevel); }

    /** Besteding per bezoeker aan eten, drinken en merchandise. */
    public double spendPerFan() {
        return 1.0 + commercialLevel * 1.2;
    }
}
