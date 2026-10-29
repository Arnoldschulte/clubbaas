package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.ActionResult;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.Ledger;

/** Kasboek per week en per seizoen, plus de bank. */
public class FinanceScreen extends Screen {

    public FinanceScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Financiën";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();
        Club me = g.human();

        LinearLayout overview = Ui.card(c, "Overzicht");
        overview.addView(Ui.keyValue(c, "Kas", Fmt.money(me.money)));
        overview.addView(Ui.keyValue(c, "Lening", Fmt.money(me.loan)));
        overview.addView(Ui.keyValue(c, "Sponsor (per seizoen)", Fmt.money(me.sponsorPerSeason)));
        overview.addView(Ui.keyValue(c, "Tv-gelden (per week)", Fmt.money(g.tvMoneyWeekly())));
        long fixed = me.sponsorPerSeason / Game.WEEKS_PER_SEASON + g.tvMoneyWeekly()
                - me.weeklyPlayerWages() - me.weeklyStaffWages();
        overview.addView(Ui.keyValue(c, "Vaste inkomsten − salarissen p/w", Fmt.money(fixed)));
        overview.addView(Ui.muted(c, "Daarbovenop komen de recettes van thuiswedstrijden (om de week)."));
        if (me.money < 0) {
            TextView warn = Ui.text(c, "Je staat rood! Na 8 weken rood volgt faillissement.", 14, Ui.BAD, true);
            warn.setPadding(0, Ui.dp(c, 8), 0, 0);
            overview.addView(warn);
        }
        root.addView(overview);

        root.addView(ledgerCard(c, "Deze week", me.ledger.week()));
        root.addView(ledgerCard(c, "Dit seizoen", me.ledger.season()));

        LinearLayout bank = Ui.card(c, "Bank");
        bank.addView(Ui.keyValue(c, "Maximale lening", Fmt.money(g.loanLimit())));
        bank.addView(Ui.muted(c, "Rente: 0,25% per week over het openstaande bedrag."));
        long step = Math.max(10_000, Math.round(g.loanLimit() / 4.0 / 1000) * 1000);
        bank.addView(Ui.secondaryButton(c, "Leen " + Fmt.money(step), v -> act(g.borrow(step))));
        if (me.loan > 0) {
            long repay = Math.min(step, me.loan);
            bank.addView(Ui.secondaryButton(c, "Los " + Fmt.money(repay) + " af", v -> act(g.repay(repay))));
        }
        root.addView(bank);
    }

    private LinearLayout ledgerCard(Context c, String title, Map<String, Long> entries) {
        LinearLayout card = Ui.card(c, title);
        if (entries.isEmpty()) {
            card.addView(Ui.muted(c, "Nog geen boekingen."));
            return card;
        }
        for (Map.Entry<String, Long> e : entries.entrySet()) {
            LinearLayout kv = Ui.keyValue(c, e.getKey(), Fmt.money(e.getValue()));
            ((TextView) kv.getChildAt(1)).setTextColor(e.getValue() >= 0 ? Ui.GOOD : Ui.BAD);
            card.addView(kv);
        }
        card.addView(Ui.divider(c));
        long total = Ledger.total(entries);
        LinearLayout t = Ui.keyValue(c, "Resultaat", Fmt.money(total));
        ((TextView) t.getChildAt(1)).setTextColor(total >= 0 ? Ui.GOOD : Ui.BAD);
        card.addView(t);
        return card;
    }

    private void act(ActionResult r) {
        Ui.show(ctx(), r);
        refresh();
    }
}
