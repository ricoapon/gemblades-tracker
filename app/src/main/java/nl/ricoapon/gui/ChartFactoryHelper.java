package nl.ricoapon.gui;

import nl.ricoapon.database.GameTurn;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.List;

/**
 * Builds the JFreeChart panels used by the viewer: a per-game cumulative-resources line chart and a
 * turns-to-win bar chart across won games.
 */
final class ChartFactoryHelper {
    private ChartFactoryHelper() {
    }

    /**
     * A line chart of cumulative net resources (running sum of gained - spent) against turn number,
     * one series per resource. This shows how much of each resource the player was holding at each
     * turn of the given game.
     */
    static ChartPanel cumulativeResources(List<GameTurn> turns) {
        XYSeries money = new XYSeries("Money");
        XYSeries power = new XYSeries("Power");
        XYSeries fame = new XYSeries("Fame");
        XYSeries voters = new XYSeries("Voters");

        int cumulativeMoney = 0;
        int cumulativePower = 0;
        int cumulativeFame = 0;
        int cumulativeVoters = 0;
        for (GameTurn turn : turns) {
            cumulativeMoney += turn.getMoneyGained() - turn.getMoneySpent();
            cumulativePower += turn.getPowerGained() - turn.getPowerSpent();
            cumulativeFame += turn.getFameGained() - turn.getFameSpent();
            cumulativeVoters += turn.getVotersGained() - turn.getVotersSpent();
            money.add(turn.getTurnNumber(), cumulativeMoney);
            power.add(turn.getTurnNumber(), cumulativePower);
            fame.add(turn.getTurnNumber(), cumulativeFame);
            voters.add(turn.getTurnNumber(), cumulativeVoters);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(money);
        dataset.addSeries(power);
        dataset.addSeries(fame);
        dataset.addSeries(voters);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Cumulative resources per turn", "Turn", "Amount held", dataset);
        return new ChartPanel(chart);
    }

    /**
     * A bar chart of turns-to-win per won game, keyed by run id.
     */
    static ChartPanel turnsToWin(List<Statistics.WonGameResult> wonGames) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Statistics.WonGameResult result : wonGames) {
            dataset.addValue(result.turns(), "Turns", result.runId());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Turns to win per game", "Run", "Turns", dataset);
        return new ChartPanel(chart);
    }
}
