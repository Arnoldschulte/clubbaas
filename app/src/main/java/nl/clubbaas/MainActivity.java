package nl.clubbaas;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

import nl.clubbaas.engine.Fmt;
import nl.clubbaas.engine.Game;
import nl.clubbaas.engine.SaveManager;
import nl.clubbaas.ui.ClubScreen;
import nl.clubbaas.ui.CupScreen;
import nl.clubbaas.ui.EuropeScreen;
import nl.clubbaas.ui.FinanceScreen;
import nl.clubbaas.ui.FixturesScreen;
import nl.clubbaas.ui.HomeScreen;
import nl.clubbaas.ui.NewGameScreen;
import nl.clubbaas.ui.NewsScreen;
import nl.clubbaas.ui.Screen;
import nl.clubbaas.ui.SquadScreen;
import nl.clubbaas.ui.StadiumScreen;
import nl.clubbaas.ui.StaffScreen;
import nl.clubbaas.ui.TableScreen;
import nl.clubbaas.ui.TacticsScreen;
import nl.clubbaas.ui.TransferScreen;
import nl.clubbaas.ui.Ui;

/**
 * De enige Activity. Bovenin een kopbalk met clubinfo, in het midden het actieve scherm,
 * onderin de navigatie. Schermen worden in code opgebouwd (geen XML-layouts nodig).
 */
public class MainActivity extends Activity {
    private static final String SAVE_FILE = "clubbaas.sav";

    private Game game;
    private Screen current;

    private LinearLayout header;
    private TextView headerTitle;
    private TextView headerSub;
    private View headerStripe;
    private ScrollView scroll;
    private LinearLayout content;
    private LinearLayout nav;
    private TextView[] tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildSkeleton();
        game = loadGame();
        if (game == null) show(new NewGameScreen(this));
        else show(new HomeScreen(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (game != null && !(current instanceof HomeScreen) && !(current instanceof NewGameScreen)) {
            show(new HomeScreen(this));
        } else {
            super.onBackPressed();
        }
    }

    // ---- Publieke API voor de schermen ------------------------------------------------

    public Game game() {
        return game;
    }

    public void startNewGame(Game g) {
        game = g;
        save();
        show(new HomeScreen(this));
    }

    public void show(Screen screen) {
        current = screen;
        rebuild();
        scroll.scrollTo(0, 0);
    }

    /** Na elke actie: opslaan en het huidige scherm opnieuw opbouwen. */
    public void refresh() {
        save();
        int y = scroll.getScrollY();
        rebuild();
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    public void save() {
        if (game == null) return;
        try {
            SaveManager.save(game, new File(getFilesDir(), SAVE_FILE));
        } catch (Exception e) {
            Ui.toast(this, "Opslaan mislukt: " + e.getMessage());
        }
    }

    public void deleteSave() {
        File f = new File(getFilesDir(), SAVE_FILE);
        if (f.exists() && !f.delete()) Ui.toast(this, "Kon savegame niet verwijderen");
        game = null;
        show(new NewGameScreen(this));
    }

    // ---- Opbouw -----------------------------------------------------------------------

    private Game loadGame() {
        File f = new File(getFilesDir(), SAVE_FILE);
        if (!f.exists()) return null;
        try {
            return SaveManager.load(f);
        } catch (Exception e) {
            Ui.toast(this, "Savegame kon niet worden geladen, start een nieuw spel.");
            return null;
        }
    }

    private void buildSkeleton() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        // Kopbalk
        header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Ui.PRIMARY_DARK);
        int hp = Ui.dp(this, 16);
        header.setPadding(hp, hp, hp, hp);
        headerTitle = Ui.text(this, "", 22, 0xFFFFFFFF, true);
        headerSub = Ui.text(this, "", 13, 0xFFCFE3D4, false);
        headerSub.setPadding(0, Ui.dp(this, 2), 0, 0);
        header.addView(headerTitle);
        header.addView(headerSub);
        root.addView(header);
        headerStripe = new View(this);
        root.addView(headerStripe, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 4)));

        // Inhoud
        scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int cp = Ui.dp(this, 14);
        content.setPadding(cp, cp, cp, cp);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // Navigatie
        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Ui.CARD);
        nav.setElevation(Ui.dp(this, 8));
        String[] labels = {"⚽\nHome", "👥\nSelectie", "💶\nTransfers", "📊\nStand", "☰\nMeer"};
        tabs = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView t = Ui.text(this, labels[i], 12, Ui.MUTED, true);
            t.setGravity(Gravity.CENTER);
            int pv = Ui.dp(this, 8);
            t.setPadding(0, pv, 0, pv);
            t.setOnClickListener(v -> onTab(index));
            nav.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            tabs[i] = t;
        }
        root.addView(nav);

        // Android 15+ tekent standaard achter de statusbalk: ruimte maken met insets
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                header.setPadding(hp + bars.left, hp + bars.top, hp + bars.right, hp);
                nav.setPadding(bars.left, 0, bars.right, bars.bottom);
                return insets;
            });
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
        setContentView(root);
    }

    private void onTab(int index) {
        if (game == null) return;
        switch (index) {
            case 0: show(new HomeScreen(this)); break;
            case 1: show(new SquadScreen(this)); break;
            case 2: show(new TransferScreen(this)); break;
            case 3: show(new TableScreen(this)); break;
            default: showMoreMenu();
        }
    }

    private void showMoreMenu() {
        String[] items = {"🎯  Tactiek", "📅  Wedstrijden", "🏆  Beker", "🌍  " + Game.EUROPE_NAME, "🏟  Stadion",
                "🧑‍💼  Staf", "💰  Financiën", "📰  Nieuws", "🏛  Club & historie"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Meer")
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: show(new TacticsScreen(this)); break;
                        case 1: show(new FixturesScreen(this)); break;
                        case 2: show(new CupScreen(this)); break;
                        case 3: show(new EuropeScreen(this)); break;
                        case 4: show(new StadiumScreen(this)); break;
                        case 5: show(new StaffScreen(this)); break;
                        case 6: show(new FinanceScreen(this)); break;
                        case 7: show(new NewsScreen(this)); break;
                        default: show(new ClubScreen(this));
                    }
                })
                .show();
    }

    private void rebuild() {
        updateHeader();
        content.removeAllViews();
        TextView title = Ui.text(this, current.title(), 24, Ui.TEXT, true);
        title.setPadding(0, 0, 0, Ui.dp(this, 12));
        content.addView(title);
        current.build(content);
    }

    private void updateHeader() {
        boolean playing = game != null;
        nav.setVisibility(playing ? View.VISIBLE : View.GONE);
        if (!playing) {
            headerTitle.setText("Clubbaas");
            headerSub.setText("Bouw je club van amateur tot landskampioen");
            headerStripe.setBackgroundColor(Ui.ACCENT);
            return;
        }
        headerTitle.setText(game.human().name);
        String weekText = game.isSeasonOver() ? "seizoen afgelopen"
                : "week " + game.week() + "/" + Game.WEEKS_PER_SEASON;
        headerSub.setText(Fmt.money(game.human().money) + "  ·  " + game.humanDivision().name
                + "  ·  " + game.seasonLabel() + ", " + weekText);
        headerStripe.setBackgroundColor(game.human().color);

        int active = current instanceof HomeScreen ? 0
                : current instanceof SquadScreen ? 1
                : current instanceof TransferScreen ? 2
                : current instanceof TableScreen ? 3 : 4;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setTextColor(i == active ? Ui.PRIMARY : Ui.MUTED);
        }
    }
}
