package nl.clubbaas.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.ActionResult;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.Stadium;

/** Stadion: capaciteit, ticketprijs en faciliteiten. */
public class StadiumScreen extends Screen {

    public StadiumScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Stadion";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();
        Stadium s = g.human().stadium;

        // Kaartverkoop
        LinearLayout tickets = Ui.card(c, "Kaartverkoop");
        tickets.addView(Ui.keyValue(c, "Capaciteit", Fmt.number(s.capacity)));
        tickets.addView(Ui.keyValue(c, "Fans", Fmt.number(g.human().fans)));
        tickets.addView(Ui.keyValue(c, "Laatste thuiswedstrijd", Fmt.number(g.human().lastAttendance) + " toeschouwers"));

        LinearLayout priceRow = Ui.row(c);
        priceRow.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 4));
        priceRow.addView(stepButton(c, "−5", -5));
        priceRow.addView(stepButton(c, "−1", -1));
        TextView price = Ui.text(c, "€ " + s.ticketPrice, 22, Ui.TEXT, true);
        price.setGravity(Gravity.CENTER);
        priceRow.addView(price, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        priceRow.addView(stepButton(c, "+1", 1));
        priceRow.addView(stepButton(c, "+5", 5));
        tickets.addView(priceRow);

        int expected = g.expectedAttendance(s.ticketPrice);
        long gate = (long) expected * s.ticketPrice + Math.round(expected * s.spendPerFan());
        tickets.addView(Ui.keyValue(c, "Verwachte toeschouwers", Fmt.number(expected)));
        tickets.addView(Ui.keyValue(c, "Opbrengst per thuiswedstrijd", Fmt.money(gate)));
        tickets.addView(Ui.muted(c, "Een normale prijs in deze divisie is ongeveer € " + g.fairPrice()
                + ". Hoger levert per kaartje meer op, maar er komen minder mensen."));
        root.addView(tickets);

        // Uitbreiden
        LinearLayout expand = Ui.card(c, "Tribunes");
        expand.addView(Ui.body(c, "Bouw " + Fmt.number(s.nextExpansionSeats()) + " extra plaatsen voor "
                + Fmt.money(s.expansionCost()) + "."));
        if (expected >= s.capacity) {
            TextView full = Ui.text(c, "Je stadion zit vol! Uitbreiden levert direct meer op.", 14, Ui.GOOD, true);
            full.setPadding(0, Ui.dp(c, 6), 0, 0);
            expand.addView(full);
        }
        expand.addView(Ui.button(c, "Uitbreiden", v -> act(g.expandStadium())));
        root.addView(expand);

        // Faciliteiten
        facility(root, c, g, Game.Facility.COMMERCIEEL, "Horeca & fanshop",
                "Bezoekers besteden " + Fmt.money(Math.round(s.spendPerFan())) + " per persoon.");
        facility(root, c, g, Game.Facility.TRAINING, "Trainingscomplex",
                "Spelers ontwikkelen zich sneller aan het eind van elk seizoen.");
        facility(root, c, g, Game.Facility.JEUGD, "Jeugdacademie",
                "Meer en betere talenten bij de jaarlijkse jeugdinstroom.");
    }

    private void facility(LinearLayout root, Context c, Game g, Game.Facility f, String name, String effect) {
        int level = g.facilityLevel(f);
        LinearLayout card = Ui.card(c, name);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < Stadium.MAX_LEVEL; i++) stars.append(i < level ? "■ " : "□ ");
        card.addView(Ui.text(c, stars.toString().trim() + "   niveau " + level, 15, Ui.PRIMARY, true));
        card.addView(Ui.muted(c, effect));
        if (level < Stadium.MAX_LEVEL) {
            card.addView(Ui.button(c, "Upgraden voor " + Fmt.money(g.facilityCost(f)), v -> act(g.upgradeFacility(f))));
        } else {
            card.addView(Ui.muted(c, "Maximaal niveau bereikt."));
        }
        root.addView(card);
    }

    private TextView stepButton(Context c, String label, int delta) {
        TextView t = Ui.chip(c, label, false, v -> {
            game().setTicketPrice(game().human().stadium.ticketPrice + delta);
            refresh();
        });
        return t;
    }

    private void act(ActionResult r) {
        Ui.show(ctx(), r);
        refresh();
    }
}
