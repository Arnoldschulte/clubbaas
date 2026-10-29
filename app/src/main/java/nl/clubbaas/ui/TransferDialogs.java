package nl.clubbaas.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import nl.clubbaas.engine.ActionResult;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.NegotiationResult;
import nl.clubbaas.engine.Player;
import nl.clubbaas.engine.TransferBid;

/** Dialogen voor onderhandelen: bieden op een speler en reageren op biedingen. */
final class TransferDialogs {
    private TransferDialogs() {}

    // ---- Kopen ------------------------------------------------------------------------

    static void negotiate(Context c, Game g, Player p, Runnable onChange) {
        Club seller = g.sellerOf(p);
        long asking = g.askingPrice(p);
        if (p.isFreeAgent() && seller == null) {
            Ui.confirm(c, p.name, info(g, p) + "\n\nTransfervrij. Tekengeld: " + Fmt.money(asking), "Contracteren", () -> {
                Ui.show(c, g.buyPlayer(p));
                onChange.run();
            });
            return;
        }
        long[] amounts = {round(asking * 0.8), round(asking * 0.9), asking};
        String from = seller == null ? "Transfermarkt" : seller.name + " (" + g.division(seller.level).name + ")";
        Ui.actionDialog(c, p.name, info(g, p) + "\n\nClub: " + from + "\nVraagprijs: " + Fmt.money(asking)
                        + "\n\nBied lager en je bespaart misschien geld, maar bied je te laag, dan wil de verkoper "
                        + "deze week niet meer praten.",
                new String[]{"Vraagprijs bieden: " + Fmt.money(amounts[2]), "Bied " + Fmt.money(amounts[1]) + " (90%)",
                        "Bied " + Fmt.money(amounts[0]) + " (80%)", "Eigen bod…"},
                new int[]{Ui.STYLE_PRIMARY, Ui.STYLE_SECONDARY, Ui.STYLE_SECONDARY, Ui.STYLE_SECONDARY},
                new Runnable[]{
                        () -> handle(c, g, p, g.bid(p, amounts[2]), onChange),
                        () -> handle(c, g, p, g.bid(p, amounts[1]), onChange),
                        () -> handle(c, g, p, g.bid(p, amounts[0]), onChange),
                        () -> customBid(c, g, p, onChange)});
    }

    private static String info(Game g, Player p) {
        int bestOwn = 0;
        for (Player own : g.human().squad) if (own.position == p.position) bestOwn = Math.max(bestOwn, own.skill);
        return p.position.label + ", " + p.age + " jaar\n"
                + "Vaardigheid: " + p.skill + (p.age <= 24 ? " (potentie " + p.potential + ")" : "") + "\n"
                + "Jouw beste " + p.position.label.toLowerCase(Fmt.NL) + ": " + bestOwn + "\n"
                + "Salaris: " + Fmt.money(Player.wageFor(p.skill, p.age)) + " per week";
    }

    private static void customBid(Context c, Game g, Player p, Runnable onChange) {
        EditText input = new EditText(c);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Bedrag in euro's");
        LinearLayout box = new LinearLayout(c);
        int pad = Ui.dp(c, 24);
        box.setPadding(pad, Ui.dp(c, 8), pad, 0);
        box.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(c)
                .setTitle("Bod op " + p.name)
                .setView(box)
                .setPositiveButton("Bieden", (d, w) -> {
                    long amount;
                    try {
                        amount = Long.parseLong(input.getText().toString().replace(".", "").trim());
                    } catch (NumberFormatException e) {
                        Ui.toast(c, "Vul een bedrag in.");
                        return;
                    }
                    handle(c, g, p, g.bid(p, amount), onChange);
                })
                .setNegativeButton("Annuleren", null)
                .show();
    }

    private static void handle(Context c, Game g, Player p, NegotiationResult r, Runnable onChange) {
        switch (r.outcome) {
            case ACCEPTED:
                Ui.toast(c, r.message);
                onChange.run();
                break;
            case COUNTER:
                Ui.actionDialog(c, "Tegenbod", r.message,
                        new String[]{"Accepteren: " + Fmt.money(r.counter), "Nieuw bod doen", "Stoppen"},
                        new int[]{Ui.STYLE_PRIMARY, Ui.STYLE_SECONDARY, Ui.STYLE_SECONDARY},
                        new Runnable[]{() -> {
                            Ui.show(c, g.acceptCounter(p));
                            onChange.run();
                        }, () -> customBid(c, g, p, onChange), () -> { }});
                break;
            default:
                Ui.info(c, "Geen akkoord", r.message);
                onChange.run();
        }
    }

    private static long round(double v) {
        return Math.round(v / 500.0) * 500;
    }

    // ---- Verkopen ---------------------------------------------------------------------

    static void respondToBid(Context c, Game g, TransferBid b, Runnable onChange) {
        Player p = b.player;
        String msg = b.buyerName() + " biedt " + Fmt.money(b.amount) + " voor " + p.name
                + " (marktwaarde " + Fmt.money(p.value()) + ").";
        if (b.raised) msg += "\n\nDit is hun laatste bod.";
        else msg += "\n\nMeer vragen kan: dan verhogen ze, of ze haken af.";
        if (p.wantsTransfer()) msg += "\n\n" + p.name + " wil graag weg; afwijzen valt slecht bij hem.";
        Ui.actionDialog(c, "Bod op " + p.name, msg,
                new String[]{"Accepteren", "Meer vragen", "Afwijzen"},
                new int[]{Ui.STYLE_PRIMARY, Ui.STYLE_SECONDARY, Ui.STYLE_DANGER},
                new Runnable[]{
                        () -> {
                            Ui.show(c, g.acceptBid(b));
                            onChange.run();
                        },
                        () -> {
                            ActionResult r = g.askMore(b);
                            Ui.show(c, r);
                            onChange.run();
                            if (r.ok) respondToBid(c, g, b, onChange);
                        },
                        () -> {
                            Ui.show(c, g.rejectBid(b));
                            onChange.run();
                        }});
    }
}
