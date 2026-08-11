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
            case 0 -> turn.getTurnNumber();
            case 1 -> turn.getMoneyGained();
            case 2 -> turn.getMoneySpent();
            case 3 -> turn.getPowerGained();
            case 4 -> turn.getPowerSpent();
            case 5 -> turn.getFameGained();
            case 6 -> turn.getFameSpent();
            case 7 -> turn.getVotersGained();
            case 8 -> turn.getVotersSpent();
            case 9 -> turn.getStartingDeckSize();
            default -> 0;
        };
    }
}
