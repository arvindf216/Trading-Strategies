package finance;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class Backtester {
    private static final String DATA_FOLDER = "data/";

    /** 
     * Performs backtesting on the trading strategy, calculating portfolio performance metrics such as 
     * returns, drawdowns, and Sharpe ratio based on daily stock data.
     */
    public static void TradeOnStrategy() {
        double initialBalance = 100000; // Starting portfolio balance
        double balance = initialBalance;
        double peakBalance = initialBalance; // For drawdown calculation
        List<Double> portfolioReturns = new ArrayList<>(); // Daily portfolio returns
        List<Double> drawdownPercentages = new ArrayList<>(); // Store drawdowns
        List<Double> cumulativePnL = new ArrayList<>(); // Store cumulative profit and loss
        List<String> dates = new ArrayList<>(); // Store dates for plotting

        File folder = new File(DATA_FOLDER);
        File[] processedFiles = folder.listFiles((dir, name) -> name.startsWith("processed_") && name.endsWith(".csv"));

        if (processedFiles == null || processedFiles.length == 0) {
            System.err.println("No processed files found for backtesting.");
            return;
        }

        Map<String, Map<String, Double>> dateTickerReturns = new HashMap<>(); // Map of date -> (ticker -> returns)
        Map<String, Map<String, String>> dateTickerPositions = new HashMap<>(); // Map of date -> (ticker -> position)

        int totalSignals = 0;  // Count of all trading signals (Long or Short)
        int correctSignals = 0;  // Count of correct trading signals

        // Aggregate data from all files
        for (File file : processedFiles) {
            String ticker = file.getName().replace("processed_", "").replace(".csv", "");
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                reader.readLine(); 
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split(",");
                    String date = columns[0]; // Date
                    if (!columns[7].equalsIgnoreCase("NULL") && !columns[7].isEmpty()) {
                        double returns = Double.parseDouble(columns[7]); // Returns at index 7
                        dateTickerReturns.putIfAbsent(date, new HashMap<>());
                        dateTickerReturns.get(date).put(ticker, returns);
                    }
                    String position = (columns.length > 12) ? columns[14] : columns[11]; // Position based on column count
                    dateTickerPositions.putIfAbsent(date, new HashMap<>());
                    dateTickerPositions.get(date).put(ticker, position);
                } 
            } catch (IOException e) {
                System.err.println("Error reading file " + file.getName() + ": " + e.getMessage());
            }
        }

        // Perform portfolio-level backtesting
        for (String date : dateTickerReturns.keySet()) {
            Map<String, Double> tickerReturns = dateTickerReturns.get(date);
            Map<String, String> tickerPositions = dateTickerPositions.get(date);

            double dailyReturn = 0.0;
            int signalsToday = 0;

            for (String ticker : tickerReturns.keySet()) {
                double returns = tickerReturns.get(ticker);
                String position = tickerPositions.getOrDefault(ticker, "Neutral");
    
                if (!position.equalsIgnoreCase("Neutral")) { // Long or short
                    signalsToday++;
                    totalSignals++;
                    boolean isCorrectSignal = (position.equalsIgnoreCase("Long") && returns > 0) || 
                                              (position.equalsIgnoreCase("Short") && returns < 0);
                    if (isCorrectSignal) {
                        correctSignals++;
                    }
                    dailyReturn += position.equalsIgnoreCase("Long") ? returns : -returns;
                }
            }

            dailyReturn /= signalsToday > 0 ? signalsToday : 1; // Average return for the portfolio
            portfolioReturns.add(dailyReturn);
            balance += balance * dailyReturn; // Update balance

            // Track cumulative PnL and dates
            cumulativePnL.add(balance - initialBalance);
            dates.add(date);

            // Drawdown calculation
            peakBalance = Math.max(peakBalance, balance);
            double currentDrawdown = (balance - peakBalance) / peakBalance;
            drawdownPercentages.add(currentDrawdown);
        }

        // Performance Metrics
        double totalReturn = (balance - initialBalance) / initialBalance;
        double avgReturn = portfolioReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double volatility = Math.sqrt(portfolioReturns.stream()
                .mapToDouble(r -> Math.pow(r - avgReturn, 2)).sum() / portfolioReturns.size());
        double sharpeRatio = volatility > 0 ? avgReturn / volatility * Math.sqrt(252) : 0.0;
        double maxDrawdown = drawdownPercentages.stream().min(Double::compareTo).orElse(0.0);
        double accuracy = totalSignals > 0 ? (double) correctSignals / totalSignals : 0.0;

        // Results
        System.out.println("\nPortfolio Performance Metrics:");
        System.out.printf("Initial Balance: $%.2f%n", initialBalance);
        System.out.printf("Final Balance: $%.2f%n", balance);
        System.out.printf("Total Return: %.2f%%%n", totalReturn * 100);
        System.out.printf("Average Daily Return: %.2f%%%n", avgReturn * 100);
        System.out.printf("Volatility: %.2f%%%n", volatility * 100);
        System.out.printf("Annualized Sharpe Ratio: %.2f%n", sharpeRatio);
        System.out.printf("Maximum Drawdown: %.2f%%%n", maxDrawdown * 100);
        System.out.printf("Signal Accuracy: %.2f%%%n", accuracy * 100);

        // Cumulative PnL chart
        plotCumulativePnL(dates, cumulativePnL);
    }
    
    /** 
     * Plots and saves the cumulative profit and loss (PnL) over time as a chart.
     * Image is saved in the same directory as 'cumulative_pnl.png'
     */
    private static void plotCumulativePnL(List<String> dates, List<Double> pnl) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < dates.size(); i++) {
            dataset.addValue(pnl.get(i), "Cumulative PnL", dates.get(i));
        }
    
        JFreeChart chart = ChartFactory.createLineChart(
                "Cumulative PnL Over Time",
                "Date",
                "PnL ($)",
                dataset
        );
    
        try {
            File imageFile = new File(DATA_FOLDER + "cumulative_pnl.png");
            ChartUtils.saveChartAsPNG(imageFile, chart, 800, 600);
            System.out.println("Cumulative PnL chart saved as " + imageFile.getPath());
        } catch (IOException e) {
            System.err.println("Error saving cumulative PnL chart: " + e.getMessage());
        }
    }
}
