package nl.clubbaas.engine;

import java.io.Serializable;

/** Een stafmedewerker met 1 tot 5 sterren. */
public class Staff implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String name;
    public final StaffRole role;
    public final int rating;
    public final int wage;

    public Staff(String name, StaffRole role, int rating) {
        this.name = name;
        this.role = role;
        this.rating = rating;
        this.wage = wageFor(rating);
    }

    public static int wageFor(int rating) {
        return (int) (80 * Math.pow(3, rating - 1));
    }

    /** Eenmalige tekenpremie bij aanstellen. */
    public long signingFee() {
        return wage * 4L;
    }

    /** Afkoopsom bij ontslag. */
    public long severance() {
        return wage * 8L;
    }

    public String stars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < rating ? '★' : '☆');
        return sb.toString();
    }
}
