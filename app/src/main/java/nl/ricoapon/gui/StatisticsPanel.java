package nl.ricoapon.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * The "Statistics" tab: a grid of aggregate figures on top and a turns-to-win bar chart below.
 */
class StatisticsPanel extends JPanel {
    private final JPanel summary = new JPanel(new GridBagLayout());
    private final JPanel chartContainer = new JPanel(new BorderLayout());

    StatisticsPanel() {
        super(new BorderLayout());
        summary.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        add(summary, BorderLayout.NORTH);
        add(chartContainer, BorderLayout.CENTER);
    }

    void setStatistics(Statistics stats) {
        summary.removeAll();
        int row = 0;

        addSection("Games", row++);
        addRow("Total", Integer.toString(stats.totalGames()), row++);
        addRow("In progress", Integer.toString(stats.inProgress()), row++);
        addRow("Finished", Integer.toString(stats.finished()), row++);
        addRow("Won / lost", stats.won() + " / " + stats.lost(), row++);
        addRow("Win rate", String.format("%.1f%%", stats.winRate()), row++);

        addSection("Turns to win", row++);
        addRow("Average", stats.won() == 0 ? "-" : String.format("%.1f", stats.averageTurnsToWin()), row++);
        addRow("Best (fewest)", formatTurns(stats.bestTurnsToWin(), stats.bestRunId()), row++);
        addRow("Worst (most)", formatTurns(stats.worstTurnsToWin(), stats.worstRunId()), row++);

        addSection("Per-turn economy (won games)", row++);
        addRow("Money spent / turn", format(stats.avgMoneySpentPerTurn(), stats.won()), row++);
        addRow("Money gained / turn", format(stats.avgMoneyGainedPerTurn(), stats.won()), row++);
        addRow("Net money / turn", format(stats.avgNetMoneyPerTurn(), stats.won()), row++);
        addRow("Net power / turn", format(stats.avgNetPowerPerTurn(), stats.won()), row++);
        addRow("Net fame / turn", format(stats.avgNetFamePerTurn(), stats.won()), row++);
        addRow("Net voters / turn", format(stats.avgNetVotersPerTurn(), stats.won()), row++);

        chartContainer.removeAll();
        if (stats.wonGames().isEmpty()) {
            JLabel hint = new JLabel("No won games yet — nothing to chart.", JLabel.CENTER);
            chartContainer.add(hint, BorderLayout.CENTER);
        } else {
            chartContainer.add(ChartFactoryHelper.turnsToWin(stats.wonGames()), BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    private static String formatTurns(int turns, String runId) {
        return runId == null ? "-" : turns + " (" + runId + ")";
    }

    private static String format(double value, int wonGames) {
        return wonGames == 0 ? "-" : String.format("%.1f", value);
    }

    private void addSection(String title, int row) {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(row == 0 ? 0 : 10, 0, 4, 0);
        summary.add(label, c);
    }

    private void addRow(String key, String value, int row) {
        GridBagConstraints keyConstraints = new GridBagConstraints();
        keyConstraints.gridx = 0;
        keyConstraints.gridy = row;
        keyConstraints.anchor = GridBagConstraints.WEST;
        keyConstraints.insets = new Insets(1, 12, 1, 24);
        summary.add(new JLabel(key), keyConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.anchor = GridBagConstraints.WEST;
        valueConstraints.insets = new Insets(1, 0, 1, 0);
        valueConstraints.weightx = 1.0;
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD));
        summary.add(valueLabel, valueConstraints);
    }
}
