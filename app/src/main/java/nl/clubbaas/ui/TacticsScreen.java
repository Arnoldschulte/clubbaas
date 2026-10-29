package nl.clubbaas.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Fixture;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Formation;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.MatchEngine;
import nl.clubbaas.engine.Position;
import nl.clubbaas.engine.Tactic;
import nl.clubbaas.engine.TeamRating;

/** Formatie en speelwijze kiezen, met een vergelijking tegen de volgende tegenstander. */
public class TacticsScreen extends Screen {

    public TacticsScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Tactiek";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();
        Club me = g.human();
        TeamRating mine = MatchEngine.rate(me);

        // Formatie
        LinearLayout formCard = Ui.card(c, "Formatie");
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = Ui.row(c);
        for (Formation f : Formation.values()) {
            chips.addView(Ui.chip(c, f.label, f == me.formation(), v -> {
                g.setFormation(f);
                refresh();
            }));
        }
        hs.addView(chips);
        formCard.addView(hs);

        TextView hint = Ui.muted(c, "Aanval en verdediging per formatie met jouw huidige selectie:");
        hint.setPadding(0, Ui.dp(c, 12), 0, Ui.dp(c, 4));
        formCard.addView(hint);
        LinearLayout head = Ui.row(c);
        head.addView(Ui.cell(c, "Formatie", 1.2f, true, Gravity.START));
        head.addView(Ui.cell(c, "Aanval", 1f, true, Gravity.END));
        head.addView(Ui.cell(c, "Verdediging", 1.3f, true, Gravity.END));
        formCard.addView(head);
        for (Formation f : Formation.values()) {
            TeamRating r = MatchEngine.rate(me, f);
            LinearLayout row = Ui.row(c);
            int pv = Ui.dp(c, 4);
            row.setPadding(Ui.dp(c, 4), pv, Ui.dp(c, 4), pv);
            if (f == me.formation()) row.setBackground(Ui.rounded(Ui.HIGHLIGHT, Ui.dp(c, 6)));
            row.addView(Ui.cell(c, f.label, 1.2f, f == me.formation(), Gravity.START));
            row.addView(Ui.cell(c, num(r.attack), 1f, false, Gravity.END));
            row.addView(Ui.cell(c, num(r.defense), 1.3f, false, Gravity.END));
            row.setOnClickListener(v -> {
                g.setFormation(f);
                refresh();
            });
            formCard.addView(row);
        }
        root.addView(formCard);

        // Speelwijze
        LinearLayout styleCard = Ui.card(c, "Speelwijze");
        LinearLayout styles = Ui.row(c);
        for (Tactic t : Tactic.values()) {
            styles.addView(Ui.chip(c, t.label, t == me.tactic(), v -> {
                g.setTactic(t);
                refresh();
            }));
        }
        styleCard.addView(styles);
        TextView desc = Ui.body(c, me.tactic().description);
        desc.setPadding(0, Ui.dp(c, 10), 0, 0);
        styleCard.addView(desc);
        root.addView(styleCard);

        // Volgende tegenstander
        Fixture next = g.nextLeagueFixture();
        if (next != null) {
            Club opp = next.opponentOf(me);
            TeamRating theirs = MatchEngine.rate(opp);
            LinearLayout oppCard = Ui.card(c, "Volgende tegenstander");
            oppCard.addView(Ui.text(c, (next.home == me ? "Thuis tegen " : "Uit bij ") + opp.name, 16, Ui.TEXT, true));
            oppCard.addView(Ui.muted(c, "Speelt meestal " + theirs.formation.label + ", verwacht: "
                    + g.expectedOpponentTactic().label.toLowerCase(Fmt.NL)));
            LinearLayout cmpHead = Ui.row(c);
            cmpHead.setPadding(0, Ui.dp(c, 10), 0, 0);
            cmpHead.addView(Ui.cell(c, "", 1.4f, true, Gravity.START));
            cmpHead.addView(Ui.cell(c, "Wij", 1f, true, Gravity.END));
            cmpHead.addView(Ui.cell(c, "Zij", 1f, true, Gravity.END));
            oppCard.addView(cmpHead);
            oppCard.addView(compareRow(c, "Aanval", mine.attack, theirs.attack));
            oppCard.addView(compareRow(c, "Verdediging", mine.defense, theirs.defense));
            double[] xg = g.expectedGoalsNextMatch();
            if (xg != null) {
                TextView exp = Ui.body(c, String.format(Locale.ROOT, "Verwachte score: %.1f – %.1f", xg[0], xg[1])
                        .replace('.', ','));
                exp.setPadding(0, Ui.dp(c, 10), 0, 0);
                oppCard.addView(exp);
            }
            oppCard.addView(Ui.muted(c, adviceFor(mine, theirs)));
            root.addView(oppCard);
        }

        // Basiself
        LinearLayout xi = Ui.card(c, "Basiself (" + mine.formation.label + ")");
        xi.addView(Ui.muted(c, "De beste fitte spelers worden automatisch opgesteld."));
        Position last = null;
        for (TeamRating.Slot s : mine.slots) {
            if (s.role != last) {
                TextView label = Ui.text(c, s.role.label + "s", 13, Ui.PRIMARY, true);
                label.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 2));
                xi.addView(label);
                last = s.role;
            }
            String name = s.player == null ? "— noodspeler (te weinig fitte spelers)" : s.player.name;
            String right = s.player == null ? "10" : String.valueOf(s.player.skill);
            LinearLayout kv = Ui.keyValue(c, name + (s.outOfPosition() ? "  (uit positie, 70%)" : ""), right);
            if (s.outOfPosition() || s.player == null) ((TextView) kv.getChildAt(0)).setTextColor(Ui.BAD);
            xi.addView(kv);
        }
        root.addView(xi);
    }

    private LinearLayout compareRow(Context c, String label, double ours, double theirs) {
        LinearLayout row = Ui.row(c);
        row.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 3));
        row.addView(Ui.cell(c, label, 1.4f, false, Gravity.START));
        TextView a = Ui.cell(c, num(ours), 1f, true, Gravity.END);
        a.setTextColor(ours >= theirs ? Ui.GOOD : Ui.BAD);
        row.addView(a);
        row.addView(Ui.cell(c, num(theirs), 1f, false, Gravity.END));
        return row;
    }

    private static String adviceFor(TeamRating mine, TeamRating theirs) {
        double diff = mine.overall() - theirs.overall();
        if (diff < -5) return "Zij zijn duidelijk sterker. Verdedigend spelen vergroot je kans op een punt.";
        if (diff > 5) return "Jij bent de favoriet. Aanvallend spelen helpt om de drie punten echt binnen te halen.";
        return "De teams zijn aan elkaar gewaagd.";
    }

    private static String num(double v) {
        return String.valueOf(Math.round(v));
    }
}
