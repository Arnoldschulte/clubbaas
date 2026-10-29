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
import nl.clubbaas.engine.MoraleBreakdown;
import nl.clubbaas.engine.Player;
import nl.clubbaas.engine.Position;
import nl.clubbaas.engine.Staff;
import nl.clubbaas.engine.StaffRole;

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
        if (me.staff.get(StaffRole.MANAGER) != null) {
            LinearLayout mood = Ui.keyValue(c, "Verhouding met manager", Game.managerMoodLabel(me.managerFrustration()));
            if (me.managerFrustration() >= Game.FRUSTRATION_WARNING) {
                ((android.widget.TextView) mood.getChildAt(1)).setTextColor(Ui.BAD);
            }
            summary.addView(mood);
        }
        int squadMorale = game().squadMorale();
        summary.addView(Ui.keyValue(c, "Sfeer in de selectie",
                Player.moraleEmoji(squadMorale) + " " + Player.moraleLabel(squadMorale)));
        int unhappy = 0;
        for (Player p : me.squad) if (p.wantsTransfer()) unhappy++;
        if (unhappy > 0) {
            LinearLayout req = Ui.keyValue(c, "Transferverzoeken", String.valueOf(unhappy));
            ((android.widget.TextView) req.getChildAt(1)).setTextColor(Ui.BAD);
            summary.addView(req);
        }
        summary.addView(Ui.muted(c, "Tik op een speler voor acties. De beste elf wordt automatisch opgesteld in je "
                + "formatie (" + me.formation().label + "); die kies je bij Tactiek."));
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
                String detail = Player.moraleEmoji(p.morale()) + " "
                        + (p.wantsTransfer() ? "📝 wil weg · " : "")
                        + (p == me.favorite() ? "⭐ " : "") + p.age + " jr · " + Fmt.money(p.wage) + " p/w · "
                        + contractText(p) + (eleven.contains(p) ? " · basis" : "")
                        + (p.goals > 0 ? " · ⚽ " + p.goals : "")
                        + (p.yellowCards % 5 >= 3 ? " · 🟨 " + p.yellowCards % 5 + "/5" : "");
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
        Game g = game();
        boolean isFavorite = g.human().favorite() == p;
        StringBuilder extra = new StringBuilder();
        if (isFavorite) extra.append("\n⭐ Jouw voorkeursspeler: start altijd als hij fit is.");
        if (Game.isShowcased(p)) extra.append("\nIn de etalage: biedingen liggen 10% hoger.");
        if (p.age <= 23) {
            extra.append("\nSpeeltijd: ").append(Math.min(p.appearances, Game.PLAYING_TIME_BONUS_APPS))
                    .append("/").append(Game.PLAYING_TIME_BONUS_APPS)
                    .append(" wedstrijden voor extra ontwikkeling dit seizoen.");
        }
        MoraleBreakdown mb = g.moraleBreakdown(p);
        StringBuilder morale = new StringBuilder("\n\nMoraal: ")
                .append(p.morale()).append(" ").append(Player.moraleEmoji(p.morale())).append(" ")
                .append(Player.moraleLabel(p.morale()));
        int trend = Integer.compare(mb.target, p.morale());
        if (trend != 0) morale.append(trend > 0 ? "  ↗ stijgend" : "  ↘ dalend");
        for (String reason : mb.reasons) morale.append("\n• ").append(reason);
        if (p.wantsTransfer()) {
            morale.append("\n📝 Heeft een transferverzoek ingediend: verlengt niet en biedingen liggen 10% lager.");
        }
        String info = p.position.label + ", " + p.age + " jaar\n"
                + "Vaardigheid: " + p.skill + "  (potentie: " + (p.age <= 24 ? String.valueOf(p.potential) : "bereikt") + ")\n"
                + "Marktwaarde: " + Fmt.money(p.value()) + "\n"
                + "Salaris: " + Fmt.money(p.wage) + " per week\n"
                + "Contract: " + (p.contractYears == 0 ? "loopt af na dit seizoen" : "nog " + p.contractYears + " seizoen(en) na dit seizoen") + "\n"
                + "Dit seizoen: " + p.appearances + " wedstrijden, " + p.goals + " doelpunten"
                + (p.isInjured() ? "\nGeblesseerd: nog " + p.injuryWeeks + " week/weken" : "")
                + (p.isSuspended() ? "\nGeschorst: nog " + p.suspension + " wedstrijd(en)" : "")
                + "\nGele kaarten dit seizoen: " + p.yellowCards + " (bij elke 5e een wedstrijd schorsing)"
                + extra
                + morale;
        Ui.actionDialog(c, p.name, info,
                new String[]{isFavorite ? "Niet meer voortrekken" : "⭐ Vraag de manager hem te laten starten",
                        "Verkopen", "Contract verlengen", "Contract ontbinden"},
                new int[]{Ui.STYLE_PRIMARY, Ui.STYLE_SECONDARY, Ui.STYLE_SECONDARY, Ui.STYLE_DANGER},
                new Runnable[]{() -> toggleFavorite(p, isFavorite), () -> sell(p), () -> renew(p), () -> release(p)});
    }

    private void toggleFavorite(Player p, boolean isFavorite) {
        Game g = game();
        if (isFavorite) {
            g.setFavorite(null);
            Ui.toast(ctx(), "Je laat de opstelling weer aan de manager over.");
            refresh();
            return;
        }
        Staff manager = g.human().staff.get(StaffRole.MANAGER);
        Player current = g.human().favorite();
        String msg;
        if (manager == null) {
            msg = "Er is geen manager, dus " + p.name + " start gewoon als hij fit is.";
        } else {
            msg = manager.name + " zet " + p.name + " in de basis zolang hij fit is.\n\n"
                    + "Zou de manager hem zelf niet opstellen, dan ergert hij zich: +"
                    + g.frustrationPerForcedMatch() + " per wedstrijd (bij 100 stapt hij op). "
                    + "Speelt hij op eigen kracht, dan zakt de ergernis weer.\n\n"
                    + "Huidige verhouding: " + Game.managerMoodLabel(g.human().managerFrustration()) + ".";
        }
        msg += "\n\nLet op: de speler die voor hem naar de bank moet, wordt daar niet vrolijk van.";
        if (current != null) msg += "\n\nJe huidige voorkeursspeler " + current.name + " wordt dan niet meer voorgetrokken.";
        Ui.confirm(ctx(), "Bemoeien met de opstelling", msg, "Doen", () -> {
            g.setFavorite(p);
            refresh();
        });
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
