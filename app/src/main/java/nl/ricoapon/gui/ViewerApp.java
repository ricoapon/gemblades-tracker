package nl.ricoapon.gui;

import nl.ricoapon.Constants;
import nl.ricoapon.database.Database;
import nl.ricoapon.database.Game;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

    /**
     * Shows the viewer for the given database on the Swing event dispatch thread. {@code onClose} is
     * run when the window is closed, allowing the caller to shut down background work such as the log
     * file tracker.
     */
    public static void launch(Database database, Runnable onClose) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fall back to the default look and feel.
            }
            new ViewerApp(database).show(onClose);
        });
    }

    private void show(Runnable onClose) {
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
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                onClose.run();
            }
        });
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
