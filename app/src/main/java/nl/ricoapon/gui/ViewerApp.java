package nl.ricoapon.gui;

import nl.ricoapon.Constants;
import nl.ricoapon.database.Database;
import nl.ricoapon.database.Game;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Barebones read-only Swing viewer for the Gemblades tracker database. Shows an overview of games
 * and aggregate statistics. The database is written continuously by the tracker, so the view is
 * reloaded on demand via the Refresh button rather than kept live.
 */
public final class ViewerApp {
    private static final DateTimeFormatter LOADED_AT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Database database;
    private final GamesPanel gamesPanel;
    private final StatisticsPanel statisticsPanel = new StatisticsPanel();
    private final JLabel statusLabel = new JLabel();

    private ViewerApp(Database database) {
        this.database = database;
        this.gamesPanel = new GamesPanel(database);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default look and feel.
        }
        SwingUtilities.invokeLater(ViewerApp::launch);
    }

    private static void launch() {
        Database database;
        try {
            database = new Database(Constants.DB_FILE_PATH);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null,
                    "Could not open the database at:\n" + Constants.DB_FILE_PATH
                            + "\n\n" + e.getMessage()
                            + "\n\nThe tracker may not have created it yet.",
                    "Gemblades Tracker", JOptionPane.ERROR_MESSAGE);
            return;
        }
        new ViewerApp(database).show();
    }

    private void show() {
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> reload());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(refresh);
        toolbar.add(statusLabel);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Games", gamesPanel);
        tabs.addTab("Statistics", statisticsPanel);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        content.add(toolbar, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);

        JFrame frame = new JFrame("Gemblades Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(content);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);

        reload();
        frame.setVisible(true);
    }

    private void reload() {
        List<Game> games = database.gameDao().findAll();
        gamesPanel.showGames(games);
        statisticsPanel.setStatistics(StatisticsService.compute(database));
        statusLabel.setText(games.size() + " game(s) — loaded " + LocalTime.now().format(LOADED_AT)
                + " — " + Constants.DB_FILE_PATH);
    }
}
