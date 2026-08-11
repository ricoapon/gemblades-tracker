package nl.ricoapon.gui;

import nl.ricoapon.database.Database;
import nl.ricoapon.database.Game;
import nl.ricoapon.database.GameTurn;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes aggregate {@link Statistics} from the games and turns stored in a {@link Database}.
 *
 * <p>The data volume is small (one player's runs), so this reads every game and its turns through
 * the existing DAOs and aggregates in plain Java rather than pushing the work into SQL.
 */
public final class StatisticsService {
    private StatisticsService() {
    }

    /**
     * Reads all games and their turns and returns the derived statistics. Returns
     * {@link Statistics#empty()} when there are no games.
     */
    public static Statistics compute(Database database) {
        List<Game> games = database.gameDao().findAll();
        if (games.isEmpty()) {
            return Statistics.empty();
        }

        int inProgress = 0;
        int finished = 0;
        int won = 0;

        // Accumulators over won games only.
        List<Statistics.WonGameResult> wonGames = new ArrayList<>();
        long totalTurns = 0;
        long sumMoneySpent = 0;
        long sumMoneyGained = 0;
        long sumNetMoney = 0;
        long sumNetPower = 0;
        long sumNetFame = 0;
        long sumNetVoters = 0;

        for (Game game : games) {
            if (!game.isFinished()) {
                inProgress++;
                continue;
            }
            finished++;
            if (!Boolean.TRUE.equals(game.getWon())) {
                continue;
            }
            won++;

            List<GameTurn> turns = database.gameTurnDao().findByGameId(game.getId());
            wonGames.add(new Statistics.WonGameResult(game.getRunId(), turns.size()));
            totalTurns += turns.size();
            for (GameTurn turn : turns) {
                sumMoneySpent += turn.getMoneySpent();
                sumMoneyGained += turn.getMoneyGained();
                sumNetMoney += turn.getMoneyGained() - turn.getMoneySpent();
                sumNetPower += turn.getPowerGained() - turn.getPowerSpent();
                sumNetFame += turn.getFameGained() - turn.getFameSpent();
                sumNetVoters += turn.getVotersGained() - turn.getVotersSpent();
            }
        }

        int lost = finished - won;
        double winRate = finished == 0 ? 0 : 100.0 * won / finished;

        double averageTurnsToWin = 0;
        int bestTurnsToWin = 0;
        String bestRunId = null;
        int worstTurnsToWin = 0;
        String worstRunId = null;
        if (!wonGames.isEmpty()) {
            averageTurnsToWin = (double) totalTurns / wonGames.size();
            Statistics.WonGameResult best = wonGames.getFirst();
            Statistics.WonGameResult worst = wonGames.getFirst();
            for (Statistics.WonGameResult result : wonGames) {
                if (result.turns() < best.turns()) {
                    best = result;
                }
                if (result.turns() > worst.turns()) {
                    worst = result;
                }
            }
            bestTurnsToWin = best.turns();
            bestRunId = best.runId();
            worstTurnsToWin = worst.turns();
            worstRunId = worst.runId();
        }

        // Guard against division by zero when won games recorded no turns.
        double avgMoneySpentPerTurn = totalTurns == 0 ? 0 : (double) sumMoneySpent / totalTurns;
        double avgMoneyGainedPerTurn = totalTurns == 0 ? 0 : (double) sumMoneyGained / totalTurns;
        double avgNetMoneyPerTurn = totalTurns == 0 ? 0 : (double) sumNetMoney / totalTurns;
        double avgNetPowerPerTurn = totalTurns == 0 ? 0 : (double) sumNetPower / totalTurns;
        double avgNetFamePerTurn = totalTurns == 0 ? 0 : (double) sumNetFame / totalTurns;
        double avgNetVotersPerTurn = totalTurns == 0 ? 0 : (double) sumNetVoters / totalTurns;

        return new Statistics(
                games.size(), inProgress, finished, won, lost, winRate,
                averageTurnsToWin, bestTurnsToWin, bestRunId, worstTurnsToWin, worstRunId,
                avgMoneySpentPerTurn, avgMoneyGainedPerTurn, avgNetMoneyPerTurn,
                avgNetPowerPerTurn, avgNetFamePerTurn, avgNetVotersPerTurn,
                wonGames);
    }
}
