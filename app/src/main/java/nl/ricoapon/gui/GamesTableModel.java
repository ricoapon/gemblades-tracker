package nl.ricoapon.gui;

import nl.ricoapon.database.Game;

import javax.swing.table.AbstractTableModel;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Read-only table model backing the games overview. Each row is a {@link Game}; the turn count
 * comes from the game's derived {@link Game#getNrOfTurns()}.
 */
class GamesTableModel extends AbstractTableModel {
    private static final String[] COLUMNS =
            {"Run", "Difficulty", "Length", "Req. voters", "Started", "Ended", "Finished", "Won", "Turns", "Duration"};
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private List<Game> games = List.of();

    void setData(List<Game> games) {
        this.games = games;
        fireTableDataChanged();
    }

    Game gameAt(int rowIndex) {
        return games.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return games.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Game game = games.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> game.getRunId();
            case 1 -> game.getDifficulty();
            case 2 -> game.getLength();
            case 3 -> game.getRequiredVoters();
            case 4 -> TIME_FORMAT.format(game.getStartedAt());
            case 5 -> game.getEndedAt() == null ? "" : TIME_FORMAT.format(game.getEndedAt());
            case 6 -> game.isFinished() ? "yes" : "no";
            case 7 -> outcome(game);
            case 8 -> game.getNrOfTurns();
            case 9 -> duration(game.getStartedAt(), game.getEndedAt());
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        // Difficulty, Length, Req. voters and Turns are numeric; everything else renders as text.
        return switch (columnIndex) {
            case 1, 2, 3, 8 -> Integer.class;
            default -> String.class;
        };
    }

    private static String outcome(Game game) {
        if (!game.isFinished()) {
            return "";
        }
        return Boolean.TRUE.equals(game.getWon()) ? "won" : "lost";
    }

    private static String duration(Instant start, Instant end) {
        if (start == null || end == null) {
            return "";
        }
        Duration d = Duration.between(start, end);
        if (d.isNegative()) {
            return "";
        }
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        return hours > 0
                ? String.format("%dh %02dm %02ds", hours, minutes, seconds)
                : String.format("%dm %02ds", minutes, seconds);
    }
}
