package nl.ricoapon.ingest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Tails the log file that contains the data. Each line will be given to the {@link #lineHandler} to process.
 */
public class LogFileTailer {
    private final Path logFilePath;
    private final Consumer<String> lineHandler;

    public LogFileTailer(Path logFilePath, Consumer<String> lineHandler) {
        this.logFilePath = logFilePath;
        this.lineHandler = lineHandler;
    }

    /**
     * Method that runs forever to tail the logfile. Recommended to run in a separate thread. This method supports
     * interrupting it with {@code thread.interrupt()}.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public void tail() throws InterruptedException {
        try (BufferedReader reader = Files.newBufferedReader(logFilePath, StandardCharsets.UTF_8)) {
            while (true) {
                String line = reader.readLine();

                if (line == null) {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                    continue;
                }

                lineHandler.accept(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to tail log file: " + logFilePath, e);
        }
    }
}
