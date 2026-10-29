package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.ActionResult;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Player;
import nl.clubbaas.engine.Position;
import nl.clubbaas.engine.StaffRole;

/** Transfermarkt. Elke week verdwijnen er een paar spelers en komen er nieuwe bij. */
public class TransferScreen extends Screen {
    private Position filter; // null = alle posities

    public TransferScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Transfermarkt";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Club me = game().human();

        LinearLayout info = Ui.card(c);
        info.addView(Ui.keyValue(c, "Budget", Fmt.money(me.money)));
        info.addView(Ui.keyValue(c, "Selectie", me.squad.size() + " spelers"));
        if (me.staffRating(StaffRole.SCOUT) == 0) {
            info.addView(Ui.muted(c, "Tip: met een scout zie je betere spelers op de markt."));
        }
        root.addView(info);

        LinearLayout chips = Ui.row(c);
        chips.setPadding(0, 0, 0, Ui.dp(c, 12));
        chips.addView(Ui.chip(c, "Alle", filter == null, v -> { filter = null; refresh(); }));
        for (Position p : Position.values()) {
            chips.addView(Ui.chip(c, p.code, filter == p, v -> { filter = p; refresh(); }));
        }
        root.addView(chips);

        List<Player> list = new ArrayList<>();
        for (Player p : game().market()) if (filter == null || p.position == filter) list.add(p);
        list.sort((a, b) -> b.skill - a.skill);

        LinearLayout card = Ui.card(c);
        for (int i = 0; i < list.size(); i++) {
            Player p = list.get(i);
            if (i > 0) card.addView(Ui.divider(c));
            String price = p.isFreeAgent() ? "Transfervrij" : Fmt.money(p.askingPrice);
            String detail = p.age + " jr · " + price + " · " + Fmt.money(p.wage) + " p/w";
            card.addView(PlayerRow.create(c, p, detail, v -> showPlayer(p)));
        }
        if (list.isEmpty()) card.addView(Ui.muted(c, "Geen spelers beschikbaar."));
        root.addView(card);
    }

    private void showPlayer(Player p) {
        Club me = game().human();
        int bestOwn = 0;
        for (Player own : me.squad) if (own.position == p.position) bestOwn = Math.max(bestOwn, own.skill);
        long cost = game().signingCost(p);
        String msg = p.position.label + ", " + p.age + " jaar\n"
                + "Vaardigheid: " + p.skill + (p.age <= 24 ? " (potentie " + p.potential + ")" : "") + "\n"
                + "Jouw beste " + p.position.label.toLowerCase(Fmt.NL) + ": " + bestOwn + "\n\n"
                + (p.isFreeAgent() ? "Transfervrij, tekengeld: " : "Vraagprijs: ") + Fmt.money(cost) + "\n"
                + "Salaris: " + Fmt.money(p.wage) + " per week";
        Ui.confirm(ctx(), p.name, msg, "Kopen", () -> {
            ActionResult r = game().buyPlayer(p);
            Ui.show(ctx(), r);
            refresh();
        });
    }
}
