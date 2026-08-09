package nl.ricoapon;

import java.nio.file.Path;

public class Constants {
    private final static Path LOG_FILE_PATH = Path.of("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Gemblades\\BepInEx\\LogOutput.log");

    /**
     * Location of the local SQLite database, alongside the tailed log file. The tracker writes runs
     * here and the Swing viewer reads from the same file.
     */
    public final static Path DB_FILE_PATH = Path.of("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Gemblades\\BepInEx\\gemblades-tracker.db");
}
