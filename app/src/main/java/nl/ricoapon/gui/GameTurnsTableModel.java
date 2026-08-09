package nl.ricoapon.gui;

import nl.ricoapon.database.GameTurn;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * Read-only table model listing every turn of the currently selected game.
 */
class GameTurnsTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "Turn", "Money +", "Money -", "Power +", "Power -",
            "Fame +", "Fame -", "Voters +", "Voters -", "Deck size"
    };

    private List<GameTurn> turns = List.of();

    void setTurns(List<GameTurn> turns) {
        this.turns = turns;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return turns.size();
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
    public Class<?> getColumnClass(int columnIndex) {
        return Integer.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        GameTurn turn = turns.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> turn.turnNumber();
            case 1 -> turn.moneyGained();
            case 2 -> turn.moneySpent();
            case 3 -> turn.powerGained();
            case 4 -> turn.powerSpent();
            case 5 -> turn.fameGained();
            case 6 -> turn.fameSpent();
            case 7 -> turn.votersGained();
            case 8 -> turn.votersSpent();
            case 9 -> turn.startingDeckSize();
            default -> 0;
        };
    }
}
