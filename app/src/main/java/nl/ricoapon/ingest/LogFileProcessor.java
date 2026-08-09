package nl.ricoapon.ingest;

import nl.ricoapon.database.Database;

/**
 * Processes the content of the log file line by line and updates the database accordingly.
 *
 * <p>Each line read by the {@link LogFileTailer} is handed to {@link #process(String)}. The actual
 * parsing and database updates are implemented separately.
 */
public class LogFileProcessor {
    private final Database database;

    public LogFileProcessor(Database database) {
        this.database = database;
    }

    /**
     * Processes a single line from the log file.
     *
     * @param line one line of the log file
     */
    public void process(String line) {
        // To be implemented.
    }
}
