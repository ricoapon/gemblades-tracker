package nl.ricoapon.gui;

import nl.ricoapon.database.Database;
import nl.ricoapon.database.Game;

import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;

/**
 * The overview for one game mode (Gauntlet or normal). Gauntlet and normal runs are scored very
 * differently, so each mode gets its own {@code GameModePanel}; the panel itself is mode-agnostic and
 * simply shows whatever games and statistics it is given, using the same Games and Statistics views.
 */
class GameModePanel extends JPanel {
    private final GamesPanel gamesPanel;
    private final StatisticsPanel statisticsPanel = new StatisticsPanel();

    GameModePanel(Database database) {
        super(new BorderLayout());
        this.gamesPanel = new GamesPanel(database);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Games", gamesPanel);
        tabs.addTab("Statistics", statisticsPanel);
        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Replaces the games shown and the statistics rendered. The caller is responsible for passing
     * only the games of this panel's mode, and statistics computed over that same mode.
     */
    void update(List<Game> games, Statistics statistics) {
        gamesPanel.showGames(games);
        statisticsPanel.setStatistics(statistics);
    }
}
