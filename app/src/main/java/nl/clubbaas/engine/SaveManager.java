package nl.clubbaas.engine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Opslaan en laden.
 *
 * Formaat: JSON (zie SaveCodec), gecomprimeerd met gzip. Er wordt eerst naar een tijdelijk bestand
 * geschreven; de vorige save blijft bewaard als back-up (.bak). Lukt laden van de save niet, dan
 * probeert de app de back-up. Oude savegames (Java-serialisatie, van vóór dit formaat) worden
 * herkend en omgezet.
 */
public final class SaveManager {
    private SaveManager() {}

    public static void save(Game game, File file) throws IOException {
        byte[] json = SaveCodec.encode(game).getBytes(StandardCharsets.UTF_8);
        File tmp = new File(file.getPath() + ".tmp");
        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(tmp))) {
            out.write(json);
        }
        File backup = backupOf(file);
        if (file.exists()) {
            if (backup.exists() && !backup.delete()) throw new IOException("Kan oude back-up niet verwijderen");
            if (!file.renameTo(backup)) throw new IOException("Kan back-up niet maken");
        }
        if (!tmp.renameTo(file)) throw new IOException("Kan save niet wegschrijven");
    }

    public static Game load(File file) throws IOException {
        try {
            return loadFile(file);
        } catch (IOException | RuntimeException e) {
            File backup = backupOf(file);
            if (backup.exists()) {
                try {
                    return loadFile(backup);
                } catch (IOException | RuntimeException ignored) {
                    // val terug op de oorspronkelijke fout
                }
            }
            throw e instanceof IOException ? (IOException) e : new IOException("Savegame is ongeldig: " + e.getMessage(), e);
        }
    }

    static File backupOf(File file) {
        return new File(file.getPath() + ".bak");
    }

    private static Game loadFile(File file) throws IOException {
        byte[] data = readAll(new FileInputStream(file));
        if (data.length >= 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
            byte[] json = readAll(new GZIPInputStream(new ByteArrayInputStream(data)));
            return SaveCodec.decode(new String(json, StandardCharsets.UTF_8));
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xAC && (data[1] & 0xFF) == 0xED) {
            return loadLegacy(data);
        }
        if (data.length > 0 && data[0] == '{') {
            return SaveCodec.decode(new String(data, StandardCharsets.UTF_8));
        }
        throw new IOException("Onbekend savegame-formaat");
    }

    /** Savegame uit een oudere versie van de app (Java-serialisatie). */
    private static Game loadLegacy(byte[] data) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            Game g = (Game) in.readObject();
            if (!(g.rng instanceof Rng)) g.rng = new Rng(g.rng.nextLong());
            return g;
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Oude savegame is ongeldig", e);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = input.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }
}
