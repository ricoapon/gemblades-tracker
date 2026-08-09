package nl.ricoapon.database;

import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {
    @TempDir
    Path tempDir;

    private Path dbPath;
    private Database database;

    @BeforeEach
    void setUp() {
        dbPath = tempDir.resolve("test.db");
        database = new Database(dbPath);
    }

    @Test
    void flywayCreatesTheDatabaseAndSchemaHistory() {
        assertTrue(Files.exists(dbPath), "Database file should be created by the Database constructor");

        boolean historyExists = database.jdbi().withHandle(handle -> handle
                .createQuery("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'flyway_schema_history'")
                .mapTo(int.class)
                .one()) == 1;
        assertTrue(historyExists, "Flyway should have created its schema history table");
    }

    @Test
    void insertsAndReadsBackAGame() {
        // Truncate to millis: SQLite stores what we give it, but comparing at millis avoids any
        // nanosecond surprises and reflects realistic usage.
        Instant startedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Game game = Game.start("run-42", startedAt);

        database.gameDao().insert(game);

        Optional<Game> loaded = database.gameDao().findById(game.id());
        assertTrue(loaded.isPresent());
        assertEquals(game, loaded.get());
        assertFalse(loaded.get().finished());
        assertNull(loaded.get().won(), "won should round-trip as null while the game is unfinished");
        assertEquals(startedAt, loaded.get().startedAt());
        assertNull(loaded.get().endedAt(), "endedAt should round-trip as null while in progress");
    }

    @Test
    void updatesAGameToFinished() {
        Game game = Game.start("run-1", Instant.now().truncatedTo(ChronoUnit.MILLIS));
        database.gameDao().insert(game);

        Instant endedAt = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
        database.gameDao().update(game.finish(true, endedAt));

        Game loaded = database.gameDao().findById(game.id()).orElseThrow();
        assertTrue(loaded.finished());
        assertTrue(loaded.won());
        assertEquals(endedAt, loaded.endedAt());
    }

    @Test
    void insertsAndReadsBackTurnsInOrder() {
        Game game = Game.start("run-7", Instant.now().truncatedTo(ChronoUnit.MILLIS));
        database.gameDao().insert(game);

        GameTurn turn2 = new GameTurn(game.id(), 2, 20, 5, 8, 2, 4, 1, 100, 30, 12);
        GameTurn turn1 = new GameTurn(game.id(), 1, 10, 3, 4, 1, 2, 0, 50, 10, 10);
        // Insert out of order to prove the query orders by turn_number.
        database.gameTurnDao().insert(turn2);
        database.gameTurnDao().insert(turn1);

        List<GameTurn> turns = database.gameTurnDao().findByGameId(game.id());
        assertEquals(List.of(turn1, turn2), turns);
        assertEquals(50, turns.get(0).votersGained());
        assertEquals(12, turns.get(1).startingDeckSize());
    }

    @Test
    void foreignKeyConstraintIsEnforced() {
        GameTurn orphan = new GameTurn("no-such-game", 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        assertThrows(UnableToExecuteStatementException.class,
                () -> database.gameTurnDao().insert(orphan),
                "Inserting a turn for a non-existent game must violate the foreign key");
    }
}
