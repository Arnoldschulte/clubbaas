package nl.clubbaas.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Club;
import nl.clubbaas.engine.Fixture;
import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Formation;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.MatchEngine;
import nl.clubbaas.engine.Player;
import nl.clubbaas.engine.Position;
import nl.clubbaas.engine.Staff;
import nl.clubbaas.engine.StaffRole;
import nl.clubbaas.engine.Tactic;
import nl.clubbaas.engine.TeamRating;

/** Formatie en speelwijze kiezen, met een vergelijking tegen de volgende tegenstander. */
public class TacticsScreen extends Screen {

    public TacticsScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Tactiek";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        Game g = game();
        Club me = g.human();
        TeamRating mine = MatchEngine.rate(me);

        // Formatie
        LinearLayout formCard = Ui.card(c, "Formatie");
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = Ui.row(c);
        for (Formation f : Formation.values()) {
            chips.addView(Ui.chip(c, f.label, f == me.formation(), v -> {
                g.setFormation(f);
                refresh();
            }));
        }
        hs.addView(chips);
        formCard.addView(hs);

        TextView hint = Ui.muted(c, "Aanval en verdediging per formatie met jouw huidige selectie:");
        hint.setPadding(0, Ui.dp(c, 12), 0, Ui.dp(c, 4));
        formCard.addView(hint);
        LinearLayout head = Ui.row(c);
        head.addView(Ui.cell(c, "Formatie", 1.2f, true, Gravity.START));
        head.addView(Ui.cell(c, "Aanval", 1f, true, Gravity.END));
        head.addView(Ui.cell(c, "Verdediging", 1.3f, true, Gravity.END));
        formCard.addView(head);
        for (Formation f : Formation.values()) {
            TeamRating r = MatchEngine.rate(me, f);
            LinearLayout row = Ui.row(c);
            int pv = Ui.dp(c, 4);
            row.setPadding(Ui.dp(c, 4), pv, Ui.dp(c, 4), pv);
            if (f == me.formation()) row.setBackground(Ui.rounded(Ui.HIGHLIGHT, Ui.dp(c, 6)));
            row.addView(Ui.cell(c, f.label, 1.2f, f == me.formation(), Gravity.START));
            row.addView(Ui.cell(c, num(r.attack), 1f, false, Gravity.END));
            row.addView(Ui.cell(c, num(r.defense), 1.3f, false, Gravity.END));
            row.setOnClickListener(v -> {
                g.setFormation(f);
                refresh();
            });
            formCard.addView(row);
        }
        root.addView(formCard);

        // Speelwijze
        LinearLayout styleCard = Ui.card(c, "Speelwijze");
        LinearLayout styles = Ui.row(c);
        for (Tactic t : Tactic.values()) {
            styles.addView(Ui.chip(c, t.label, t == me.tactic(), v -> {
                g.setTactic(t);
                refresh();
            }));
        }
        styleCard.addView(styles);
        TextView desc = Ui.body(c, me.tactic().description);
        desc.setPadding(0, Ui.dp(c, 10), 0, 0);
        styleCard.addView(desc);
        root.addView(styleCard);

        // Volgende tegenstander
        Fixture next = g.nextLeagueFixture();
        if (next != null) {
            Club opp = next.opponentOf(me);
            TeamRating theirs = MatchEngine.rate(opp);
            LinearLayout oppCard = Ui.card(c, "Volgende tegenstander");
            oppCard.addView(Ui.text(c, (next.home == me ? "Thuis tegen " : "Uit bij ") + opp.name, 16, Ui.TEXT, true));
            oppCard.addView(Ui.muted(c, "Speelt meestal " + theirs.formation.label + ", verwacht: "
                    + g.expectedOpponentTactic().label.toLowerCase(Fmt.NL)));
            LinearLayout cmpHead = Ui.row(c);
            cmpHead.setPadding(0, Ui.dp(c, 10), 0, 0);
            cmpHead.addView(Ui.cell(c, "", 1.4f, true, Gravity.START));
            cmpHead.addView(Ui.cell(c, "Wij", 1f, true, Gravity.END));
            cmpHead.addView(Ui.cell(c, "Zij", 1f, true, Gravity.END));
            oppCard.addView(cmpHead);
            oppCard.addView(compareRow(c, "Aanval", mine.attack, theirs.attack));
            oppCard.addView(compareRow(c, "Verdediging", mine.defense, theirs.defense));
            double[] xg = g.expectedGoalsNextMatch();
            if (xg != null) {
                TextView exp = Ui.body(c, String.format(Locale.ROOT, "Verwachte score: %.1f – %.1f", xg[0], xg[1])
                        .replace('.', ','));
                exp.setPadding(0, Ui.dp(c, 10), 0, 0);
                oppCard.addView(exp);
            }
            oppCard.addView(Ui.muted(c, adviceFor(mine, theirs)));
            root.addView(oppCard);
        }

        // Invloed van de voorzitter
        root.addView(influenceCard(c, g, me, mine));

        // Basiself
        LinearLayout xi = Ui.card(c, "Basiself (" + mine.formation.label + ")");
        xi.addView(Ui.muted(c, "De manager stelt de beste spelers op en laat vermoeide spelers soms rusten."));
        StringBuilder missing = new StringBuilder();
        for (Player p : me.squad) {
            if (p.isAvailable()) continue;
            if (missing.length() > 0) missing.append(", ");
            missing.append(p.name).append(p.isSuspended() ? " (geschorst)" : " (geblesseerd)");
        }
        if (missing.length() > 0) {
            TextView miss = Ui.text(c, "Ontbreken: " + missing, 13, Ui.BAD, false);
            miss.setPadding(0, Ui.dp(c, 4), 0, 0);
            xi.addView(miss);
        }
        Player favorite = me.favorite();
        Position last = null;
        for (TeamRating.Slot s : mine.slots) {
            if (s.role != last) {
                TextView label = Ui.text(c, s.role.label + "s", 13, Ui.PRIMARY, true);
                label.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 2));
                xi.addView(label);
                last = s.role;
            }
            String name = s.player == null ? "— noodspeler (te weinig fitte spelers)"
                    : (s.player == favorite ? "⭐ " : "") + s.player.name
                    + (s.player == favorite && mine.favoriteForced ? "  (op jouw verzoek)" : "");
            String right = s.player == null ? "10"
                    : Player.moraleEmoji(s.player.morale()) + " ⚡" + s.player.fitness() + "%  " + s.player.skill;
            LinearLayout kv = Ui.keyValue(c, name + (s.outOfPosition() ? "  (uit positie, 70%)" : ""), right);
            if (s.outOfPosition() || s.player == null) ((TextView) kv.getChildAt(0)).setTextColor(Ui.BAD);
            xi.addView(kv);
        }
        root.addView(xi);
    }

    private LinearLayout influenceCard(Context c, Game g, Club me, TeamRating mine) {
        LinearLayout card = Ui.card(c, "Jouw invloed");
        Staff manager = me.staff.get(StaffRole.MANAGER);
        Player favorite = me.favorite();
        if (manager != null) {
            int frustration = me.managerFrustration();
            LinearLayout mood = Ui.keyValue(c, "Verhouding met " + manager.name, Game.managerMoodLabel(frustration));
            ((TextView) mood.getChildAt(1)).setTextColor(frustration >= Game.FRUSTRATION_WARNING ? Ui.BAD : Ui.GOOD);
            card.addView(mood);
            card.addView(moodBar(c, frustration));
        }
        if (favorite == null) {
            card.addView(Ui.body(c, "Je laat de opstelling aan de manager over."));
            card.addView(Ui.muted(c, "Wil je dat een speler start, bijvoorbeeld een talent dat speeltijd nodig heeft "
                    + "of een speler die je wilt verkopen? Kies hem in de Selectie."));
            card.addView(Ui.secondaryButton(c, "Naar de selectie", v -> act.show(new SquadScreen(act))));
            return card;
        }
        card.addView(Ui.keyValue(c, "Voorkeursspeler", "⭐ " + favorite.name));
        String status;
        if (favorite.isInjured()) {
            status = "Geblesseerd, dus hij speelt nu niet. Geen ergernis.";
        } else if (!mine.favoriteForced) {
            status = "De manager stelt hem ook zonder jouw bemoeienis op. Geen ergernis.";
        } else if (manager == null) {
            status = "Hij speelt op jouw verzoek. Er is geen manager die zich eraan ergert.";
        } else {
            status = "Hij speelt alleen op jouw verzoek: +" + g.frustrationPerForcedMatch()
                    + " ergernis per wedstrijd. Bij 100 stapt " + manager.name + " op.";
        }
        TextView st = Ui.body(c, status);
        st.setPadding(0, Ui.dp(c, 6), 0, 0);
        card.addView(st);
        card.addView(Ui.secondaryButton(c, "Niet meer voortrekken", v -> {
            g.setFavorite(null);
            refresh();
        }));
        return card;
    }

    /** Balk van groen naar rood die de ergernis van de manager laat zien. */
    private LinearLayout moodBar(Context c, int frustration) {
        LinearLayout bar = Ui.row(c);
        bar.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 4));
        int h = Ui.dp(c, 8);
        int color = frustration >= Game.FRUSTRATION_DANGER ? Ui.BAD
                : frustration >= Game.FRUSTRATION_WARNING ? Ui.ACCENT : Ui.GOOD;
        android.view.View filled = new android.view.View(c);
        filled.setBackground(Ui.rounded(color, Ui.dp(c, 4)));
        android.view.View empty = new android.view.View(c);
        empty.setBackground(Ui.rounded(Ui.LINE, Ui.dp(c, 4)));
        bar.addView(filled, new LinearLayout.LayoutParams(0, h, Math.max(1, frustration)));
        bar.addView(empty, new LinearLayout.LayoutParams(0, h, Math.max(1, Game.FRUSTRATION_RESIGN - frustration)));
        return bar;
    }

    private LinearLayout compareRow(Context c, String label, double ours, double theirs) {
        LinearLayout row = Ui.row(c);
        row.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 3));
        row.addView(Ui.cell(c, label, 1.4f, false, Gravity.START));
        TextView a = Ui.cell(c, num(ours), 1f, true, Gravity.END);
        a.setTextColor(ours >= theirs ? Ui.GOOD : Ui.BAD);
        row.addView(a);
        row.addView(Ui.cell(c, num(theirs), 1f, false, Gravity.END));
        return row;
    }

    private static String adviceFor(TeamRating mine, TeamRating theirs) {
        double diff = mine.overall() - theirs.overall();
        if (diff < -5) return "Zij zijn duidelijk sterker. Verdedigend spelen vergroot je kans op een punt.";
        if (diff > 5) return "Jij bent de favoriet. Aanvallend spelen helpt om de drie punten echt binnen te halen.";
        return "De teams zijn aan elkaar gewaagd.";
    }

    private static String num(double v) {
        return String.valueOf(Math.round(v));
    }
}
