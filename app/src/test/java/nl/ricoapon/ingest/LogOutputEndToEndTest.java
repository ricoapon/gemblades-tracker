package nl.ricoapon.ingest;

import nl.ricoapon.database.Database;
import nl.ricoapon.database.Game;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test: feeds the complete {@code LogOutput.log} captured from the tracker plugin through
 * {@link LogFileProcessor}, line for line, and asserts the resulting database state. This guards
 * against regressions where whole lines are skipped or games are miscategorised, and exercises every
 * line type against a realistic recording.
 *
 * <p>The recording contains one run in which four games are started (three Gauntlet, one normal);
 * only the last game is played to completion.
 */
class LogOutputEndToEndTest {
    private static final String RUN_ID = "4b2a63ed-faa8-449a-ae3b-b3c1191e7ca4";

    @TempDir
    Path tempDir;

    @Test
    void replaysTheWholeLogAndRecordsEveryGame() {
        Database database = new Database(tempDir.resolve("e2e.db"));
        LogFileProcessor processor = new LogFileProcessor(database);

        for (String line : readLogLines()) {
            processor.process(line);
        }

        List<Game> games = database.gameDao().findAll();
        assertEquals(4, games.size(), "each GameStarted line should have created a game");
        assertTrue(games.stream().allMatch(g -> RUN_ID.equals(g.getRunId())), "all games share the one run id");

        assertEquals(3, games.stream().filter(Game::isGauntlet).count(), "three Gauntlet games");
        assertEquals(1, games.stream().filter(g -> !g.isGauntlet()).count(), "one normal game");
        assertEquals(26, games.stream().mapToInt(Game::getNrOfTurns).sum(), "1 + 1 + 12 + 12 turns recorded");

        // findAll is ordered by started_at, so the games come back in the order they were started.
        Game gauntletShort = games.getFirst();
        assertTrue(gauntletShort.isGauntlet());
        assertEquals(1, gauntletShort.getDifficulty());
        assertEquals(12, gauntletShort.getLength());
        assertEquals(40, gauntletShort.getRequiredVoters());
        assertEquals(1, gauntletShort.getNrOfTurns());
        assertFalse(gauntletShort.isFinished());

        Game normal = games.get(1);
        assertFalse(normal.isGauntlet());
        assertEquals(5, normal.getDifficulty());
        assertEquals(30, normal.getLength());
        assertEquals(250, normal.getRequiredVoters());
        assertEquals(1, normal.getNrOfTurns());
        assertFalse(normal.isFinished());

        Game gauntletAbandoned = games.get(2);
        assertTrue(gauntletAbandoned.isGauntlet());
        assertEquals(12, gauntletAbandoned.getNrOfTurns());
        assertFalse(gauntletAbandoned.isFinished(), "this game was replaced before it ended");

        // Only the last game runs to completion, and it was lost on turn 12.
        Game finished = games.get(3);
        assertTrue(finished.isFinished());
        assertFalse(finished.getWon());
        assertTrue(finished.isGauntlet());
        assertEquals(2, finished.getDifficulty());
        assertEquals(12, finished.getLength());
        assertEquals(50, finished.getRequiredVoters());
        assertEquals(12, finished.getNrOfTurns());
        assertEquals(Instant.parse("2026-08-11T20:26:22.7395675Z"), finished.getStartedAt());
        assertEquals(Instant.parse("2026-08-11T20:28:21.2856045Z"), finished.getEndedAt(),
                "endedAt should track the last processed line");

        assertEquals(1, games.stream().filter(Game::isFinished).count(), "only one game was finished");
        assertNull(games.getFirst().getWon(), "unfinished games have no outcome");
    }

    private List<String> readLogLines() {
        try (InputStream in = Objects.requireNonNull(
                getClass().getResourceAsStream("/LogOutput.log"), "LogOutput.log test resource is missing");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
