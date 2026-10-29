package nl.clubbaas.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import nl.clubbaas.engine.ActionResult;

/** Kleine bouwstenen voor de UI, zodat de schermen kort en leesbaar blijven. */
public final class Ui {
    // Kleuren: grasgroen met een warm geel accent
    public static final int BG = 0xFFF1F4EF;
    public static final int CARD = 0xFFFFFFFF;
    public static final int TEXT = 0xFF1B2420;
    public static final int MUTED = 0xFF68736C;
    public static final int LINE = 0xFFE2E7DF;
    public static final int PRIMARY = 0xFF1E6B3A;
    public static final int PRIMARY_DARK = 0xFF123F23;
    public static final int ACCENT = 0xFFF2B705;
    public static final int GOOD = 0xFF2E7D32;
    public static final int BAD = 0xFFC62828;
    public static final int NEUTRAL = 0xFF8D8D8D;
    public static final int PROMO_BG = 0xFFE3F2E6;
    public static final int RELEG_BG = 0xFFFBE7E5;
    public static final int HIGHLIGHT = 0xFFFFF3C4;

    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static TextView body(Context c, String s) {
        return text(c, s, 15, TEXT, false);
    }

    public static TextView muted(Context c, String s) {
        return text(c, s, 13, MUTED, false);
    }

    public static TextView heading(Context c, String s) {
        TextView t = text(c, s, 13, PRIMARY, true);
        t.setAllCaps(true);
        t.setLetterSpacing(0.08f);
        t.setPadding(0, 0, 0, dp(c, 8));
        return t;
    }

    /** Witte kaart met afgeronde hoeken. */
    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rounded(CARD, dp(c, 14)));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, 12));
        l.setLayoutParams(lp);
        l.setElevation(dp(c, 1));
        return l;
    }

    public static LinearLayout card(Context c, String title) {
        LinearLayout l = card(c);
        l.addView(heading(c, title));
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /** Regel met links een label en rechts een waarde. */
    public static LinearLayout keyValue(Context c, String key, String value) {
        LinearLayout r = row(c);
        r.setPadding(0, dp(c, 4), 0, dp(c, 4));
        TextView k = text(c, key, 15, MUTED, false);
        r.addView(k, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView v = text(c, value, 15, TEXT, true);
        v.setGravity(Gravity.END);
        r.addView(v);
        return r;
    }

    public static TextView cell(Context c, String s, float weight, boolean bold, int gravity) {
        TextView t = text(c, s, 14, TEXT, bold);
        t.setGravity(gravity);
        t.setSingleLine(true);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        return t;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 1)));
        return v;
    }

    public static Button button(Context c, String label, View.OnClickListener l) {
        return styledButton(c, label, PRIMARY, 0xFFFFFFFF, l);
    }

    public static Button secondaryButton(Context c, String label, View.OnClickListener l) {
        return styledButton(c, label, 0xFFE4EDE5, PRIMARY_DARK, l);
    }

    public static Button dangerButton(Context c, String label, View.OnClickListener l) {
        return styledButton(c, label, 0xFFF9E1DF, BAD, l);
    }

    private static Button styledButton(Context c, String label, int bg, int fg, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(fg);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(rounded(bg, dp(c, 10)));
        b.setStateListAnimator(null);
        int ph = dp(c, 14);
        b.setPadding(ph, 0, ph, 0);
        b.setMinHeight(dp(c, 44));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 6), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    /** Kleine selecteerbare 'chip', bijv. voor divisie- of positiefilters. */
    public static TextView chip(Context c, String label, boolean selected, View.OnClickListener l) {
        TextView t = text(c, label, 14, selected ? 0xFFFFFFFF : PRIMARY_DARK, true);
        t.setBackground(rounded(selected ? PRIMARY : 0xFFE4EDE5, dp(c, 16)));
        int ph = dp(c, 12), pv = dp(c, 6);
        t.setPadding(ph, pv, ph, pv);
        t.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(c, 6), 0);
        t.setLayoutParams(lp);
        return t;
    }

    /** Gekleurd rondje met W, G of V. */
    public static TextView resultBadge(Context c, String result) {
        int color = result.equals("W") ? GOOD : result.equals("V") ? BAD : NEUTRAL;
        TextView t = text(c, result, 12, 0xFFFFFFFF, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(rounded(color, dp(c, 12)));
        int s = dp(c, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.setMargins(0, 0, dp(c, 4), 0);
        t.setLayoutParams(lp);
        return t;
    }

    public static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    public static void show(Context c, ActionResult r) {
        Toast.makeText(c, r.message, r.ok ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    }

    public static void confirm(Context c, String title, String message, String okLabel, Runnable onOk) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(okLabel, (d, w) -> onOk.run())
                .setNegativeButton("Annuleren", null)
                .show();
    }

    public static final int STYLE_PRIMARY = 0;
    public static final int STYLE_SECONDARY = 1;
    public static final int STYLE_DANGER = 2;

    /** Dialoog met tekst en een willekeurig aantal actieknoppen onder elkaar. */
    public static void actionDialog(Context c, String title, String message,
                                    String[] labels, int[] styles, Runnable[] actions) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        int ph = dp(c, 24);
        box.setPadding(ph, dp(c, 8), ph, 0);
        box.addView(body(c, message));
        AlertDialog[] holder = new AlertDialog[1];
        for (int i = 0; i < labels.length; i++) {
            final Runnable action = actions[i];
            View.OnClickListener l = v -> {
                if (holder[0] != null) holder[0].dismiss();
                action.run();
            };
            Button b = styles[i] == STYLE_PRIMARY ? button(c, labels[i], l)
                    : styles[i] == STYLE_DANGER ? dangerButton(c, labels[i], l)
                    : secondaryButton(c, labels[i], l);
            box.addView(b);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(c);
        scroll.addView(box);
        holder[0] = new AlertDialog.Builder(c)
                .setTitle(title)
                .setView(scroll)
                .setNegativeButton("Sluiten", null)
                .show();
    }

    public static void info(Context c, String title, String message) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
