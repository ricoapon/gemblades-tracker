package nl.ricoapon.ingest;

import nl.ricoapon.database.Database;
import nl.ricoapon.database.Game;
import nl.ricoapon.database.GameTurn;
import nl.ricoapon.ingest.LogFileProcessor.GameEnd;
import nl.ricoapon.ingest.LogFileProcessor.GameStarted;
import nl.ricoapon.ingest.LogFileProcessor.LogLine;
import nl.ricoapon.ingest.LogFileProcessor.ResourcesChanged;
import nl.ricoapon.ingest.LogFileProcessor.TrackerLoaded;
import nl.ricoapon.ingest.LogFileProcessor.TurnStarted;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogFileProcessorTest {

    /** Builds a log line in the exact format the tracker plugin emits. */
    private static String logLine(String timestamp, String type, String params) {
        return "[Info   :Gemblades Tracker] [" + timestamp + "] [" + type + "] " + params;
    }

    @Nested
    class Parsing {
        @Test
        void parsesEachLineTypeIntoItsTypedRecord() {
            assertInstanceOf(TrackerLoaded.class,
                    LogLine.parse(logLine("2026-08-08T22:11:51Z", "TrackerLoaded", "GameVersion=1 RunID=abc")));
            assertInstanceOf(GameStarted.class,
                    LogLine.parse(logLine("2026-08-08T22:11:52Z", "GameStarted", "Difficulty=1 Length=30 RequiredVoters=120")));
            assertInstanceOf(TurnStarted.class,
                    LogLine.parse(logLine("2026-08-08T22:11:53Z", "TurnStarted", "Turn=1 DeckSize=12")));
            assertInstanceOf(ResourcesChanged.class,
                    LogLine.parse(logLine("2026-08-08T22:11:54Z", "ResourcesChanged", "Money=1 Power=0 Fame=0 Voters=0")));
            assertInstanceOf(GameEnd.class,
                    LogLine.parse(logLine("2026-08-08T22:11:55Z", "GameEnd", "Won=true Turns=13")));
        }

        @Test
        void populatesTrackerLoadedFields() {
            LogLine parsed = LogLine.parse(logLine(
                    "2026-08-08T22:11:51.8407274Z", "TrackerLoaded",
                    "GameVersion=1 RunID=79467be7-476b-4ba7-9d08-d66c1eee54c6"));

            TrackerLoaded trackerLoaded = assertInstanceOf(TrackerLoaded.class, parsed);
            assertEquals(Instant.parse("2026-08-08T22:11:51.8407274Z"), trackerLoaded.timestamp());
            assertEquals(1, trackerLoaded.gameVersion());
            assertEquals("79467be7-476b-4ba7-9d08-d66c1eee54c6", trackerLoaded.runId());
        }

        @Test
        void populatesResourcesChangedFields() {
            ResourcesChanged resources = assertInstanceOf(ResourcesChanged.class, LogLine.parse(
                    logLine("2026-08-08T22:12:24Z", "ResourcesChanged", "Money=5 Power=4 Fame=3 Voters=2")));
            assertEquals(5, resources.money());
            assertEquals(4, resources.power());
            assertEquals(3, resources.fame());
            assertEquals(2, resources.voters());
        }

        @Test
        void populatesGameEndFields() {
            GameEnd lost = assertInstanceOf(GameEnd.class, LogLine.parse(
                    logLine("2026-08-08T22:12:24Z", "GameEnd", "Won=false Turns=7")));
            assertFalse(lost.won());
            assertEquals(7, lost.turns());
        }

        @Test
        void throwsOnLineThatDoesNotMatchTheFormat() {
            assertThrows(LogProcessingException.class, () -> LogLine.parse("this is not a tracker line"));
        }

        @Test
        void throwsOnUnknownLineType() {
            assertThrows(LogProcessingException.class, () -> LogLine.parse(
                    logLine("2026-08-08T22:11:51Z", "SomethingElse", "Foo=1")));
        }

        @Test
        void throwsWhenARequiredKeyIsMissing() {
            // TurnStarted requires DeckSize.
            assertThrows(LogProcessingException.class, () -> LogLine.parse(
                    logLine("2026-08-08T22:11:51Z", "TurnStarted", "Turn=1")));
        }

        @Test
        void throwsWhenAnIntegerValueCannotBeParsed() {
            assertThrows(LogProcessingException.class, () -> LogLine.parse(
                    logLine("2026-08-08T22:11:51Z", "TurnStarted", "Turn=one DeckSize=12")));
        }

        @Test
        void throwsWhenABooleanValueIsNotTrueOrFalse() {
            assertThrows(LogProcessingException.class, () -> LogLine.parse(
                    logLine("2026-08-08T22:11:51Z", "GameEnd", "Won=maybe Turns=3")));
        }

        @Test
        void throwsOnAnInvalidTimestamp() {
            assertThrows(LogProcessingException.class, () -> LogLine.parse(
                    logLine("not-a-timestamp", "TrackerLoaded", "GameVersion=1 RunID=abc")));
        }

        @Test
        void throwsWhenThereAreNoParameters() {
            assertThrows(LogProcessingException.class, () -> LogLine.parse(
                    logLine("2026-08-08T22:11:51Z", "TrackerLoaded", "")));
        }
    }

    @Nested
    class Processing {
        @TempDir
        Path tempDir;

        private Database database;
        private LogFileProcessor processor;

        @BeforeEach
        void setUp() {
            database = new Database(tempDir.resolve("test.db"));
            processor = new LogFileProcessor(database);
        }

        private void feed(String... lines) {
            for (String line : lines) {
                processor.process(line);
            }
        }

        private String onlyGameId() {
            List<Game> games = database.gameDao().findAll();
            assertEquals(1, games.size(), "expected exactly one game to have been created");
            return games.get(0).getId();
        }

        @Test
        void recordsAFullGameFromStartToFinish() {
            feed(
                    logLine("2026-08-08T22:12:00Z", "TrackerLoaded", "GameVersion=1 RunID=run-1"),
                    logLine("2026-08-08T22:12:01Z", "GameStarted", "Difficulty=1 Length=30 RequiredVoters=120"),
                    logLine("2026-08-08T22:12:02Z", "TurnStarted", "Turn=1 DeckSize=12"),
                    logLine("2026-08-08T22:12:03Z", "ResourcesChanged", "Money=5 Power=0 Fame=0 Voters=0"),
                    logLine("2026-08-08T22:12:04Z", "ResourcesChanged", "Money=1 Power=0 Fame=0 Voters=2"),
                    logLine("2026-08-08T22:12:05Z", "TurnStarted", "Turn=2 DeckSize=13"),
                    logLine("2026-08-08T22:12:06Z", "ResourcesChanged", "Money=3 Power=0 Fame=0 Voters=2"),
                    logLine("2026-08-08T22:12:07Z", "GameEnd", "Won=true Turns=2"));

            Game game = database.gameDao().findById(onlyGameId()).orElseThrow();
            assertEquals("run-1", game.getRunId());
            assertTrue(game.isFinished());
            assertTrue(game.getWon());
            assertEquals(Instant.parse("2026-08-08T22:12:01Z"), game.getStartedAt());
            assertEquals(Instant.parse("2026-08-08T22:12:07Z"), game.getEndedAt(), "endedAt should track the last line");
            assertEquals(2, game.getNrOfTurns());

            List<GameTurn> turns = database.gameTurnDao().findByGameId(game.getId());
            assertEquals(2, turns.size());
            assertEquals(12, turns.get(0).getStartingDeckSize());
            assertEquals(13, turns.get(1).getStartingDeckSize());
        }

        @Test
        void splitsResourceChangesIntoGainedAndSpent() {
            feed(
                    logLine("2026-08-08T22:12:00Z", "TrackerLoaded", "GameVersion=1 RunID=run-1"),
                    logLine("2026-08-08T22:12:01Z", "GameStarted", "Difficulty=1 Length=30 RequiredVoters=120"),
                    logLine("2026-08-08T22:12:02Z", "TurnStarted", "Turn=1 DeckSize=12"),
                    // Money climbs to 5 (gained 5), then drops to 2 (spent 3). Power climbs to 3. Voters climb to 4.
                    logLine("2026-08-08T22:12:03Z", "ResourcesChanged", "Money=5 Power=0 Fame=0 Voters=0"),
                    logLine("2026-08-08T22:12:04Z", "ResourcesChanged", "Money=2 Power=3 Fame=0 Voters=0"),
                    logLine("2026-08-08T22:12:05Z", "ResourcesChanged", "Money=2 Power=3 Fame=0 Voters=4"));

            GameTurn turn = database.gameTurnDao().findByGameId(onlyGameId()).get(0);
            assertEquals(5, turn.getMoneyGained());
            assertEquals(3, turn.getMoneySpent());
            assertEquals(3, turn.getPowerGained());
            assertEquals(0, turn.getPowerSpent());
            assertEquals(4, turn.getVotersGained());
            assertEquals(0, turn.getVotersSpent());
        }

        @Test
        void votersCarryAcrossTurnsWhileOtherResourcesResetEachTurn() {
            feed(
                    logLine("2026-08-08T22:12:00Z", "TrackerLoaded", "GameVersion=1 RunID=run-1"),
                    logLine("2026-08-08T22:12:01Z", "GameStarted", "Difficulty=1 Length=30 RequiredVoters=120"),
                    logLine("2026-08-08T22:12:02Z", "TurnStarted", "Turn=1 DeckSize=12"),
                    logLine("2026-08-08T22:12:03Z", "ResourcesChanged", "Money=4 Power=0 Fame=0 Voters=4"),
                    // New turn: money baseline resets to 0, but voters stay at 4.
                    logLine("2026-08-08T22:12:04Z", "TurnStarted", "Turn=2 DeckSize=13"),
                    logLine("2026-08-08T22:12:05Z", "ResourcesChanged", "Money=1 Power=0 Fame=0 Voters=4"),
                    logLine("2026-08-08T22:12:06Z", "ResourcesChanged", "Money=1 Power=0 Fame=0 Voters=7"));

            List<GameTurn> turns = database.gameTurnDao().findByGameId(onlyGameId());
            GameTurn turn2 = turns.get(1);
            // Money is measured from 0 again, so reaching 1 counts as a single gain, not relative to turn 1.
            assertEquals(1, turn2.getMoneyGained());
            // Voters carried the baseline of 4, so only the climb to 7 (a gain of 3) is counted.
            assertEquals(3, turn2.getVotersGained());
            assertEquals(0, turn2.getVotersSpent());
        }

        @Test
        void marksTheGameAsLostWhenGameEndSaysSo() {
            feed(
                    logLine("2026-08-08T22:12:00Z", "TrackerLoaded", "GameVersion=1 RunID=run-1"),
                    logLine("2026-08-08T22:12:01Z", "GameStarted", "Difficulty=1 Length=30 RequiredVoters=120"),
                    logLine("2026-08-08T22:12:02Z", "TurnStarted", "Turn=1 DeckSize=12"),
                    logLine("2026-08-08T22:12:03Z", "GameEnd", "Won=false Turns=1"));

            Game game = database.gameDao().findById(onlyGameId()).orElseThrow();
            assertTrue(game.isFinished());
            assertFalse(game.getWon());
        }

        @Test
        void ignoresBlankLines() {
            assertDoesNotThrow(() -> feed("", "   "));
            assertTrue(database.gameDao().findAll().isEmpty());
        }

        @Test
        void throwsWhenATurnStartsBeforeAnyGame() {
            assertThrows(LogProcessingException.class, () -> feed(
                    logLine("2026-08-08T22:12:00Z", "TurnStarted", "Turn=1 DeckSize=12")));
        }

        @Test
        void throwsWhenResourcesChangeBeforeAnyTurn() {
            assertThrows(LogProcessingException.class, () -> feed(
                    logLine("2026-08-08T22:12:00Z", "TrackerLoaded", "GameVersion=1 RunID=run-1"),
                    logLine("2026-08-08T22:12:01Z", "GameStarted", "Difficulty=1 Length=30 RequiredVoters=120"),
                    logLine("2026-08-08T22:12:02Z", "ResourcesChanged", "Money=1 Power=0 Fame=0 Voters=0")));
        }

        @Test
        void throwsWhenASecondTrackerLoadedIsSeen() {
            assertThrows(LogProcessingException.class, () -> feed(
                    logLine("2026-08-08T22:12:00Z", "TrackerLoaded", "GameVersion=1 RunID=run-1"),
                    logLine("2026-08-08T22:12:01Z", "TrackerLoaded", "GameVersion=1 RunID=run-2")));
        }

        @Test
        void aFailingLineIsRolledBackAndLeavesTheDatabaseUntouched() {
            feed(
                    logLine("2026-08-08T22:12:00Z", "TrackerLoaded", "GameVersion=1 RunID=run-1"),
                    logLine("2026-08-08T22:12:01Z", "GameStarted", "Difficulty=1 Length=30 RequiredVoters=120"),
                    logLine("2026-08-08T22:12:02Z", "TurnStarted", "Turn=1 DeckSize=12"),
                    logLine("2026-08-08T22:12:03Z", "ResourcesChanged", "Money=3 Power=0 Fame=0 Voters=0"));

            String gameId = onlyGameId();
            Instant committedEndedAt = database.gameDao().findById(gameId).orElseThrow().getEndedAt();

            // Re-declaring turn 1 violates the (game_id, turn_number) primary key, so the whole line fails.
            assertThrows(UnableToExecuteStatementException.class, () -> processor.process(
                    logLine("2026-08-08T22:12:09Z", "TurnStarted", "Turn=1 DeckSize=99")));

            // Nothing from the failed line is persisted: still one turn with its original totals, and the
            // game's endedAt was not advanced to the failed line.
            List<GameTurn> turns = database.gameTurnDao().findByGameId(gameId);
            assertEquals(1, turns.size());
            assertEquals(3, turns.get(0).getMoneyGained());
            assertEquals(12, turns.get(0).getStartingDeckSize());
            assertEquals(committedEndedAt, database.gameDao().findById(gameId).orElseThrow().getEndedAt());
        }
    }
}
