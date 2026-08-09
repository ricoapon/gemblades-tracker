package nl.ricoapon;

import nl.ricoapon.database.Database;
import nl.ricoapon.gui.ViewerApp;
import nl.ricoapon.ingest.IngestService;

import javax.swing.JOptionPane;

/**
 * Application entry point. Opens the database, starts the log file tracker on a background thread, and
 * launches the Swing viewer. When the viewer window is closed, the tracker is stopped as well.
 */
public final class Main {
    public static void main(String[] args) {
        Database database;
        try {
            database = new Database(Constants.DB_FILE_PATH);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null,
                    "Could not open the database at:\n" + Constants.DB_FILE_PATH
                            + "\n\n" + e.getMessage(),
                    "Gemblades Tracker", JOptionPane.ERROR_MESSAGE);
            return;
        }

        IngestService ingestService = new IngestService(Constants.LOG_FILE_PATH, database);
        ingestService.start();

        ViewerApp.launch(database, ingestService::stop);
    }
}
