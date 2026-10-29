package nl.clubbaas.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.List;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Division;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.MatchEngine;
import nl.clubbaas.engine.Player;
import nl.clubbaas.engine.TableRow;

/** Stand van elke divisie plus topscorers. */
public class TableScreen extends Screen {
    private int level;

    public TableScreen(MainActivity act) {
        super(act);
        level = act.game().human().level;
    }

    @Override
    public String title() {
        return "Stand";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();

        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = Ui.row(c);
        chips.setPadding(0, 0, 0, Ui.dp(c, 12));
        for (Division d : g.divisions()) {
            final int lvl = d.level;
            chips.addView(Ui.chip(c, d.name, lvl == level, v -> { level = lvl; refresh(); }));
        }
        hs.addView(chips);
        root.addView(hs);

        Division d = g.division(level);
        LinearLayout card = Ui.card(c);
        LinearLayout head = Ui.row(c);
        head.setPadding(0, 0, 0, Ui.dp(c, 6));
        addCells(c, head, new String[]{"#", "Club", "G", "W", "G", "V", "DS", "Pt"}, true, Ui.MUTED);
        card.addView(head);

        List<TableRow> rows = d.sortedTable();
        for (int i = 0; i < rows.size(); i++) {
            TableRow r = rows.get(i);
            LinearLayout line = Ui.row(c);
            int pv = Ui.dp(c, 7);
            line.setPadding(Ui.dp(c, 4), pv, Ui.dp(c, 4), pv);
            boolean promo = d.level > 1 && i < Game.PROMOTION_SPOTS;
            boolean releg = d.level < Game.DIVISIONS && i >= rows.size() - Game.PROMOTION_SPOTS;
            if (r.club.human) line.setBackground(Ui.rounded(Ui.HIGHLIGHT, Ui.dp(c, 6)));
            else if (promo) line.setBackground(Ui.rounded(Ui.PROMO_BG, Ui.dp(c, 6)));
            else if (releg) line.setBackground(Ui.rounded(Ui.RELEG_BG, Ui.dp(c, 6)));
            String gd = (r.goalDifference() > 0 ? "+" : "") + r.goalDifference();
            addCells(c, line, new String[]{String.valueOf(i + 1), r.club.name, String.valueOf(r.played),
                    String.valueOf(r.won), String.valueOf(r.drawn), String.valueOf(r.lost), gd,
                    String.valueOf(r.points())}, r.club.human, Ui.TEXT);
            final Club club = r.club;
            line.setOnClickListener(v -> showClub(club, d));
            card.addView(line);
        }
        card.addView(legend(c, d));
        root.addView(card);

        LinearLayout scorers = Ui.card(c, "Topscorers");
        List<Player> top = d.topScorers(8);
        for (Player p : top) {
            Club club = d.clubOf(p);
            scorers.addView(Ui.keyValue(c, p.name + " (" + (club == null ? "?" : club.name) + ")",
                    String.valueOf(p.goals)));
        }
        if (top.isEmpty()) scorers.addView(Ui.muted(c, "Nog geen doelpunten gemaakt."));
        root.addView(scorers);
    }

    private static final float[] WEIGHTS = {0.6f, 3.6f, 0.7f, 0.7f, 0.7f, 0.7f, 0.9f, 0.8f};

    private void addCells(Context c, LinearLayout row, String[] values, boolean bold, int color) {
        for (int i = 0; i < values.length; i++) {
            android.widget.TextView t = Ui.cell(c, values[i], WEIGHTS[i], bold || i == values.length - 1,
                    i == 1 ? Gravity.START : Gravity.CENTER);
            t.setTextColor(color);
            if (i == 1) t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(t);
        }
    }

    private LinearLayout legend(Context c, Division d) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, Ui.dp(c, 10), 0, 0);
        if (d.level > 1) l.addView(Ui.muted(c, "Groen: promotie naar de " + Game.DIVISION_NAMES[d.level - 2]));
        if (d.level < Game.DIVISIONS) l.addView(Ui.muted(c, "Rood: degradatie naar de " + Game.DIVISION_NAMES[d.level]));
        return l;
    }

    private void showClub(Club club, Division d) {
        StringBuilder sb = new StringBuilder();
        sb.append("Positie: ").append(d.positionOf(club)).append("e\n");
        sb.append("Teamsterkte: ").append(Math.round(MatchEngine.strength(club))).append("\n");
        sb.append("Gemiddelde vaardigheid: ").append(Math.round(club.averageSkill())).append("\n");
        sb.append("Selectiewaarde: ").append(Fmt.money(club.totalSquadValue())).append("\n");
        if (!club.human) sb.append("Budget: ").append(Fmt.money(club.money)).append("\n");
        sb.append("\nBeste spelers:\n");
        List<Player> sorted = new java.util.ArrayList<>(club.squad);
        sorted.sort((a, b) -> b.skill - a.skill);
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Player p = sorted.get(i);
            sb.append("• ").append(p.name).append(" (").append(p.position.code).append(", ")
                    .append(p.skill).append(")\n");
        }
        Ui.info(ctx(), club.name, sb.toString().trim());
    }
}
