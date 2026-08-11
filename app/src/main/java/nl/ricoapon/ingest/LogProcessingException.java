package nl.ricoapon.ingest;

/**
 * Thrown when a log line cannot be processed: it is malformed, has an unexpected type, is missing a
 * required parameter, carries a value that cannot be converted, or arrives in an order that violates
 * the expected game state machine.
 *
 * <p>It is unchecked because {@link LogFileProcessor#process(String)} is invoked from a
 * {@link java.util.function.Consumer} handed to the {@link LogFileTailer}, which cannot declare
 * checked exceptions. Callers that tail a long-running log can catch this specific type to skip a
 * single bad line while letting genuinely unexpected failures (e.g. the database being unavailable)
 * propagate.
 */
public class LogProcessingException extends RuntimeException {
    public LogProcessingException(String message) {
        super(message);
    }

    public LogProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
