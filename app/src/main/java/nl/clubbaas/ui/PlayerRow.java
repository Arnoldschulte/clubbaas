package nl.clubbaas.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import nl.clubbaas.engine.Player;

/** Herbruikbare regel voor een speler: positie, naam, details en vaardigheid. */
final class PlayerRow {
    private PlayerRow() {}

    static LinearLayout create(Context c, Player p, String detail, View.OnClickListener click) {
        LinearLayout row = Ui.row(c);
        int pv = Ui.dp(c, 8);
        row.setPadding(0, pv, 0, pv);
        row.setOnClickListener(click);

        TextView pos = Ui.text(c, p.position.code, 13, 0xFFFFFFFF, true);
        pos.setGravity(Gravity.CENTER);
        pos.setBackground(Ui.rounded(positionColor(p), Ui.dp(c, 6)));
        int s = Ui.dp(c, 28);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.setMargins(0, 0, Ui.dp(c, 10), 0);
        row.addView(pos, lp);

        LinearLayout mid = new LinearLayout(c);
        mid.setOrientation(LinearLayout.VERTICAL);
        String name = p.name + (p.isInjured() ? "  🩹 " + p.injuryWeeks + "w" : "")
                + (p.isSuspended() ? "  🟥 geschorst" + (p.suspension > 1 ? " (" + p.suspension + ")" : "") : "");
        mid.addView(Ui.text(c, name, 15, Ui.TEXT, true));
        mid.addView(Ui.muted(c, detail));
        row.addView(mid, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView skill = Ui.text(c, String.valueOf(p.skill), 18, skillColor(p.skill), true);
        skill.setGravity(Gravity.END);
        row.addView(skill);
        return row;
    }

    static int positionColor(Player p) {
        switch (p.position) {
            case DOELMAN: return 0xFFF2A900;
            case VERDEDIGER: return 0xFF1976D2;
            case MIDDENVELDER: return 0xFF2E7D32;
            default: return 0xFFC62828;
        }
    }

    static int skillColor(int skill) {
        if (skill >= 80) return 0xFF6A1B9A;
        if (skill >= 65) return 0xFF1565C0;
        if (skill >= 50) return 0xFF2E7D32;
        if (skill >= 35) return 0xFF8D6E00;
        return 0xFF6D4C41;
    }
}
