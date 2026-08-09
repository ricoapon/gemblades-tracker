package nl.ricoapon.gui;

import java.util.List;

/**
 * Aggregate statistics computed across all stored games, used to populate the Statistics tab of the
 * viewer. Values are derived once by {@link StatisticsService#compute} and then rendered as-is.
 *
 * <p>"Turns to win" is the number of recorded turns of a <em>won</em> game; the best game is the one
 * with the fewest such turns and the worst the one with the most. Per-turn economy figures are
 * averaged over all turns of won games only.
 *
 * @param totalGames         number of games in the database
 * @param inProgress         games not yet finished
 * @param finished           games that have ended
 * @param won                finished games that were won
 * @param lost               finished games that were lost
 * @param winRate            won / finished as a percentage (0 when nothing is finished)
 * @param averageTurnsToWin  mean turns-to-win over won games (0 when there are none)
 * @param bestTurnsToWin     fewest turns-to-win, or 0 when there are no won games
 * @param bestRunId          run id of the best (fewest-turn) won game, or {@code null}
 * @param worstTurnsToWin    most turns-to-win, or 0 when there are no won games
 * @param worstRunId         run id of the worst (most-turn) won game, or {@code null}
 * @param avgMoneySpentPerTurn  average money spent per turn over won games
 * @param avgMoneyGainedPerTurn average money gained per turn over won games
 * @param avgNetMoneyPerTurn    average net (gained - spent) money per turn over won games
 * @param avgNetPowerPerTurn    average net power per turn over won games
 * @param avgNetFamePerTurn     average net fame per turn over won games
 * @param avgNetVotersPerTurn   average net voters per turn over won games
 * @param wonGames           per-won-game turn totals, for the turns-to-win bar chart
 */
public record Statistics(
        int totalGames,
        int inProgress,
        int finished,
        int won,
        int lost,
        double winRate,
        double averageTurnsToWin,
        int bestTurnsToWin,
        String bestRunId,
        int worstTurnsToWin,
        String worstRunId,
        double avgMoneySpentPerTurn,
        double avgMoneyGainedPerTurn,
        double avgNetMoneyPerTurn,
        double avgNetPowerPerTurn,
        double avgNetFamePerTurn,
        double avgNetVotersPerTurn,
        List<WonGameResult> wonGames) {

    /**
     * A single won game's turns-to-win, used to plot the turns-to-win bar chart.
     */
    public record WonGameResult(String runId, int turns) {
    }

    /**
     * Statistics for an empty database (no games).
     */
    public static Statistics empty() {
        return new Statistics(0, 0, 0, 0, 0, 0, 0, 0, null, 0, null,
                0, 0, 0, 0, 0, 0, List.of());
    }
}
