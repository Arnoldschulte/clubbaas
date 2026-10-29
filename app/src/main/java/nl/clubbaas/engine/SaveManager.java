package nl.clubbaas.engine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Slaat het spel op met Java-serialisatie. Eerst naar een tijdelijk bestand, daarna hernoemen,
 * zodat een crash tijdens het opslaan je savegame niet kapotmaakt.
 */
public final class SaveManager {
    private SaveManager() {}

    public static void save(Game game, File file) throws IOException {
        File tmp = new File(file.getPath() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(tmp)))) {
            out.writeObject(game);
        }
        if (file.exists() && !file.delete()) throw new IOException("Kan oude save niet verwijderen");
        if (!tmp.renameTo(file)) throw new IOException("Kan save niet hernoemen");
    }

    public static Game load(File file) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            return (Game) in.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Savegame is ongeldig", e);
        }
    }
}
