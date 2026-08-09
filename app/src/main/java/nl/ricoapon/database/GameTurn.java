package nl.ricoapon.database;

/**
 * A single turn within a {@link Game}. Turns are ordered by {@link #turnNumber()} and are unique
 * per game, so {@code (gameId, turnNumber)} together form the identity of a turn.
 *
 * @param gameId           the game this turn belongs to
 * @param turnNumber       the incremental turn ordinal within the game (1-based)
 * @param moneyGained      money gained during the turn
 * @param moneySpent       money spent during the turn
 * @param powerGained      power gained during the turn
 * @param powerSpent       power spent during the turn
 * @param fameGained       fame gained during the turn
 * @param fameSpent        fame spent during the turn
 * @param votersGained     voters gained during the turn
 * @param votersSpent      voters spent during the turn
 * @param startingDeckSize the deck size at the start of the turn
 */
public record GameTurn(
        String gameId,
        int turnNumber,
        int moneyGained,
        int moneySpent,
        int powerGained,
        int powerSpent,
        int fameGained,
        int fameSpent,
        int votersGained,
        int votersSpent,
        int startingDeckSize) {
}
