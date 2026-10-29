package nl.clubbaas.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** De opstelling van een team in een formatie, met de berekende aanvals- en verdedigingskracht. */
public final class TeamRating {

    /** Eén plek in de opstelling. player is null als er niemand fit genoeg is (noodspeler). */
    public static final class Slot {
        public final Position role;
        public final Player player;

        Slot(Position role, Player player) {
            this.role = role;
            this.player = player;
        }

        public boolean outOfPosition() {
            return player != null && player.position != role;
        }
    }

    public final Formation formation;
    public final List<Slot> slots;
    public final double attack;
    public final double defense;

    TeamRating(Formation formation, List<Slot> slots, double attack, double defense) {
        this.formation = formation;
        this.slots = Collections.unmodifiableList(slots);
        this.attack = attack;
        this.defense = defense;
    }

    public double overall() {
        return (attack + defense) / 2.0;
    }

    public List<Player> eleven() {
        List<Player> out = new ArrayList<>();
        for (Slot s : slots) if (s.player != null) out.add(s.player);
        return out;
    }
}
