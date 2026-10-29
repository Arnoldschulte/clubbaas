package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Game;

/** Basisklasse voor alle schermen. Een scherm bouwt zijn inhoud in een verticale LinearLayout. */
public abstract class Screen {
    protected final MainActivity act;

    protected Screen(MainActivity act) {
        this.act = act;
    }

    public abstract String title();

    public abstract void build(LinearLayout root);

    protected Game game() {
        return act.game();
    }

    protected Context ctx() {
        return act;
    }

    /** Opnieuw opbouwen na een actie (en automatisch opslaan). */
    protected void refresh() {
        act.refresh();
    }
}
