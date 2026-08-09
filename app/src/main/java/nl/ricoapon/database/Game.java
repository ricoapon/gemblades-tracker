package nl.ricoapon.database;

import java.time.Instant;
import java.util.UUID;

/**
 * A single play-through of a run. A game is created when a run starts and marked
 * {@link #finished()} (with {@link #won()}) once it ends.
 *
 * @param id        randomly generated identifier
 * @param runId     the run this game belongs to
 * @param finished  whether the game has ended
 * @param won       whether the game was won, or {@code null} while the game is not yet finished
 * @param startedAt when the game started
 * @param endedAt   when the game ended, or {@code null} while still in progress
 */
public record Game(String id, String runId, boolean finished, Boolean won, Instant startedAt, Instant endedAt) {
    /**
     * Creates a new, unfinished game with a freshly generated id. The outcome ({@link #won()}) is
     * left {@code null} until the game is finished.
     */
    public static Game start(String runId, Instant startedAt) {
        return new Game(UUID.randomUUID().toString(), runId, false, null, startedAt, null);
    }

    /**
     * Returns a copy of this game marked as finished with the given outcome and end time.
     */
    public Game finish(boolean won, Instant endedAt) {
        return new Game(id, runId, true, won, startedAt, endedAt);
    }
}
