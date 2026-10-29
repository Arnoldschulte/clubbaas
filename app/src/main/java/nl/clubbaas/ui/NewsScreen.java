package nl.clubbaas.ui;

import android.content.Context;
import android.widget.LinearLayout;

import nl.clubbaas.MainActivity;
import nl.clubbaas.engine.NewsItem;

public class NewsScreen extends Screen {

    public NewsScreen(MainActivity act) {
        super(act);
    }

    @Override
    public String title() {
        return "Nieuws";
    }

    @Override
    public void build(LinearLayout root) {
        Context c = ctx();
        LinearLayout card = Ui.card(c);
        boolean first = true;
        for (NewsItem n : game().news()) {
            if (!first) card.addView(Ui.divider(c));
            first = false;
            LinearLayout item = new LinearLayout(c);
            item.setOrientation(LinearLayout.VERTICAL);
            int pv = Ui.dp(c, 8);
            item.setPadding(0, pv, 0, pv);
            item.addView(Ui.muted(c, n.season + (n.week > 0 ? ", week " + n.week : ", voorbereiding")));
            item.addView(Ui.body(c, n.text));
            card.addView(item);
        }
        if (first) card.addView(Ui.muted(c, "Nog geen nieuws."));
        root.addView(card);
    }
}
