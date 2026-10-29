package nl.clubbaas.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Highscorelijst over alle spellen heen. Los van de savegame opgeslagen als eenvoudig tekstbestand
 * (één regel per spel, velden gescheiden door tabs), zodat hij een nieuw spel overleeft.
 */
public final class Highscores {
    public static final int MAX_ENTRIES = 20;

    public static final class Entry {
        public final String chairman;
        public final String club;
        public final int seasons;
        public final String bestDivision;
        public final int titles;
        public final int trophies;
        public final int score;
        public final String ending;

        public Entry(String chairman, String club, int seasons, String bestDivision,
                     int titles, int trophies, int score, String ending) {
            this.chairman = chairman;
            this.club = club;
            this.seasons = seasons;
            this.bestDivision = bestDivision;
            this.titles = titles;
            this.trophies = trophies;
            this.score = score;
            this.ending = ending;
        }

        String toLine() {
            return clean(chairman) + "\t" + clean(club) + "\t" + seasons + "\t" + clean(bestDivision) + "\t"
                    + titles + "\t" + trophies + "\t" + score + "\t" + clean(ending);
        }

        static Entry fromLine(String line) {
            String[] f = line.split("\t", -1);
            if (f.length < 8) return null;
            try {
                return new Entry(f[0], f[1], Integer.parseInt(f[2]), f[3], Integer.parseInt(f[4]),
                        Integer.parseInt(f[5]), Integer.parseInt(f[6]), f[7]);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static String clean(String s) {
            return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ');
        }
    }

    private Highscores() {}

    public static Entry entryFor(Game g, String ending) {
        Club me = g.human();
        int best = Game.DIVISIONS;
        int titles = 0, trophies = 0;
        for (SeasonRecord r : me.history) {
            best = Math.min(best, levelOf(r.division));
            if (r.outcome.contains("ampioen")) titles++;
            if (r.cupResult.startsWith("Bekerwinnaar")) trophies++;
            if ("Winnaar!".equals(r.europeResult)) trophies++;
        }
        best = Math.min(best, me.level);
        return new Entry(g.chairmanName(), me.name, me.history.size(), Game.DIVISION_NAMES[best - 1],
                titles, trophies, g.score(), ending);
    }

    static int levelOf(String divisionName) {
        for (int i = 0; i < Game.DIVISION_NAMES.length; i++) {
            if (Game.DIVISION_NAMES[i].equals(divisionName)) return i + 1;
        }
        return Game.DIVISIONS;
    }

    public static List<Entry> load(File file) {
        List<Entry> out = new ArrayList<>();
        if (!file.exists()) return out;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                Entry e = Entry.fromLine(line);
                if (e != null) out.add(e);
            }
        } catch (IOException e) {
            // Een kapotte lijst is geen ramp: begin opnieuw
        }
        sort(out);
        return out;
    }

    /** Voegt een spel toe en bewaart de beste MAX_ENTRIES. */
    public static void add(File file, Entry entry) throws IOException {
        List<Entry> all = load(file);
        all.add(entry);
        sort(all);
        while (all.size() > MAX_ENTRIES) all.remove(all.size() - 1);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            for (Entry e : all) w.write(e.toLine() + "\n");
        }
    }

    private static void sort(List<Entry> list) {
        Collections.sort(list, (a, b) -> b.score - a.score);
    }
}
