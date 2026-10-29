package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Cup;
import nl.clubbaas.engine.Fixture;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.MatchEngine;

/** De Europese Beker: deelnemers, rondes en jouw resultaten. */
public class EuropeScreen extends Screen {

    public EuropeScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return Game.EUROPE_NAME;
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();
        Cup europe = g.europe();
        Club me = g.human();

        LinearLayout status = Ui.card(c, "Status");
        if (europe == null) {
            status.addView(Ui.body(c, "Het toernooi begint volgend seizoen."));
            root.addView(status);
            return;
        }
        status.addView(Ui.keyValue(c, "Jouw club", g.humanEuropeResult() == null ? "Niet geplaatst" : g.humanEuropeResult()));
        status.addView(Ui.keyValue(c, "Clubs nog in de race", String.valueOf(europe.remainingCount())));
        if (europe.winner != null) status.addView(Ui.keyValue(c, "Winnaar", europe.winner.name));
        StringBuilder weeks = new StringBuilder();
        for (int w : Game.EUROPE_WEEKS) {
            if (weeks.length() > 0) weeks.append(", ");
            weeks.append(w);
        }
        status.addView(Ui.muted(c, "De top " + Game.EUROPE_SPOTS + " van de Topdivisie plaatst zich voor het volgende "
                + "seizoen, samen met sterke buitenlandse clubs. Wedstrijden in week " + weeks
                + ". Deelname en elke gewonnen ronde leveren veel prijzengeld op."));
        root.addView(status);

        for (int i = europe.rounds.size() - 1; i >= 0; i--) {
            List<Fixture> round = europe.rounds.get(i);
            LinearLayout card = Ui.card(c, europe.roundNames.get(i));
            for (Fixture f : round) {
                boolean mine = f.involves(me);
                TextView t = Ui.text(c, f.describe(), 14, Ui.TEXT, mine);
                t.setPadding(Ui.dp(c, 6), Ui.dp(c, 4), Ui.dp(c, 6), Ui.dp(c, 4));
                if (mine) t.setBackground(Ui.rounded(Ui.HIGHLIGHT, Ui.dp(c, 6)));
                card.addView(t);
            }
            root.addView(card);
        }

        if (!g.foreignClubs().isEmpty()) {
            LinearLayout foreign = Ui.card(c, "Buitenlandse deelnemers");
            for (Club f : g.foreignClubs()) {
                foreign.addView(Ui.keyValue(c, f.name, "sterkte " + Math.round(MatchEngine.strength(f))));
            }
            root.addView(foreign);
        }
    }
}
