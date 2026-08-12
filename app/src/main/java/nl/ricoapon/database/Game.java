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
    /** The chosen difficulty of the run, as reported by the game. */
    private int difficulty;
    /** The run length (number of turns the run is scheduled to last), as reported by the game. */
    private int length;
    /** The number of voters required to win the run. */
    private int requiredVoters;
    /** Whether this run is a Gauntlet run, which is scored and displayed separately. */
    private boolean gauntlet;
    /**
     * The number of turns recorded for this game. This is not a stored column: it is derived from
     * {@code game_turn} by the DAO when a game is read, and is 0 on a freshly constructed game. It
     * is therefore excluded from {@link #equals(Object)} / {@link #hashCode()}.
     */
    private int nrOfTurns;

    /** No-arg constructor required for JDBI bean mapping. */
    public Game() {
    }

    /**
     * Convenience constructor for a game whose run parameters are not known, leaving
     * {@code difficulty}, {@code length} and {@code requiredVoters} at 0 and {@code gauntlet} false.
     */
    public Game(String id, String runId, boolean finished, Boolean won, Instant startedAt, Instant endedAt) {
        this(id, runId, finished, won, startedAt, endedAt, 0, 0, 0, false);
    }

    public Game(String id, String runId, boolean finished, Boolean won, Instant startedAt, Instant endedAt,
                int difficulty, int length, int requiredVoters, boolean gauntlet) {
        this.id = id;
        this.runId = runId;
        this.finished = finished;
        this.won = won;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.difficulty = difficulty;
        this.length = length;
        this.requiredVoters = requiredVoters;
        this.gauntlet = gauntlet;
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

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getRequiredVoters() {
        return requiredVoters;
    }

    public void setRequiredVoters(int requiredVoters) {
        this.requiredVoters = requiredVoters;
    }

    public boolean isGauntlet() {
        return gauntlet;
    }

    public void setGauntlet(boolean gauntlet) {
        this.gauntlet = gauntlet;
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
                && difficulty == other.difficulty
                && length == other.length
                && requiredVoters == other.requiredVoters
                && gauntlet == other.gauntlet
                && Objects.equals(id, other.id)
                && Objects.equals(runId, other.runId)
                && Objects.equals(won, other.won)
                && Objects.equals(startedAt, other.startedAt)
                && Objects.equals(endedAt, other.endedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, runId, finished, won, startedAt, endedAt,
                difficulty, length, requiredVoters, gauntlet);
    }

    @Override
    public String toString() {
        return "Game{id='" + id + "', runId='" + runId + "', finished=" + finished
                + ", won=" + won + ", startedAt=" + startedAt + ", endedAt=" + endedAt
                + ", difficulty=" + difficulty + ", length=" + length
                + ", requiredVoters=" + requiredVoters + ", gauntlet=" + gauntlet
                + ", nrOfTurns=" + nrOfTurns + '}';
    }
}
