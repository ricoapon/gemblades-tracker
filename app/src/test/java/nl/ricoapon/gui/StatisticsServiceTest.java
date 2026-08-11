package nl.ricoapon.gui;

import nl.ricoapon.database.Database;
import nl.ricoapon.database.Game;
import nl.ricoapon.database.GameTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsServiceTest {
    @TempDir
    Path tempDir;

    private Database database;

    @BeforeEach
    void setUp() {
        database = new Database(tempDir.resolve("stats.db"));
    }

    @Test
    void emptyDatabaseYieldsEmptyStatistics() {
        Statistics stats = StatisticsService.compute(database);
        assertEquals(0, stats.totalGames());
        assertEquals(0, stats.won());
        assertEquals(0, stats.winRate());
        assertEquals(0, stats.averageTurnsToWin());
        assertEquals(0, stats.wonGames().size());
    }

    @Test
    void computesAggregatesOverWonLostAndInProgressGames() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");

        // Won game with 2 turns: money spent 3+5=8, gained 10+8=18, net money +10.
        Game won1 = new Game(UUID.randomUUID().toString(), "run-win-1", true, true, base, base.plusSeconds(120));
        database.gameDao().insert(won1);
        database.gameTurnDao().insert(new GameTurn(won1.getId(), 1, 10, 3, 0, 0, 0, 0, 0, 0, 20));
        database.gameTurnDao().insert(new GameTurn(won1.getId(), 2, 8, 5, 0, 0, 0, 0, 0, 0, 20));

        // Won game with 4 turns: each spent 2, gained 4 -> spent 8, gained 16, net money +8.
        Game won2 = new Game(UUID.randomUUID().toString(), "run-win-2", true, true, base.plusSeconds(1), base.plusSeconds(300));
        database.gameDao().insert(won2);
        for (int turn = 1; turn <= 4; turn++) {
            database.gameTurnDao().insert(new GameTurn(won2.getId(), turn, 4, 2, 0, 0, 0, 0, 0, 0, 20));
        }

        // Lost game (finished, not won) with a turn — must not affect economy or turns-to-win.
        Game lost = new Game(UUID.randomUUID().toString(), "run-lose", true, false, base.plusSeconds(2), base.plusSeconds(60));
        database.gameDao().insert(lost);
        database.gameTurnDao().insert(new GameTurn(lost.getId(), 1, 99, 99, 0, 0, 0, 0, 0, 0, 20));

        // In-progress game (not finished) with no turns.
        database.gameDao().insert(new Game(UUID.randomUUID().toString(), "run-open", false, null, base.plusSeconds(3), null));

        Statistics stats = StatisticsService.compute(database);

        assertEquals(4, stats.totalGames());
        assertEquals(1, stats.inProgress());
        assertEquals(3, stats.finished());
        assertEquals(2, stats.won());
        assertEquals(1, stats.lost());
        assertEquals(100.0 * 2 / 3, stats.winRate(), 1e-9);

        // Turns to win: (2 + 4) / 2 = 3.0; best is the 2-turn game, worst the 4-turn game.
        assertEquals(3.0, stats.averageTurnsToWin(), 1e-9);
        assertEquals(2, stats.bestTurnsToWin());
        assertEquals("run-win-1", stats.bestRunId());
        assertEquals(4, stats.worstTurnsToWin());
        assertEquals("run-win-2", stats.worstRunId());
        assertEquals(2, stats.wonGames().size());

        // Economy over the 6 won-game turns only.
        assertEquals(16.0 / 6, stats.avgMoneySpentPerTurn(), 1e-9);
        assertEquals(34.0 / 6, stats.avgMoneyGainedPerTurn(), 1e-9);
        assertEquals(18.0 / 6, stats.avgNetMoneyPerTurn(), 1e-9);
        assertEquals(0.0, stats.avgNetPowerPerTurn(), 1e-9);
    }
}
