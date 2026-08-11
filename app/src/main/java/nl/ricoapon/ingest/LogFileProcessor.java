package nl.ricoapon.ingest;

import nl.ricoapon.database.Database;
import nl.ricoapon.database.Game;
import nl.ricoapon.database.GameDao;
import nl.ricoapon.database.GameTurn;
import nl.ricoapon.database.GameTurnDao;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes the content of the log file line by line and updates the database accordingly. Each line read by the
 * {@link LogFileTailer} is handed to {@link #process(String)}. Every line is first parsed into a typed {@link LogLine}
 * and then dispatched to the matching handler.
 */
public class LogFileProcessor {
    private final static Pattern LINE_REGEX = Pattern.compile("\\[Info\\s+:Gemblades Tracker] \\[(.*)] \\[(.*)] (.*)");
    private final static Pattern PARAM_REGEX = Pattern.compile("(\\w+)=([^ ]+)");

    /**
     * A single parsed log line. Each concrete type carries exactly the fields that its kind of line
     * provides, already converted to their proper types. Use {@link #parse(String)} to build one.
     */
    sealed interface LogLine permits TrackerLoaded, GameStarted, TurnStarted, ResourcesChanged, GameEnd {
        Instant timestamp();

        static LogLine parse(String line) {
            Matcher lineMatcher = LINE_REGEX.matcher(line);
            if (!lineMatcher.matches()) {
                throw new LogProcessingException("Line does not match regex: " + line);
            }

            Instant timestamp = parseTimestamp(lineMatcher.group(1), line);
            String type = lineMatcher.group(2);
            Params params = Params.parse(lineMatcher.group(3), line);

            return switch (type) {
                case "TrackerLoaded" -> new TrackerLoaded(timestamp,
                        params.getInt("GameVersion"), params.get("RunID"));
                case "GameStarted" -> new GameStarted(timestamp,
                        params.getInt("Difficulty"), params.getInt("Length"), params.getInt("RequiredVoters"));
                case "TurnStarted" -> new TurnStarted(timestamp,
                        params.getInt("Turn"), params.getInt("DeckSize"));
                case "ResourcesChanged" -> new ResourcesChanged(timestamp,
                        params.getInt("Money"), params.getInt("Power"), params.getInt("Fame"), params.getInt("Voters"));
                case "GameEnd" -> new GameEnd(timestamp,
                        params.getBoolean("Won"), params.getInt("Turns"));
                default -> throw new LogProcessingException("Unknown log line type '" + type + "': " + line);
            };
        }

        private static Instant parseTimestamp(String value, String line) {
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException e) {
                throw new LogProcessingException("Line has an invalid timestamp '" + value + "': " + line, e);
            }
        }
    }

    record TrackerLoaded(Instant timestamp, int gameVersion, String runId) implements LogLine {
    }

    record GameStarted(Instant timestamp, int difficulty, int length, int requiredVoters) implements LogLine {
    }

    record TurnStarted(Instant timestamp, int turn, int deckSize) implements LogLine {
    }

    record ResourcesChanged(Instant timestamp, int money, int power, int fame, int voters) implements LogLine {
    }

    record GameEnd(Instant timestamp, boolean won, int turns) implements LogLine {
    }

    /**
     * The {@code key=value} portion of a log line, with typed accessors that fail when a required key is missing or
     * cannot be converted.
     */
    private record Params(Map<String, String> values, String rawLine) {
        static Params parse(String paramSection, String rawLine) {
            Map<String, String> values = new HashMap<>();
            Matcher paramMatcher = PARAM_REGEX.matcher(paramSection);
            while (paramMatcher.find()) {
                values.put(paramMatcher.group(1), paramMatcher.group(2));
            }

            if (values.isEmpty()) {
                throw new LogProcessingException("Every line should have at least one parameter: " + rawLine);
            }

            return new Params(values, rawLine);
        }

        String get(String key) {
            String value = values.get(key);
            if (value == null) {
                throw new LogProcessingException("Expected key " + key + " to exist, but we only have " + values + ": " + rawLine);
            }
            return value;
        }

        int getInt(String key) {
            String value = get(key);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new LogProcessingException("Expected key " + key + " to be an integer, but was '" + value + "': " + rawLine, e);
            }
        }

        boolean getBoolean(String key) {
            String value = get(key);
            if (value.equals("true")) {
                return true;
            }
            if (value.equals("false")) {
                return false;
            }
            throw new LogProcessingException("Expected key " + key + " to be a boolean, but was '" + value + "': " + rawLine);
        }
    }

    record Resources(int money, int power, int fame, int voters) {
    }

    private final Database database;
    private String runId;
    private Game game;
    private GameTurn gameTurn;
    private Resources resources = new Resources(0, 0, 0, 0);

    // The DAOs for the transaction currently being processed. They are bound to a single handle so
    // that every write for one line commits (or rolls back) together; see process(String).
    private GameDao gameDao;
    private GameTurnDao gameTurnDao;

    public LogFileProcessor(Database database) {
        this.database = database;
    }

    /**
     * Processes a single line from the log file. Parsing happens first, outside any transaction; if
     * it fails no database work is started. All database writes for the line then run inside a single
     * transaction that commits when the line is processed successfully and rolls back otherwise.
     *
     * @param line one line of the log file
     * @throws LogProcessingException if the line cannot be parsed or violates the expected game state
     */
    public void process(String line) {
        if (line.isBlank()) {
            return;
        }

        LogLine parsedLine = LogLine.parse(line);

        database.jdbi().useTransaction(handle -> {
            gameDao = handle.attach(GameDao.class);
            gameTurnDao = handle.attach(GameTurnDao.class);

            switch (parsedLine) {
                case TrackerLoaded logLine -> handleTrackerLoaded(logLine);
                case GameStarted logLine -> handleGameStarted(logLine);
                case TurnStarted logLine -> handleTurnStarted(logLine);
                case ResourcesChanged logLine -> handleResourcesChanged(logLine);
                case GameEnd logLine -> handleGameEnd(logLine);
            }

            // Any line could be the last one before the game (or the tracker) is closed, so keep the
            // game's end time pushed forward to the latest activity we have seen. There is nothing to
            // update until a game has actually started.
            if (game != null) {
                game.setEndedAt(parsedLine.timestamp());
                gameDao.update(game);
            }
            if (gameTurn != null) {
                gameTurnDao.update(gameTurn);
            }
        });
    }

    private void handleTrackerLoaded(TrackerLoaded trackerLoaded) {
        if (runId != null) {
            // Whenever you start the game, the runId is logged once. When you restart the game, the file is cleared
            // and a new runId is generated. So we should never find a second one.
            throw new LogProcessingException("The runID is already set to " + runId + ", but we found a new one: " + trackerLoaded.runId);
        }
        runId = trackerLoaded.runId;
    }

    private void handleGameStarted(GameStarted gameStarted) {
        // If we found a new game while the previous one wasn't finished, we don't need to do anything. Every step is
        // saved, so we can throw away the old one without doing anything.
        game = new Game(UUID.randomUUID().toString(), runId, false, null, gameStarted.timestamp, null);
        gameDao.insert(game);

        // A new game starts fresh: forget the previous game's turn and resource totals.
        gameTurn = null;
        resources = new Resources(0, 0, 0, 0);
    }

    private void handleTurnStarted(TurnStarted turnStarted) {
        if (game == null) {
            throw new LogProcessingException("A turn started before any game was started: turn " + turnStarted.turn);
        }

        // Idem for game: we can just create a new turn object and forget about the old one.
        gameTurn = new GameTurn(game.getId(), turnStarted.turn,
                0, 0, 0, 0, 0, 0, 0, 0,
                turnStarted.deckSize);
        gameTurnDao.insert(gameTurn);

        // Every turn resets resources, except for voters.
        resources = new Resources(0, 0, 0, resources.voters());
    }

    private void handleResourcesChanged(ResourcesChanged resourcesChanged) {
        if (gameTurn == null) {
            throw new LogProcessingException("Resources changed before any turn was started: " + resourcesChanged);
        }

        Resources beforeResources = resources;
        Resources afterResources = new Resources(resourcesChanged.money, resourcesChanged.power, resourcesChanged.fame, resourcesChanged.voters);

        int deltaMoney = afterResources.money - beforeResources.money;
        if (deltaMoney > 0) {
            gameTurn.setMoneyGained(gameTurn.getMoneyGained() + deltaMoney);
        } else {
            gameTurn.setMoneySpent(gameTurn.getMoneySpent() - deltaMoney);
        }

        int deltaPower = afterResources.power - beforeResources.power;
        if (deltaPower > 0) {
            gameTurn.setPowerGained(gameTurn.getPowerGained() + deltaPower);
        } else {
            gameTurn.setPowerSpent(gameTurn.getPowerSpent() - deltaPower);
        }

        int deltaFame = afterResources.fame - beforeResources.fame;
        if (deltaFame > 0) {
            gameTurn.setFameGained(gameTurn.getFameGained() + deltaFame);
        } else {
            gameTurn.setFameSpent(gameTurn.getFameSpent() - deltaFame);
        }

        int deltaVoters = afterResources.voters - beforeResources.voters;
        if (deltaVoters > 0) {
            gameTurn.setVotersGained(gameTurn.getVotersGained() + deltaVoters);
        } else {
            gameTurn.setVotersSpent(gameTurn.getVotersSpent() - deltaVoters);
        }

        resources = afterResources;
    }

    private void handleGameEnd(GameEnd gameEnd) {
        if (game == null) {
            throw new LogProcessingException("A game ended before any game was started: " + gameEnd);
        }
        game.setFinished(true);
        game.setWon(gameEnd.won);
    }
}
