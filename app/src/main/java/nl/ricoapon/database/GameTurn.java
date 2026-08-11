package nl.ricoapon.database;

import java.util.Objects;

/**
 * A single turn within a {@link Game}. Turns are ordered by {@link #getTurnNumber()} and are unique
 * per game, so {@code (gameId, turnNumber)} together form the identity of a turn.
 *
 * <p>This is a mutable persistence entity: the gained/spent totals are meant to be accumulated as
 * the turn is parsed, then saved. Equality is value based over all fields, so avoid using instances
 * as keys in hash-based collections while they are still being mutated.
 */
public class GameTurn {
    private String gameId;
    private int turnNumber;
    private int moneyGained;
    private int moneySpent;
    private int powerGained;
    private int powerSpent;
    private int fameGained;
    private int fameSpent;
    private int votersGained;
    private int votersSpent;
    private int startingDeckSize;

    /** No-arg constructor required for JDBI bean mapping. */
    public GameTurn() {
    }

    public GameTurn(String gameId, int turnNumber, int moneyGained, int moneySpent, int powerGained,
                    int powerSpent, int fameGained, int fameSpent, int votersGained, int votersSpent,
                    int startingDeckSize) {
        this.gameId = gameId;
        this.turnNumber = turnNumber;
        this.moneyGained = moneyGained;
        this.moneySpent = moneySpent;
        this.powerGained = powerGained;
        this.powerSpent = powerSpent;
        this.fameGained = fameGained;
        this.fameSpent = fameSpent;
        this.votersGained = votersGained;
        this.votersSpent = votersSpent;
        this.startingDeckSize = startingDeckSize;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setTurnNumber(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    public int getMoneyGained() {
        return moneyGained;
    }

    public void setMoneyGained(int moneyGained) {
        this.moneyGained = moneyGained;
    }

    public int getMoneySpent() {
        return moneySpent;
    }

    public void setMoneySpent(int moneySpent) {
        this.moneySpent = moneySpent;
    }

    public int getPowerGained() {
        return powerGained;
    }

    public void setPowerGained(int powerGained) {
        this.powerGained = powerGained;
    }

    public int getPowerSpent() {
        return powerSpent;
    }

    public void setPowerSpent(int powerSpent) {
        this.powerSpent = powerSpent;
    }

    public int getFameGained() {
        return fameGained;
    }

    public void setFameGained(int fameGained) {
        this.fameGained = fameGained;
    }

    public int getFameSpent() {
        return fameSpent;
    }

    public void setFameSpent(int fameSpent) {
        this.fameSpent = fameSpent;
    }

    public int getVotersGained() {
        return votersGained;
    }

    public void setVotersGained(int votersGained) {
        this.votersGained = votersGained;
    }

    public int getVotersSpent() {
        return votersSpent;
    }

    public void setVotersSpent(int votersSpent) {
        this.votersSpent = votersSpent;
    }

    public int getStartingDeckSize() {
        return startingDeckSize;
    }

    public void setStartingDeckSize(int startingDeckSize) {
        this.startingDeckSize = startingDeckSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GameTurn other)) {
            return false;
        }
        return turnNumber == other.turnNumber
                && moneyGained == other.moneyGained
                && moneySpent == other.moneySpent
                && powerGained == other.powerGained
                && powerSpent == other.powerSpent
                && fameGained == other.fameGained
                && fameSpent == other.fameSpent
                && votersGained == other.votersGained
                && votersSpent == other.votersSpent
                && startingDeckSize == other.startingDeckSize
                && Objects.equals(gameId, other.gameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, turnNumber, moneyGained, moneySpent, powerGained, powerSpent,
                fameGained, fameSpent, votersGained, votersSpent, startingDeckSize);
    }

    @Override
    public String toString() {
        return "GameTurn{gameId='" + gameId + "', turnNumber=" + turnNumber
                + ", moneyGained=" + moneyGained + ", moneySpent=" + moneySpent
                + ", powerGained=" + powerGained + ", powerSpent=" + powerSpent
                + ", fameGained=" + fameGained + ", fameSpent=" + fameSpent
                + ", votersGained=" + votersGained + ", votersSpent=" + votersSpent
                + ", startingDeckSize=" + startingDeckSize + '}';
    }
}
