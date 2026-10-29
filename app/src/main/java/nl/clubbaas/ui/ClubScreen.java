package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;

import java.util.List;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Cup;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.SeasonRecord;

/** Clubinfo, de erelijst/historie en een nieuw spel starten. */
public class ClubScreen extends Screen {

    /** Balk van 0 tot 100, rood-geel-groen. */
    private static LinearLayout meter(Context c, int value) {
        LinearLayout bar = Ui.row(c);
        bar.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 8));
        int h = Ui.dp(c, 8);
        int color = value >= 55 ? Ui.GOOD : value >= 30 ? Ui.ACCENT : Ui.BAD;
        android.view.View filled = new android.view.View(c);
        filled.setBackground(Ui.rounded(color, Ui.dp(c, 4)));
        android.view.View empty = new android.view.View(c);
        empty.setBackground(Ui.rounded(Ui.LINE, Ui.dp(c, 4)));
        bar.addView(filled, new LinearLayout.LayoutParams(0, h, Math.max(1, value)));
        bar.addView(empty, new LinearLayout.LayoutParams(0, h, Math.max(1, 100 - value)));
        return bar;
    }

    public ClubScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Club & historie";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Club me = game().human();

        LinearLayout info = Ui.card(c, me.name);
        info.addView(Ui.keyValue(c, "Voorzitter", game().chairmanName()));
        info.addView(Ui.keyValue(c, "Seizoen", game().seasonLabel()));
        info.addView(Ui.keyValue(c, "Divisie", game().humanDivision().name));
        info.addView(Ui.keyValue(c, "Stadion", Fmt.number(me.stadium.capacity) + " plaatsen"));
        info.addView(Ui.keyValue(c, "Fans", Fmt.number(me.fans)));
        root.addView(info);

        nl.clubbaas.engine.Game g = game();
        LinearLayout board = Ui.card(c, "Bestuur & supporters");
        board.addView(Ui.keyValue(c, "Doel dit seizoen", g.boardObjective().label));
        board.addView(Ui.muted(c, "Minimaal de " + g.boardObjective().targetPosition + "e plaats."));
        board.addView(Ui.keyValue(c, "Vertrouwen bestuur", nl.clubbaas.engine.Game.confidenceLabel(g.boardConfidence())));
        board.addView(meter(c, g.boardConfidence()));
        board.addView(Ui.keyValue(c, "Stemming supporters", nl.clubbaas.engine.Game.supporterLabel(g.supporterMood())));
        board.addView(meter(c, g.supporterMood()));
        board.addView(Ui.muted(c, "Het bestuur let op je positie ten opzichte van het doel, op rood staan en op de "
                + "supporters. Zakt het vertrouwen naar nul, dan word je ontslagen. Supporters reageren op "
                + "resultaten, ticketprijzen en transfers, en bepalen mee hoeveel publiek er komt."));
        root.addView(board);

        List<SeasonRecord> history = me.history;
        int titles = 0, promotions = 0, cups = 0;
        for (SeasonRecord r : history) {
            if (r.outcome.contains("ampioen")) titles++;
            if (r.outcome.equals("Promotie") || r.outcome.equals("Kampioen")) promotions++;
            if (r.cupResult.startsWith("Bekerwinnaar")) cups++;
        }
        LinearLayout honours = Ui.card(c, "Erelijst");
        honours.addView(Ui.keyValue(c, "Kampioenschappen", String.valueOf(titles)));
        honours.addView(Ui.keyValue(c, "Promoties", String.valueOf(promotions)));
        honours.addView(Ui.keyValue(c, Cup.NAME, String.valueOf(cups)));
        root.addView(honours);

        LinearLayout hist = Ui.card(c, "Historie");
        if (history.isEmpty()) hist.addView(Ui.muted(c, "Je eerste seizoen is nog bezig."));
        for (int i = history.size() - 1; i >= 0; i--) {
            SeasonRecord r = history.get(i);
            if (i < history.size() - 1) hist.addView(Ui.divider(c));
            LinearLayout item = new LinearLayout(c);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8));
            String outcome = r.outcome.isEmpty() ? "" : "  ·  " + r.outcome;
            item.addView(Ui.text(c, r.season + ": " + r.position + "e in de " + r.division + outcome, 15, Ui.TEXT, true));
            item.addView(Ui.muted(c, "Beker: " + r.cupResult + "  ·  Topscorer: " + r.topScorer));
            hist.addView(item);
        }
        root.addView(hist);

        LinearLayout danger = Ui.card(c, "Spel");
        boolean live = game().isLiveEnabled();
        danger.addView(Ui.keyValue(c, "Live wedstrijdverslag", live ? "Aan" : "Uit"));
        danger.addView(Ui.secondaryButton(c, live ? "Live verslag uitzetten" : "Live verslag aanzetten", v -> {
            game().setLiveEnabled(!live);
            refresh();
        }));
        danger.addView(Ui.muted(c, "Het spel wordt na elke actie automatisch opgeslagen."));
        danger.addView(Ui.dangerButton(c, "Nieuw spel beginnen", v ->
                Ui.confirm(c, "Nieuw spel", "Je huidige spel wordt definitief verwijderd. Weet je het zeker?",
                        "Verwijderen", act::deleteSave)));
        root.addView(danger);
    }
}
