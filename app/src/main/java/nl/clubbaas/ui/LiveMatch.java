package nl.clubbaas.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Commentary;
import nl.clubbaas.engine.Fixture;
import nl.clubbaas.engine.MatchEvent;

/**
 * Speelt een al gesimuleerde wedstrijd 'live' af: een klok die in ongeveer tien seconden
 * van 0 tot 90 loopt en de gebeurtenissen op hun minuut laat zien.
 */
final class LiveMatch {
    private static final int MS_PER_MINUTE = 110;

    private final Context c;
    private final Club me;
    private final Fixture f;
    private final Runnable onDone;
    private final List<MatchEvent> events;

    private TextView scoreView;
    private TextView minuteView;
    private LinearLayout log;
    private ScrollView logScroll;
    private Button button;
    private AlertDialog dialog;

    private int minute;
    private int nextEvent;
    private int homeScore;
    private int awayScore;
    private boolean finished;
    private final Runnable tick = this::tick;

    private LiveMatch(Context c, Club me, Fixture f, Runnable onDone) {
        this.c = c;
        this.me = me;
        this.f = f;
        this.onDone = onDone;
        this.events = f.events();
    }

    static void show(Context c, Club me, Fixture f, String title, Runnable onDone) {
        new LiveMatch(c, me, f, onDone).open(title);
    }

    private void open(String title) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(c, 20);
        root.setPadding(p, Ui.dp(c, 8), p, 0);

        LinearLayout header = Ui.row(c);
        TextView home = Ui.text(c, f.home.name, 14, f.home == me ? Ui.PRIMARY : Ui.TEXT, true);
        home.setGravity(Gravity.START);
        header.addView(home, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        scoreView = Ui.text(c, "0 - 0", 28, Ui.TEXT, true);
        scoreView.setGravity(Gravity.CENTER);
        int sp = Ui.dp(c, 10);
        scoreView.setPadding(sp, 0, sp, 0);
        header.addView(scoreView);
        TextView away = Ui.text(c, f.away.name, 14, f.away == me ? Ui.PRIMARY : Ui.TEXT, true);
        away.setGravity(Gravity.END);
        header.addView(away, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        minuteView = Ui.text(c, "0'", 14, Ui.MUTED, true);
        minuteView.setGravity(Gravity.CENTER);
        minuteView.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 8));
        root.addView(minuteView);

        logScroll = new ScrollView(c);
        log = new LinearLayout(c);
        log.setOrientation(LinearLayout.VERTICAL);
        logScroll.addView(log);
        root.addView(logScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 260)));

        button = Ui.secondaryButton(c, "Overslaan", v -> {
            if (finished) close();
            else finishNow();
        });
        root.addView(button);
        TextView spacer = new TextView(c);
        spacer.setMinHeight(Ui.dp(c, 12));
        root.addView(spacer);

        addLine(Commentary.kickOff(f), false, Ui.MUTED);
        dialog = new AlertDialog.Builder(c)
                .setTitle(title)
                .setView(root)
                .setCancelable(false)
                .show();
        log.postDelayed(tick, MS_PER_MINUTE);
    }

    private void tick() {
        if (finished) return;
        minute++;
        revealMinute(minute);
        minuteView.setText(minute + "'");
        if (minute >= 90) {
            finish();
        } else {
            log.postDelayed(tick, MS_PER_MINUTE);
        }
    }

    /** Toont alle gebeurtenissen tot en met minuut m, en de rust na minuut 45. */
    private void revealMinute(int m) {
        while (nextEvent < events.size() && events.get(nextEvent).minute <= m) {
            showEvent(events.get(nextEvent));
            nextEvent++;
        }
        if (m == 45) addLine(Commentary.halfTime(homeScore, awayScore), false, Ui.MUTED);
    }

    private void showEvent(MatchEvent e) {
        if (e.type == MatchEvent.Type.GOAL) {
            if (e.home) homeScore++;
            else awayScore++;
            scoreView.setText(homeScore + " - " + awayScore);
        }
        boolean ours = (e.home && f.home == me) || (!e.home && f.away == me);
        int color;
        if (e.type == MatchEvent.Type.GOAL) color = ours ? Ui.GOOD : Ui.BAD;
        else if (e.type == MatchEvent.Type.CHANCE) color = Ui.TEXT;
        else color = ours ? Ui.BAD : Ui.TEXT;
        addLine(e.minute + "'  " + e.icon() + "  " + Commentary.line(f, e, homeScore, awayScore),
                e.type == MatchEvent.Type.GOAL, color);
    }

    private void addLine(String text, boolean bold, int color) {
        TextView t = Ui.text(c, text, 14, color, bold);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 3));
        log.addView(t);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    /** Overslaan: alles in één keer tonen. */
    private void finishNow() {
        log.removeCallbacks(tick);
        for (int m = minute + 1; m <= 90; m++) revealMinute(m);
        minute = 90;
        minuteView.setText("90'");
        finish();
    }

    private void finish() {
        if (finished) return;
        finished = true;
        // Zekerheid: de eindstand komt altijd overeen met de echte uitslag
        scoreView.setText(f.homeGoals + " - " + f.awayGoals);
        addLine(Commentary.fullTime(f), true, Ui.TEXT);
        button.setText("Verder");
        button.setBackground(Ui.rounded(Ui.PRIMARY, Ui.dp(c, 10)));
        button.setTextColor(0xFFFFFFFF);
    }

    private void close() {
        if (dialog != null) dialog.dismiss();
        onDone.run();
    }
}
