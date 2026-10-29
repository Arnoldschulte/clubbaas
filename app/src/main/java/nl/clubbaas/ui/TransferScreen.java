package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Player;
import nl.clubbaas.engine.Position;
import nl.clubbaas.engine.StaffRole;

/** Transfermarkt en spelers van andere clubs. Bij aankoop kun je onderhandelen. */
public class TransferScreen extends Screen {
    private Position filter;        // null = alle posities
    private boolean otherClubs;     // false = transfermarkt

    public TransferScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Transfers";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Club me = game().human();

        LinearLayout info = Ui.card(c);
        info.addView(Ui.keyValue(c, "Budget", Fmt.money(me.money)));
        info.addView(Ui.keyValue(c, "Selectie", me.squad.size() + " spelers"));
        if (!otherClubs && me.staffRating(StaffRole.SCOUT) == 0) {
            info.addView(Ui.muted(c, "Tip: met een scout zie je betere spelers op de markt."));
        }
        if (otherClubs) {
            info.addView(Ui.muted(c, "Spelers van clubs uit jouw divisie en de divisies erboven en eronder. "
                    + "Basisspelers en spelers van hogere clubs zijn duurder."));
        }
        root.addView(info);

        LinearLayout tabs = Ui.row(c);
        tabs.setPadding(0, 0, 0, Ui.dp(c, 8));
        tabs.addView(Ui.chip(c, "Transfermarkt", !otherClubs, v -> { otherClubs = false; refresh(); }));
        tabs.addView(Ui.chip(c, "Andere clubs", otherClubs, v -> { otherClubs = true; refresh(); }));
        root.addView(tabs);

        LinearLayout chips = Ui.row(c);
        chips.setPadding(0, 0, 0, Ui.dp(c, 12));
        chips.addView(Ui.chip(c, "Alle", filter == null, v -> { filter = null; refresh(); }));
        for (Position p : Position.values()) {
            chips.addView(Ui.chip(c, p.code, filter == p, v -> { filter = p; refresh(); }));
        }
        root.addView(chips);

        List<Player> list = new ArrayList<>();
        if (otherClubs) {
            list.addAll(game().scoutablePlayers(filter, 40));
        } else {
            for (Player p : game().market()) if (filter == null || p.position == filter) list.add(p);
            list.sort((a, b) -> b.skill - a.skill);
        }

        LinearLayout card = Ui.card(c);
        for (int i = 0; i < list.size(); i++) {
            Player p = list.get(i);
            if (i > 0) card.addView(Ui.divider(c));
            String price;
            if (otherClubs) {
                Club seller = game().sellerOf(p);
                price = (seller == null ? "" : seller.name + " · ") + Fmt.money(game().askingPrice(p));
            } else {
                price = p.isFreeAgent() ? "Transfervrij" : Fmt.money(p.askingPrice);
            }
            String detail = p.age + " jr · " + price;
            card.addView(PlayerRow.create(c, p, detail,
                    v -> TransferDialogs.negotiate(ctx(), game(), p, this::refresh)));
        }
        if (list.isEmpty()) card.addView(Ui.muted(c, "Geen spelers beschikbaar."));
        root.addView(card);
    }
}
