package finance;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class FibBacktester {
    private static final String DATA_FOLDER = "data/";

    /** 
     * Executes the trading strategy based on position sizing, risk management parameters, 
     * and volatility thresholds to buy and sell stocks in the portfolio.
     */
    public static void TradeOnStrategy(
        double initialBalance,
        // Position sizing parameters
        double buyFullBelow,    // Full position size for buying at lower levels
        double buyHalfAbove,   // Half position size for buying at higher levels
        double sellFullAbove,   // Full position size for selling at higher levels
        double sellHalfAbove,  // Half position size for selling at lower levels
        // Risk management parameters
        double atrVolatilityThreshold,  // ATR volatility threshold
        double stopLossPercent,         // Stop loss percentage
        double maxPositionSize           // Maximum position size as fraction of balance
        )
    {
        double balance = initialBalance;
        double peakBalance = initialBalance; 
        Map<String, Double> positions = new HashMap<>();  // ticker -> position size
        Map<String, List<String[]>> tickerData = new HashMap<>();
        Set<String> allDates = new TreeSet<>();  // Using TreeSet for sorted dates
        
        // Loading data
        File folder = new File(DATA_FOLDER);
        File[] processedFiles = folder.listFiles((dir, name) -> 
            name.startsWith("processed_") && name.endsWith(".csv"));
    
        if (processedFiles == null || processedFiles.length == 0) {
            System.err.println("No processed files found for backtesting.");
            return;
        }

        for (File file : processedFiles) {
            String ticker = file.getName().replace("processed_", "").replace(".csv", "");
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                List<String[]> rows = new ArrayList<>();
                reader.readLine(); 
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",");
                    rows.add(row);
                    allDates.add(row[0]); // Date is at position 0
                }
                tickerData.put(ticker, rows);
                positions.put(ticker, 0.0);
            } catch (IOException e) {
                System.err.println("Error loading file " + file.getName() + ": " + e.getMessage());
            }
        }
    
        // Initialize performance tracking
        List<Double> cumulativePnL = new ArrayList<>();
        List<Double> returns = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        List<Double> drawdownPercentages = new ArrayList<>(); 
        int totalSignals = 0;
        int correctSignals = 0;
    
        // Create date -> row index map for each ticker
        Map<String, Map<String, Integer>> tickerDateIndices = new HashMap<>();
        for (Map.Entry<String, List<String[]>> entry : tickerData.entrySet()) {
            Map<String, Integer> dateIndices = new HashMap<>();
            List<String[]> rows = entry.getValue();
            for (int i = 0; i < rows.size(); i++) {
                dateIndices.put(rows.get(i)[0], i);
            }
            tickerDateIndices.put(entry.getKey(), dateIndices);
        }


        // Portfolio allocation limits
        int numTickers = tickerData.size();
        double baseAllocationPerTicker = initialBalance / (numTickers * 2);
        Map<String, Double> maxAllocationPerTicker = new HashMap<>();
        
        // Initialize allocation tracking
        Map<String, Double> currentAllocation = new HashMap<>();
        for (String ticker : tickerData.keySet()) {
            positions.put(ticker, 0.0);
            currentAllocation.put(ticker, 0.0);
            maxAllocationPerTicker.put(ticker, baseAllocationPerTicker);
        }
    
        double previousTotalValue = initialBalance;

        // Trading starts!
        for (String date : allDates) {
            dates.add(date);

            // Update available balance for each ticker
            Map<String, Double> availableBalance = new HashMap<>();
            double totalPortfolioValue = balance;
            for (Map.Entry<String, Double> position : positions.entrySet()) {
                String ticker = position.getKey();
                double pos = position.getValue();
                if (pos > 0) {
                    double closePrice = Double.parseDouble(
                        tickerData.get(ticker).get(tickerDateIndices.get(ticker).get(date))[4]
                    );
                    totalPortfolioValue += pos * closePrice;
                }
            }
            double averageAllocation = totalPortfolioValue / numTickers;
            for (String ticker : tickerData.keySet()) {
                double currentPos = positions.get(ticker);
                double currentPrice = Double.parseDouble(
                    tickerData.get(ticker).get(tickerDateIndices.get(ticker).get(date))[4]);
                double tickerValue = currentPos * currentPrice;
                currentAllocation.put(ticker, tickerValue);
                double maxAllocation = Math.min(
                    maxAllocationPerTicker.get(ticker),
                    averageAllocation * maxPositionSize);
                availableBalance.put(ticker, maxAllocation - tickerValue);
            }
    
            // Iterating over all tickers
            for (String ticker : tickerData.keySet()) {
                List<String[]> rows = tickerData.get(ticker);
                Map<String, Integer> dateIndices = tickerDateIndices.get(ticker);
                Integer currentIndex = dateIndices.get(date);
                
                if (currentIndex == null || currentIndex == 0) continue; // Skip if no data or first entry
                
                String[] currentRow = rows.get(currentIndex);
                String[] previousRow = rows.get(currentIndex - 1);
    
                double closePrice = Double.parseDouble(currentRow[4]);
                double previousClose = Double.parseDouble(previousRow[4]);
                double atr = Double.parseDouble(currentRow[15]);
                double swingHigh = Double.parseDouble(currentRow[8]);
                double swingLow = Double.parseDouble(currentRow[9]);
                Map<String, Double> fibLevels = Map.of(
                    "Fib 23.6", Double.parseDouble(currentRow[10]),
                    "Fib 38.2", Double.parseDouble(currentRow[11]),
                    "Fib 50", Double.parseDouble(currentRow[12]),
                    "Fib 61.8", Double.parseDouble(currentRow[13]),
                    "Fib 78.6", Double.parseDouble(currentRow[14])
                );
    
                
                String signal = "Hold";
                double investment = 0.0;
                double position = positions.get(ticker);
                double tickerBalance = availableBalance.get(ticker);

                // Updating signals
                if (atr <= atrVolatilityThreshold * closePrice) {
                    
                    if (closePrice <= fibLevels.get("Fib 23.6") && previousClose > fibLevels.get("Fib 23.6")) {
                        signal = "Buy";
                        investment = Math.min(tickerBalance, baseAllocationPerTicker * buyHalfAbove);
                    } else if (closePrice <= fibLevels.get("Fib 38.2") && previousClose > fibLevels.get("Fib 38.2")) {
                        signal = "Buy";
                        investment = Math.min(tickerBalance, baseAllocationPerTicker * buyFullBelow);
                    } else if (closePrice <= fibLevels.get("Fib 61.8") && previousClose > fibLevels.get("Fib 61.8")) {
                        signal = "Buy";
                        investment = Math.min(tickerBalance, baseAllocationPerTicker * buyFullBelow);
                    }
                    
                    if (closePrice >= fibLevels.get("Fib 78.6") && previousClose < fibLevels.get("Fib 78.6")) {
                        signal = "Sell";
                        investment = position * sellHalfAbove;
                    } else if (closePrice >= fibLevels.get("Fib 61.8") && previousClose < fibLevels.get("Fib 61.8")) {
                        signal = "Sell";
                        investment = position * sellFullAbove;
                    }
                }

                // Executing trades
                if ("Buy".equalsIgnoreCase(signal)) {
                    double positionSize = investment / closePrice;
                    if (balance >= investment && currentAllocation.get(ticker) + investment <= maxAllocationPerTicker.get(ticker)) {
                        positions.put(ticker, position + positionSize);
                        balance -= investment;
                        currentAllocation.put(ticker, currentAllocation.get(ticker) + investment);
                        totalSignals++;
                        if (closePrice > swingLow && closePrice < swingHigh) correctSignals++;
                    }
                } else if ("Sell".equalsIgnoreCase(signal)) {
                    double sellAmount = Math.min(position, investment / closePrice);
                    balance += sellAmount * closePrice;
                    positions.put(ticker, position - sellAmount);
                    currentAllocation.put(ticker, currentAllocation.get(ticker) - (sellAmount * closePrice));
                    totalSignals++;
                    if (closePrice < swingLow || closePrice > swingHigh) correctSignals++;
                }
            }
    
            // Calculating total portfolio value
            double totalValue = balance;
            for (Map.Entry<String, Double> position : positions.entrySet()) {
                String ticker = position.getKey();
                double pos = position.getValue();
                if (pos > 0) {
                    String[] currentRow = tickerData.get(ticker).get(tickerDateIndices.get(ticker).get(date));
                    double closePrice = Double.parseDouble(currentRow[4]);
                    totalValue += pos * closePrice;
                }
            }
    
            // Daily performance metrics
            double dailyReturn = (totalValue - previousTotalValue) / previousTotalValue;
            returns.add(dailyReturn);
            cumulativePnL.add(totalValue - initialBalance);
            previousTotalValue = totalValue;

            // Drawdown computation
            peakBalance = Math.max(peakBalance, balance);
            double currentDrawdown = (balance - peakBalance) / peakBalance;
            drawdownPercentages.add(currentDrawdown);
        }
    
        // Final performance metrics
        double finalValue = balance;
        for (Map.Entry<String, Double> position : positions.entrySet()) {
            String ticker = position.getKey();
            double pos = position.getValue();
            if (pos > 0) {
                List<String[]> rows = tickerData.get(ticker);
                double lastClose = Double.parseDouble(rows.get(rows.size() - 1)[4]);
                finalValue += pos * lastClose;
            }
        }
    
        double cumulativeReturn = (finalValue - initialBalance) / initialBalance;
        double avgReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double volatility = Math.sqrt(returns.stream()
                .mapToDouble(r -> Math.pow(r - avgReturn, 2))
                .sum() / returns.size());
        double sharpeRatio = volatility > 0 ? avgReturn / volatility * Math.sqrt(252) : 0.0;
        double maxDrawdown = drawdownPercentages.stream().min(Double::compareTo).orElse(0.0);
        double accuracy = totalSignals > 0 ? (double) correctSignals / totalSignals : 0.0;
    
        // Print performance metrics
        System.out.println("\nPortfolio Performance Metrics:");
        System.out.printf("Initial Balance: $%.2f%n", initialBalance);
        System.out.printf("Final Balance: $%.2f%n", finalValue);
        System.out.printf("Total Return: %.2f%%%n", cumulativeReturn * 100);
        System.out.printf("Average Daily Return: %.2f%%%n", avgReturn * 100);
        System.out.printf("Volatility: %.2f%%%n", volatility * 100);
        System.out.printf("Annualized Sharpe Ratio: %.2f%n", sharpeRatio);
        System.out.printf("Maximum Drawdown: %.2f%%%n", maxDrawdown * 100);
        System.out.printf("Signal Accuracy: %.2f%%%n", accuracy * 100);
    
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
