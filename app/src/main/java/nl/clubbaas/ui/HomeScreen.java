package nl.clubbaas.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Cup;
import nl.clubbaas.engine.Division;
import nl.clubbaas.engine.Fixture;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.MatchEngine;
import nl.clubbaas.engine.NewsItem;
import nl.clubbaas.engine.WeekReport;

/** Overzicht met de volgende wedstrijd, de knop om door te spelen en het laatste nieuws. */
public class HomeScreen extends Screen {

    public HomeScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Overzicht";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();
        Club me = g.human();

        if (g.isGameOver()) {
            LinearLayout over = Ui.card(c, "Einde verhaal");
            over.addView(Ui.body(c, g.gameOverReason()));
            over.addView(Ui.button(c, "Nieuw spel", v -> act.deleteSave()));
            root.addView(over);
            return;
        }

        // Volgende wedstrijd
        LinearLayout next = Ui.card(c, g.isSeasonOver() ? "Seizoen afgelopen" : "Week " + (g.week() + 1));
        if (g.isSeasonOver()) {
            next.addView(Ui.body(c, g.lastSummary() == null ? "" : g.lastSummary().outcome));
            next.addView(Ui.button(c, "Start nieuw seizoen", v -> playWeek()));
        } else {
            Fixture f = g.nextLeagueFixture();
            if (f != null) {
                Club opp = f.opponentOf(me);
                boolean home = f.home == me;
                next.addView(Ui.text(c, (home ? "Thuis tegen " : "Uit bij ") + opp.name, 18, Ui.TEXT, true));
                next.addView(Ui.muted(c, "Nummer " + g.humanDivision().positionOf(opp) + " in de "
                        + g.humanDivision().name));
                next.addView(strengthBar(c, me, opp));
                TextView tactic = Ui.muted(c, "Jouw tactiek: " + me.formation().label + ", "
                        + me.tactic().label.toLowerCase(Fmt.NL));
                tactic.setPadding(0, Ui.dp(c, 8), 0, 0);
                next.addView(tactic);
                next.addView(Ui.secondaryButton(c, "Tactiek aanpassen", v -> act.show(new TacticsScreen(act))));
            }
            if (g.nextWeekHasCupMatch()) {
                TextView cup = Ui.text(c, "🏆 Deze week ook een bekerronde (" + Cup.NAME + ")", 14, Ui.PRIMARY, true);
                cup.setPadding(0, Ui.dp(c, 8), 0, 0);
                next.addView(cup);
            }
            next.addView(Ui.button(c, "Speel week " + (g.week() + 1), v -> playWeek()));
            next.addView(Ui.secondaryButton(c, "Speel rest van het seizoen", v ->
                    Ui.confirm(c, "Doorspelen", "Alle resterende weken worden gespeeld zonder tussenstops. "
                            + "Je kunt in de tussentijd geen spelers kopen of verkopen.", "Doorspelen",
                            this::playRestOfSeason)));
        }
        root.addView(next);

        // Clubstatus
        LinearLayout status = Ui.card(c, "Jouw club");
        Division d = g.humanDivision();
        status.addView(Ui.keyValue(c, "Positie", d.positionOf(me) + "e in de " + d.name));
        status.addView(Ui.keyValue(c, "Kas", Fmt.money(me.money)));
        status.addView(Ui.keyValue(c, "Fans", Fmt.number(me.fans)));
        status.addView(Ui.keyValue(c, "Teamsterkte", String.valueOf(Math.round(MatchEngine.strength(me)))));
        status.addView(Ui.keyValue(c, Cup.NAME, g.humanCupResult()));
        LinearLayout form = Ui.row(c);
        form.setPadding(0, Ui.dp(c, 8), 0, 0);
        TextView formLabel = Ui.muted(c, "Vorm  ");
        form.addView(formLabel);
        List<Fixture> played = new ArrayList<>();
        for (Fixture f : d.fixturesOf(me)) if (f.played) played.add(f);
        for (int i = Math.max(0, played.size() - 5); i < played.size(); i++) {
            form.addView(Ui.resultBadge(c, played.get(i).resultFor(me)));
        }
        if (played.isEmpty()) form.addView(Ui.muted(c, "nog niet gespeeld"));
        status.addView(form);
        root.addView(status);

        // Nieuws
        LinearLayout news = Ui.card(c, "Laatste nieuws");
        List<NewsItem> items = g.news();
        for (int i = 0; i < Math.min(4, items.size()); i++) {
            TextView t = Ui.body(c, "• " + items.get(i).text);
            t.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 3));
            news.addView(t);
        }
        news.addView(Ui.secondaryButton(c, "Alle nieuws", v -> act.show(new NewsScreen(act))));
        root.addView(news);
    }

    /** Twee balken die de teamsterkte van beide clubs vergelijken. */
    private LinearLayout strengthBar(Context c, Club me, Club opp) {
        double a = MatchEngine.strength(me);
        double b = MatchEngine.strength(opp);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, Ui.dp(c, 10), 0, 0);
        LinearLayout labels = Ui.row(c);
        labels.addView(Ui.cell(c, "Wij " + Math.round(a), 1, true, android.view.Gravity.START));
        labels.addView(Ui.cell(c, Math.round(b) + " Zij", 1, true, android.view.Gravity.END));
        box.addView(labels);
        LinearLayout bar = Ui.row(c);
        android.view.View left = new android.view.View(c);
        left.setBackground(Ui.rounded(me.color, Ui.dp(c, 4)));
        android.view.View right = new android.view.View(c);
        right.setBackground(Ui.rounded(opp.color, Ui.dp(c, 4)));
        int h = Ui.dp(c, 8);
        bar.addView(left, new LinearLayout.LayoutParams(0, h, (float) a));
        android.view.View gap = new android.view.View(c);
        bar.addView(gap, new LinearLayout.LayoutParams(Ui.dp(c, 3), h));
        bar.addView(right, new LinearLayout.LayoutParams(0, h, (float) b));
        box.addView(bar);
        return box;
    }

    private void playWeek() {
        WeekReport r = game().advanceWeek();
        refresh();
        if (r.gameOver) {
            Ui.info(ctx(), "Failliet", game().gameOverReason());
        } else if (r.newSeasonStarted) {
            Ui.info(ctx(), "Nieuw seizoen", join(r.messages));
        } else {
            showReport(r);
        }
    }

    private void playRestOfSeason() {
        Game g = game();
        WeekReport last = null;
        while (!g.isSeasonOver() && !g.isGameOver()) {
            last = g.advanceWeek();
        }
        refresh();
        if (g.isGameOver()) Ui.info(ctx(), "Failliet", g.gameOverReason());
        else if (last != null && last.seasonSummary != null) Ui.info(ctx(), "Einde seizoen", last.seasonSummary.text());
    }

    private void showReport(WeekReport r) {
        StringBuilder sb = new StringBuilder();
        Club me = game().human();
        if (r.leagueMatch != null) {
            sb.append("Competitie\n").append(r.leagueMatch.describe()).append("\n");
            appendScorers(sb, r.leagueMatch, me);
            appendOpponentSetup(sb, r.leagueMatch, me);
            if (r.leagueMatch.attendance > 0) {
                sb.append("Toeschouwers: ").append(Fmt.number(r.leagueMatch.attendance)).append("\n");
            }
            sb.append("\n");
        }
        if (r.cupMatch != null) {
            sb.append(Cup.NAME).append(" – ").append(r.cupRoundName).append("\n")
                    .append(r.cupMatch.describe()).append("\n");
            if (r.cupMatch.penaltyWinner != null) {
                sb.append(r.cupMatch.penaltyWinner.name).append(" wint na strafschoppen\n");
            }
            appendScorers(sb, r.cupMatch, me);
            appendOpponentSetup(sb, r.cupMatch, me);
            sb.append("\n");
        }
        if (!r.messages.isEmpty()) sb.append(join(r.messages));

        String title = r.leagueMatch == null ? "Week " + game().week()
                : resultTitle(r.leagueMatch.resultFor(me));
        AlertDialog.Builder b = new AlertDialog.Builder(ctx())
                .setTitle(title)
                .setMessage(sb.toString().trim());
        if (r.seasonSummary != null) {
            b.setPositiveButton("Seizoensoverzicht", (d, w) ->
                    Ui.info(ctx(), "Einde seizoen", r.seasonSummary.text()));
        } else {
            b.setPositiveButton("Verder", null);
        }
        b.show();
    }

    private static String resultTitle(String r) {
        if (r.equals("W")) return "Gewonnen! 🎉";
        if (r.equals("V")) return "Verloren";
        return "Gelijkspel";
    }

    private static void appendScorers(StringBuilder sb, Fixture f, Club me) {
        List<String> ours = f.home == me ? f.homeScorers : f.awayScorers;
        if (!ours.isEmpty()) sb.append("Onze doelpunten: ").append(scorers(ours)).append("\n");
    }

    private static void appendOpponentSetup(StringBuilder sb, Fixture f, Club me) {
        String setup = f.home == me ? f.awaySetup : f.homeSetup;
        if (setup != null) sb.append("Tegenstander speelde ").append(setup).append("\n");
    }

    /** "Jansen (2), de Vries" */
    static String scorers(List<String> names) {
        Map<String, Integer> count = new LinkedHashMap<>();
        for (String n : names) {
            Integer old = count.get(n);
            count.put(n, old == null ? 1 : old + 1);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : count.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey());
            if (e.getValue() > 1) sb.append(" (").append(e.getValue()).append(")");
        }
        return sb.toString();
    }

    private static String join(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) sb.append(s).append("\n");
        return sb.toString().trim();
    }
}
