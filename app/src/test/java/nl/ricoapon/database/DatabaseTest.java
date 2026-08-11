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
import java.util.UUID;

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
    void createsDatabaseFileAndMissingParentDirectories() {
        Path nestedPath = tempDir.resolve("does/not/exist/yet/game.db");
        assertFalse(Files.exists(nestedPath.getParent()), "precondition: parent directory should be absent");

        Database nested = new Database(nestedPath);

        assertTrue(Files.exists(nestedPath), "the database file should be created along with its directories");
        // And it is a working database.
        assertTrue(nested.gameDao().findAll().isEmpty());
    }

    @Test
    void insertsAndReadsBackAGame() {
        // Truncate to millis: SQLite stores what we give it, but comparing at millis avoids any
        // nanosecond surprises and reflects realistic usage.
        Instant startedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Game game = new Game(UUID.randomUUID().toString(), "run-42", false, null, startedAt, null);

        database.gameDao().insert(game);

        Optional<Game> loaded = database.gameDao().findById(game.getId());
        assertTrue(loaded.isPresent());
        assertEquals(game, loaded.get());
        assertFalse(loaded.get().isFinished());
        assertNull(loaded.get().getWon(), "won should round-trip as null while the game is unfinished");
        assertEquals(startedAt, loaded.get().getStartedAt());
        assertNull(loaded.get().getEndedAt(), "endedAt should round-trip as null while in progress");
    }

    @Test
    void updatesAGameToFinished() {
        Game game = new Game(UUID.randomUUID().toString(), "run-1", false, null,
                Instant.now().truncatedTo(ChronoUnit.MILLIS), null);
        database.gameDao().insert(game);

        Instant endedAt = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
        game.setFinished(true);
        game.setWon(true);
        game.setEndedAt(endedAt);
        database.gameDao().update(game);

        Game loaded = database.gameDao().findById(game.getId()).orElseThrow();
        assertTrue(loaded.isFinished());
        assertTrue(loaded.getWon());
        assertEquals(endedAt, loaded.getEndedAt());
    }

    @Test
    void insertsAndReadsBackTurnsInOrder() {
        Game game = new Game(UUID.randomUUID().toString(), "run-7", false, null,
                Instant.now().truncatedTo(ChronoUnit.MILLIS), null);
        database.gameDao().insert(game);

        GameTurn turn2 = new GameTurn(game.getId(), 2, 20, 5, 8, 2, 4, 1, 100, 30, 12);
        GameTurn turn1 = new GameTurn(game.getId(), 1, 10, 3, 4, 1, 2, 0, 50, 10, 10);
        // Insert out of order to prove the query orders by turn_number.
        database.gameTurnDao().insert(turn2);
        database.gameTurnDao().insert(turn1);

        List<GameTurn> turns = database.gameTurnDao().findByGameId(game.getId());
        assertEquals(List.of(turn1, turn2), turns);
        assertEquals(50, turns.get(0).getVotersGained());
        assertEquals(12, turns.get(1).getStartingDeckSize());
    }

    @Test
    void foreignKeyConstraintIsEnforced() {
        GameTurn orphan = new GameTurn("no-such-game", 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        assertThrows(UnableToExecuteStatementException.class,
                () -> database.gameTurnDao().insert(orphan),
                "Inserting a turn for a non-existent game must violate the foreign key");
    }
}
