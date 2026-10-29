package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.ActionResult;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.Staff;
import nl.clubbaas.engine.StaffRole;

/** Staf aanstellen en ontslaan. Kandidaten worden elke vier weken ververst. */
public class StaffScreen extends Screen {

    public StaffScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Staf";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();
        LinearLayout intro = Ui.card(c);
        intro.addView(Ui.keyValue(c, "Stafsalarissen per week", Fmt.money(g.human().weeklyStaffWages())));
        intro.addView(Ui.muted(c, "Nieuwe kandidaten verschijnen elke vier weken. Bij ontslag betaal je "
                + "8 weken salaris als afkoopsom."));
        root.addView(intro);

        for (StaffRole role : StaffRole.values()) {
            LinearLayout card = Ui.card(c, role.label);
            card.addView(Ui.muted(c, role.effect));
            Staff cur = g.human().staff.get(role);
            if (cur != null) {
                card.addView(Ui.keyValue(c, cur.name, cur.stars()));
                card.addView(Ui.muted(c, Fmt.money(cur.wage) + " per week"));
                card.addView(Ui.dangerButton(c, "Ontslaan (" + Fmt.money(cur.severance()) + ")", v ->
                        Ui.confirm(c, "Ontslaan", cur.name + " ontslaan voor " + Fmt.money(cur.severance()) + "?",
                                "Ontslaan", () -> act(g.fireStaff(role)))));
            } else {
                card.addView(Ui.text(c, "Vacature: niemand aangesteld", 15, Ui.BAD, true));
            }

            android.widget.TextView candLabel = Ui.muted(c, "Kandidaten");
            candLabel.setPadding(0, Ui.dp(c, 12), 0, 0);
            card.addView(candLabel);
            for (Staff s : g.candidates(role)) {
                long cost = s.signingFee() + (cur == null ? 0 : cur.severance());
                card.addView(Ui.keyValue(c, s.name, s.stars()));
                card.addView(Ui.secondaryButton(c, "Aanstellen: " + Fmt.money(s.wage) + " p/w, kosten nu "
                        + Fmt.money(cost), v -> act(g.hireStaff(s))));
            }
            if (g.candidates(role).isEmpty()) card.addView(Ui.muted(c, "Geen kandidaten op dit moment."));
            root.addView(card);
        }
    }

    private void act(ActionResult r) {
        Ui.show(ctx(), r);
        refresh();
    }
}
