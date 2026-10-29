package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;

import java.util.List;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Cup;
import nl.clubbaas.engine.Fixture;
import nl.clubbaas.engine.Game;

/** De nationale beker: jouw weg door het toernooi en de laatste rondes. */
public class CupScreen extends Screen {

    public CupScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return Cup.NAME;
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();
        Cup cup = g.cup();
        Club me = g.human();

        LinearLayout status = Ui.card(c, "Status");
        status.addView(Ui.keyValue(c, "Jouw club", g.humanCupResult()));
        status.addView(Ui.keyValue(c, "Clubs nog in de race", String.valueOf(cup.remainingCount())));
        if (cup.winner != null) status.addView(Ui.keyValue(c, "Winnaar", cup.winner.name));
        StringBuilder weeks = new StringBuilder();
        for (int w : Game.CUP_WEEKS) {
            if (weeks.length() > 0) weeks.append(", ");
            weeks.append(w);
        }
        status.addView(Ui.muted(c, "Bekerrondes in week " + weeks + ". Elke gewonnen ronde levert een "
                + "premie op, die verdubbelt per ronde."));
        root.addView(status);

        if (cup.rounds.isEmpty()) {
            LinearLayout empty = Ui.card(c);
            empty.addView(Ui.muted(c, "De eerste ronde is nog niet gespeeld."));
            root.addView(empty);
            return;
        }

        for (int i = cup.rounds.size() - 1; i >= 0; i--) {
            List<Fixture> round = cup.rounds.get(i);
            LinearLayout card = Ui.card(c, cup.roundNames.get(i));
            boolean showAll = round.size() <= 8;
            boolean any = false;
            for (Fixture f : round) {
                boolean mine = f.involves(me);
                if (!showAll && !mine) continue;
                any = true;
                android.widget.TextView t = Ui.text(c, f.describe(), 14, Ui.TEXT, mine);
                t.setPadding(Ui.dp(c, 6), Ui.dp(c, 4), Ui.dp(c, 6), Ui.dp(c, 4));
                if (mine) t.setBackground(Ui.rounded(Ui.HIGHLIGHT, Ui.dp(c, 6)));
                card.addView(t);
            }
            if (!any) card.addView(Ui.muted(c, round.size() + " wedstrijden, jouw club speelde niet."));
            root.addView(card);
        }
    }
}
