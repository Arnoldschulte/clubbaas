package nl.clubbaas.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Fixture;

/** Speelschema en uitslagen van de eigen club in de competitie. */
public class FixturesScreen extends Screen {

    public FixturesScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Wedstrijden";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Club me = game().human();
        LinearLayout card = Ui.card(c, game().humanDivision().name + " " + game().seasonLabel());
        boolean first = true;
        for (Fixture f : game().humanDivision().fixturesOf(me)) {
            if (!first) card.addView(Ui.divider(c));
            first = false;
            LinearLayout row = Ui.row(c);
            int pv = Ui.dp(c, 8);
            row.setPadding(0, pv, 0, pv);
            row.addView(Ui.cell(c, "W" + f.week, 0.8f, false, Gravity.START));
            boolean home = f.home == me;
            row.addView(Ui.cell(c, (home ? "Thuis: " : "Uit: ") + f.opponentOf(me).name, 4f, home, Gravity.START));
            TextView score = Ui.cell(c, f.played ? f.scoreText() : "–", 1.4f, true, Gravity.END);
            row.addView(score);
            if (f.played) {
                TextView badge = Ui.resultBadge(c, f.resultFor(me));
                ((LinearLayout.LayoutParams) badge.getLayoutParams()).setMargins(Ui.dp(c, 8), 0, 0, 0);
                row.addView(badge);
            }
            if (f.week == game().week() + 1 && !game().isSeasonOver()) {
                row.setBackground(Ui.rounded(Ui.HIGHLIGHT, Ui.dp(c, 6)));
            }
            card.addView(row);
        }
        root.addView(card);
    }
}
