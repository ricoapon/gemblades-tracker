package nl.ricoapon;

import java.nio.file.Path;

public class Constants {
    public final static Path LOG_FILE_PATH = Path.of("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Gemblades\\BepInEx\\LogOutput.log");

    /**
     * Location of the local SQLite database, kept in a dedicated folder under the user's home
     * directory (not in the game install). The tracker writes runs here and the Swing viewer reads
     * from the same file.
     */
    public final static Path DB_FILE_PATH =
            Path.of(System.getProperty("user.home"), ".gemblades-tracker", "gemblades-tracker.db");
}
