package nl.ricoapon.gui;

import nl.ricoapon.database.Database;
import nl.ricoapon.database.Game;
import nl.ricoapon.database.GameTurn;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * The "Games" tab: a table of all games on top, and — when a row is selected — that game's per-turn
 * table plus a cumulative-resources chart below. Entirely read-only.
 */
class GamesPanel extends JPanel {
    private final Database database;

    private final GamesTableModel gamesModel = new GamesTableModel();
    private final GameTurnsTableModel turnsModel = new GameTurnsTableModel();
    private final JTable gamesTable = new JTable(gamesModel);
    private final JPanel chartContainer = new JPanel(new BorderLayout());

    GamesPanel(Database database) {
        super(new BorderLayout());
        this.database = database;

        gamesTable.setAutoCreateRowSorter(true);
        gamesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gamesTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedGame();
            }
        });

        JTable turnsTable = new JTable(turnsModel);
        JSplitPane detail = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(turnsTable), chartContainer);
        detail.setResizeWeight(0.5);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(gamesTable), detail);
        split.setResizeWeight(0.4);
        add(split, BorderLayout.CENTER);

        clearDetail();
    }

    /**
     * Replaces the games shown. The "Turns" column comes from each game's derived
     * {@link Game#getNrOfTurns()}.
     */
    void showGames(List<Game> games) {
        gamesModel.setData(games);
        gamesTable.clearSelection();
        clearDetail();
    }

    private void showSelectedGame() {
        int viewRow = gamesTable.getSelectedRow();
        if (viewRow < 0) {
            clearDetail();
            return;
        }
        Game game = gamesModel.gameAt(gamesTable.convertRowIndexToModel(viewRow));
        List<GameTurn> turns = database.gameTurnDao().findByGameId(game.getId());

        turnsModel.setTurns(turns);
        chartContainer.removeAll();
        chartContainer.add(ChartFactoryHelper.cumulativeResources(turns), BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
    }

    private void clearDetail() {
        turnsModel.setTurns(List.of());
        chartContainer.removeAll();
        JLabel hint = new JLabel("Select a game to see its turns and resource curve.", JLabel.CENTER);
        hint.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        chartContainer.add(hint, BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
    }
}
