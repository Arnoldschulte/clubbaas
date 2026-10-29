package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Set;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Achievement;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.Highscores;

/** Behaalde prestaties en de highscorelijst over al je spellen. */
public class AchievementsScreen extends Screen {

    public AchievementsScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Prestaties & highscores";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();
        if (g != null) buildCurrentGame(root, c, g);

        LinearLayout scores = Ui.card(c, "Highscores");
        List<Highscores.Entry> entries = Highscores.load(act.highscoreFile());
        if (entries.isEmpty()) {
            scores.addView(Ui.muted(c, "Nog geen afgeronde spellen. Een spel komt op de lijst als het eindigt "
                    + "of als je een nieuw spel begint."));
        }
        for (int i = 0; i < entries.size(); i++) {
            Highscores.Entry e = entries.get(i);
            if (i > 0) scores.addView(Ui.divider(c));
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
            row.addView(Ui.keyValue(c, (i + 1) + ". " + e.club + " (" + e.chairman + ")", Fmt.number(e.score)));
            TextView detail = Ui.muted(c, e.seasons + " seizoen(en) · hoogste: " + e.bestDivision
                    + " · titels " + e.titles + " · bekers " + e.trophies + " · " + e.ending);
            row.addView(detail);
            scores.addView(row);
        }
        root.addView(scores);
        if (g == null) root.addView(Ui.secondaryButton(c, "Terug", v -> act.show(new NewGameScreen(act))));
    }

    private void buildCurrentGame(LinearLayout root, Context c, Game g) {
        Set<Achievement> unlocked = g.achievements();

        LinearLayout current = Ui.card(c, "Huidig spel");
        current.addView(Ui.keyValue(c, "Score", Fmt.number(g.score())));
        current.addView(Ui.keyValue(c, "Prestaties", unlocked.size() + " / " + Achievement.values().length));
        current.addView(Ui.muted(c, "Score: elk seizoen punten voor je divisie en eindpositie, plus bonussen voor "
                + "promoties, titels, bekers, Europa en prestaties."));
        root.addView(current);

        LinearLayout list = Ui.card(c, "Prestaties");
        for (Achievement a : Achievement.values()) {
            boolean done = unlocked.contains(a);
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
            row.addView(Ui.text(c, (done ? "🏅 " : "🔒 ") + a.title, 15, done ? Ui.TEXT : Ui.MUTED, done));
            row.addView(Ui.muted(c, a.description));
            list.addView(row);
        }
        root.addView(list);
    }
}
