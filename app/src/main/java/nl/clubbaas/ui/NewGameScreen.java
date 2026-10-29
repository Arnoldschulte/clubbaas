package nl.clubbaas.ui;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.Game;

/** Startscherm voor een nieuw spel: naam voorzitter, clubnaam en clubkleur. */
public class NewGameScreen extends Screen {
    private int selectedColor = Game.PALETTE[0];

    public NewGameScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Nieuw spel";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        LinearLayout intro = Ui.card(c, "Het idee");
        intro.addView(Ui.body(c, "Je bent voorzitter van een piepkleine amateurclub in de laagste van "
                + "zeven divisies. Koop spelers, bouw je stadion uit, stel staf aan en houd de "
                + "financiën gezond. Kun jij je club naar de Topdivisie leiden?"));
        root.addView(intro);

        LinearLayout form = Ui.card(c, "Jouw club");
        form.addView(Ui.muted(c, "Naam voorzitter"));
        EditText chairman = input(c, "Bijv. Arnold");
        form.addView(chairman);
        TextView clubLabel = Ui.muted(c, "Clubnaam");
        clubLabel.setPadding(0, Ui.dp(c, 12), 0, 0);
        form.addView(clubLabel);
        EditText club = input(c, "Bijv. VV De Toekomst");
        form.addView(club);

        TextView colorLabel = Ui.muted(c, "Clubkleur");
        colorLabel.setPadding(0, Ui.dp(c, 12), 0, Ui.dp(c, 6));
        form.addView(colorLabel);
        LinearLayout colors = Ui.row(c);
        TextView[] swatches = new TextView[6];
        for (int i = 0; i < swatches.length; i++) {
            final int color = Game.PALETTE[i];
            TextView s = new TextView(c);
            int size = Ui.dp(c, 36);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(0, 0, Ui.dp(c, 8), 0);
            s.setLayoutParams(lp);
            s.setOnClickListener(v -> {
                selectedColor = color;
                paintSwatches(c, swatches);
            });
            swatches[i] = s;
            colors.addView(s);
        }
        paintSwatches(c, swatches);
        form.addView(colors);
        root.addView(form);

        root.addView(Ui.button(c, "Start je carrière", v -> {
            String name = chairman.getText().toString().trim();
            String clubName = club.getText().toString().trim();
            if (name.isEmpty()) name = "Voorzitter";
            if (clubName.isEmpty()) clubName = "VV De Toekomst";
            int year = Calendar.getInstance().get(Calendar.YEAR);
            act.startNewGame(new Game(name, clubName, selectedColor, year, System.nanoTime()));
        }));
    }

    private void paintSwatches(Context c, TextView[] swatches) {
        for (int i = 0; i < swatches.length; i++) {
            int color = Game.PALETTE[i];
            android.graphics.drawable.GradientDrawable d = Ui.rounded(color, Ui.dp(c, 18));
            if (color == selectedColor) d.setStroke(Ui.dp(c, 3), Ui.ACCENT);
            swatches[i].setBackground(d);
        }
    }

    private EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        e.setTextColor(Ui.TEXT);
        return e;
    }
}
