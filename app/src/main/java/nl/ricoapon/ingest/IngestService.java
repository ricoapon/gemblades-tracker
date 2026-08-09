package nl.ricoapon.ingest;

import nl.ricoapon.database.Database;

import java.nio.file.Path;

/**
 * Entry point for the ingest package. Runs the log file tailing and processing on a dedicated
 * background thread: the {@link LogFileTailer} reads the log file, and each line is handed to the
 * {@link LogFileProcessor} which updates the {@link Database}.
 *
 * <p>Call {@link #start()} once to begin tailing and {@link #stop()} to interrupt the thread, for
 * example when the application shuts down.
 */
public class IngestService {
    private final LogFileTailer tailer;
    private Thread thread;

    public IngestService(Path logFilePath, Database database) {
        LogFileProcessor processor = new LogFileProcessor(database);
        this.tailer = new LogFileTailer(logFilePath, processor::process);
    }

    /**
     * Starts tailing the log file on a new daemon thread. Does nothing if already started.
     */
    public synchronized void start() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this::run, "log-file-ingest");
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        try {
            tailer.tail();
        } catch (InterruptedException e) {
            // Interrupted by stop(); exit the thread cleanly.
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stops the background thread by interrupting it. Does nothing if not started.
     */
    public synchronized void stop() {
        if (thread == null) {
            return;
        }
        thread.interrupt();
        thread = null;
    }
}
