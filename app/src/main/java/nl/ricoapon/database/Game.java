package nl.ricoapon.database;

import java.time.Instant;
import java.util.Objects;

/**
 * A single play-through of a run. A game is created when a run starts and marked
 * {@link #isFinished()} (with a {@link #getWon()} outcome) once it ends.
 *
 * <p>This is a mutable persistence entity: load it, change fields, and save it back. Equality is
 * value based over the stored fields, so avoid using instances as keys in hash-based collections
 * while they are still being mutated.
 */
public class Game {
    private String id;
    private String runId;
    private boolean finished;
    /** Whether the game was won, or {@code null} while the game is not yet finished. */
    private Boolean won;
    private Instant startedAt;
    private Instant endedAt;
    /**
     * The number of turns recorded for this game. This is not a stored column: it is derived from
     * {@code game_turn} by the DAO when a game is read, and is 0 on a freshly constructed game. It
     * is therefore excluded from {@link #equals(Object)} / {@link #hashCode()}.
     */
    private int nrOfTurns;

    /** No-arg constructor required for JDBI bean mapping. */
    public Game() {
    }

    public Game(String id, String runId, boolean finished, Boolean won, Instant startedAt, Instant endedAt) {
        this.id = id;
        this.runId = runId;
        this.finished = finished;
        this.won = won;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Boolean getWon() {
        return won;
    }

    public void setWon(Boolean won) {
        this.won = won;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public int getNrOfTurns() {
        return nrOfTurns;
    }

    public void setNrOfTurns(int nrOfTurns) {
        this.nrOfTurns = nrOfTurns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Game other)) {
            return false;
        }
        return finished == other.finished
                && Objects.equals(id, other.id)
                && Objects.equals(runId, other.runId)
                && Objects.equals(won, other.won)
                && Objects.equals(startedAt, other.startedAt)
                && Objects.equals(endedAt, other.endedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, runId, finished, won, startedAt, endedAt);
    }

    @Override
    public String toString() {
        return "Game{id='" + id + "', runId='" + runId + "', finished=" + finished
                + ", won=" + won + ", startedAt=" + startedAt + ", endedAt=" + endedAt
                + ", nrOfTurns=" + nrOfTurns + '}';
    }
}
