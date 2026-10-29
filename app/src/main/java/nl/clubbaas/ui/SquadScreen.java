package nl.clubbaas.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.LinearLayout;

import java.util.List;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.ActionResult;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.MatchEngine;
import nl.clubbaas.engine.Player;
import nl.clubbaas.engine.Position;

/** Je selectie, met per speler acties: verkopen, contract verlengen of ontslaan. */
public class SquadScreen extends Screen {

    public SquadScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Selectie";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Club me = game().human();

        LinearLayout summary = Ui.card(c, "Samenvatting");
        summary.addView(Ui.keyValue(c, "Spelers", me.squad.size() + " / " + Game.MAX_SQUAD));
        summary.addView(Ui.keyValue(c, "Salarissen per week", Fmt.money(me.weeklyPlayerWages())));
        summary.addView(Ui.keyValue(c, "Teamsterkte (beste elf)", String.valueOf(Math.round(MatchEngine.strength(me)))));
        summary.addView(Ui.keyValue(c, "Waarde selectie", Fmt.money(me.totalSquadValue())));
        summary.addView(Ui.muted(c, "Tik op een speler voor acties. De beste elf (1-4-4-2) wordt automatisch opgesteld."));
        root.addView(summary);

        List<Player> eleven = MatchEngine.selectEleven(me);
        List<Player> sorted = me.sortedSquad();
        for (Position pos : Position.values()) {
            LinearLayout card = Ui.card(c, pos.label + "s");
            boolean any = false;
            for (Player p : sorted) {
                if (p.position != pos) continue;
                if (any) card.addView(Ui.divider(c));
                any = true;
                String detail = p.age + " jr · " + Fmt.money(p.wage) + " p/w · "
                        + contractText(p) + (eleven.contains(p) ? " · basis" : "")
                        + (p.goals > 0 ? " · ⚽ " + p.goals : "");
                card.addView(PlayerRow.create(c, p, detail, v -> showPlayer(p)));
            }
            if (!any) card.addView(Ui.muted(c, "Geen spelers op deze positie!"));
            root.addView(card);
        }
    }

    static String contractText(Player p) {
        return p.contractYears == 0 ? "contract loopt af" : "contract +" + p.contractYears + " jr";
    }

    private void showPlayer(Player p) {
        Context c = ctx();
        String info = p.position.label + ", " + p.age + " jaar\n"
                + "Vaardigheid: " + p.skill + "  (potentie: " + (p.age <= 24 ? String.valueOf(p.potential) : "bereikt") + ")\n"
                + "Marktwaarde: " + Fmt.money(p.value()) + "\n"
                + "Salaris: " + Fmt.money(p.wage) + " per week\n"
                + "Contract: " + (p.contractYears == 0 ? "loopt af na dit seizoen" : "nog " + p.contractYears + " seizoen(en) na dit seizoen") + "\n"
                + "Dit seizoen: " + p.appearances + " wedstrijden, " + p.goals + " doelpunten"
                + (p.isInjured() ? "\nGeblesseerd: nog " + p.injuryWeeks + " week/weken" : "");
        new AlertDialog.Builder(c)
                .setTitle(p.name)
                .setMessage(info)
                .setPositiveButton("Verkopen", (d, w) -> sell(p))
                .setNeutralButton("Verlengen", (d, w) -> renew(p))
                .setNegativeButton("Ontbinden", (d, w) -> release(p))
                .show();
    }

    private void sell(Player p) {
        long offer = game().saleOffer(p);
        Ui.confirm(ctx(), "Bod op " + p.name,
                "Een club biedt " + Fmt.money(offer) + " (marktwaarde " + Fmt.money(p.value()) + ").\n\n"
                        + "Je krijgt deze week geen ander bod op deze speler.",
                "Accepteren", () -> {
                    ActionResult r = game().sellPlayer(p);
                    Ui.show(ctx(), r);
                    refresh();
                });
    }

    private void renew(Player p) {
        Game g = game();
        int wage = g.renewalWage(p);
        long bonus = g.renewalBonus(p);
        String[] options = {"1 seizoen", "2 seizoenen", "3 seizoenen"};
        new AlertDialog.Builder(ctx())
                .setTitle("Verlengen: " + Fmt.money(wage) + " p/w, premie " + Fmt.money(bonus))
                .setItems(options, (d, which) -> {
                    ActionResult r = g.renewContract(p, which + 1);
                    Ui.show(ctx(), r);
                    refresh();
                })
                .setNegativeButton("Annuleren", null)
                .show();
    }

    private void release(Player p) {
        long cost = game().releaseCost(p);
        Ui.confirm(ctx(), "Contract ontbinden",
                p.name + " vertrekt direct. De afkoopsom is " + Fmt.money(cost) + ".",
                "Ontbinden", () -> {
                    ActionResult r = game().releasePlayer(p);
                    Ui.show(ctx(), r);
                    refresh();
                });
    }
}
