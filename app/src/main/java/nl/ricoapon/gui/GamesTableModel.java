package nl.ricoapon.gui;

import nl.ricoapon.database.Game;

import javax.swing.table.AbstractTableModel;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Read-only table model backing the games overview. Each row is a {@link Game}; the turn count is
 * supplied separately (there is no turn count on the record itself).
 */
class GamesTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Run", "Started", "Ended", "Finished", "Won", "Turns", "Duration"};
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private List<Game> games = List.of();
    private Map<String, Integer> turnCounts = Map.of();

    void setData(List<Game> games, Map<String, Integer> turnCounts) {
        this.games = games;
        this.turnCounts = turnCounts;
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
            case 1 -> TIME_FORMAT.format(game.getStartedAt());
            case 2 -> game.getEndedAt() == null ? "" : TIME_FORMAT.format(game.getEndedAt());
            case 3 -> game.isFinished() ? "yes" : "no";
            case 4 -> outcome(game);
            case 5 -> turnCounts.getOrDefault(game.getId(), 0);
            case 6 -> duration(game.getStartedAt(), game.getEndedAt());
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 5 ? Integer.class : String.class;
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
