package nl.clubbaas.engine;

import java.io.Serializable;

/** Een voetballer. Vaardigheid (skill) loopt van 1 tot 99. */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int id;
    public String name;
    public Position position;
    public int age;
    public int skill;
    public int potential;
    public int wage;            // per week
    public int contractYears;   // seizoenen ná het huidige seizoen
    public int injuryWeeks;
    public int goals;           // dit seizoen
    public int appearances;     // dit seizoen
    public int yellowCards;     // dit seizoen
    public int suspension;      // aantal wedstrijden geschorst

    // Transfermarkt
    public long askingPrice;    // alleen voor spelers op de markt (0 = transfervrij)

    // Moraal (alleen bijgehouden voor de eigen club). Opgeslagen als afwijking van 60,
    // zodat oude savegames (waar alles 0 is) netjes op 60 beginnen.
    public static final int BASE_MORALE = 60;
    int moraleDelta;
    float playShare;          // voortschrijdend gemiddelde: deel van de wedstrijden dat hij speelde
    float expectation;        // welk deel hij verwacht te spelen; zakt langzaam na verlies van basisplaats
    boolean moraleInit;
    boolean transferRequested;

    // Verkoopbod dat deze week is gedaan (zodat je niet eindeloos opnieuw kunt vragen)
    long saleOffer;
    int saleOfferWeekKey = -1;

    public Player(int id, String name, Position position, int age, int skill, int potential) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.age = age;
        this.skill = skill;
        this.potential = Math.max(skill, potential);
        this.wage = wageFor(skill, age);
    }

    /** Basiswaarde op basis van vaardigheid: exponentieel, want topspelers zijn schaars. */
    static double baseValue(int skill) {
        return 5000.0 * Math.exp(0.13 * (skill - 30));
    }

    public static int wageFor(int skill, int age) {
        double w = baseValue(skill) / 150.0 + 100;
        if (age <= 18) w *= 0.5;
        return (int) (Math.round(w / 10.0) * 10);
    }

    /** Marktwaarde: vaardigheid, leeftijd en (bij jonge spelers) potentie. */
    public long value() {
        double v = baseValue(skill);
        if (age <= 21) v *= 1.3 + (potential - skill) / 60.0;
        else if (age <= 24) v *= 1.2;
        else if (age <= 29) v *= 1.0;
        else if (age <= 32) v *= 0.65;
        else v *= 0.35;
        long rounded = v >= 100_000 ? Math.round(v / 5000.0) * 5000 : Math.round(v / 500.0) * 500;
        return Math.max(500, rounded);
    }

    /** Moraal van 0 (woedend) tot 100 (dolgelukkig). */
    public int morale() {
        return Math.max(0, Math.min(100, BASE_MORALE + moraleDelta));
    }

    public boolean wantsTransfer() {
        return transferRequested;
    }

    public static String moraleLabel(int morale) {
        if (morale >= 75) return "Dolblij";
        if (morale >= 55) return "Tevreden";
        if (morale >= 40) return "Neutraal";
        if (morale >= 25) return "Ontevreden";
        return "Boos";
    }

    public static String moraleEmoji(int morale) {
        if (morale >= 75) return "😀";
        if (morale >= 55) return "🙂";
        if (morale >= 40) return "😐";
        if (morale >= 25) return "🙁";
        return "😠";
    }

    public boolean isInjured() {
        return injuryWeeks > 0;
    }

    public boolean isSuspended() {
        return suspension > 0;
    }

    /** Kan hij opgesteld worden? */
    public boolean isAvailable() {
        return !isInjured() && !isSuspended();
    }

    public boolean isFreeAgent() {
        return askingPrice == 0;
    }
}
