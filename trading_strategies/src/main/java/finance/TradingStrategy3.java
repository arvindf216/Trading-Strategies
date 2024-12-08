package finance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Moving Average, RSI, Average Volume based trading strategy

public class TradingStrategy3 {

    private static final String DATA_FOLDER = "data/";
    private static final String PORTFOLIO_FILE = DATA_FOLDER + "portfolio.csv";

    /**
     * Applies the strategy based on Moving Averages, RSI, and Volume.
     */
    public static void applyCustomStrategy(String csvFileName, int shortWindow, int longWindow, int rsiWindow, int volumeWindow) {
        String inputFilePath = DATA_FOLDER + csvFileName;
        String tempFilePath = DATA_FOLDER + "temp_" + csvFileName;
    
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFilePath));
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(tempFilePath))) {
    
            String header = reader.readLine();
            if (header == null) throw new IOException("Empty CSV file");
    
            writer.write(header + ",Short MA,Long MA,RSI,Avg Volume,Buy Signal,Sell Signal,Position\n");
    
            List<String[]> allRows = new ArrayList<>();
            List<Double> adjClosePrices = new ArrayList<>();
            List<Double> volumes = new ArrayList<>();
            Map<String, String> portfolio = new LinkedHashMap<>();
    
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                allRows.add(columns);
                adjClosePrices.add(parseDouble(columns[5], "Invalid Adjusted Close price")); // Adjusted Close at index 5
                volumes.add(parseDouble(columns[6], "Invalid Volume")); // Volume at index 6
            }

            // Generate indicators and signals
            for (int i = 0; i < allRows.size(); i++) {
                Double shortMA = calculateMovingAverage(adjClosePrices, i, shortWindow);
                Double longMA = calculateMovingAverage(adjClosePrices, i, longWindow);
                Double avgVolume = calculateMovingAverage(volumes, i, volumeWindow);
                Double rsi = calculateRSI(adjClosePrices, i, rsiWindow);

                Double buySignal = (shortMA != null && longMA != null && avgVolume != null && rsi != null &&
                                    shortMA > longMA && rsi > 40 && volumes.get(i) > avgVolume) ? 1.0 : 0.0;

                Double sellSignal = (shortMA != null && longMA != null && avgVolume != null && rsi != null &&
                                    shortMA < longMA && rsi < 60 && volumes.get(i) > avgVolume) ? 1.0 : 0.0;

                String position = "Neutral";
                if (buySignal == 1.0) position = "Long";
                else if (sellSignal == 1.0) position = "Short";
    
                portfolio.put(allRows.get(i)[0], position);
                String[] row = allRows.get(i);
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        row[0], // Date
                        row[1], // Open Price
                        row[2], // High Price
                        row[3], // Low Price
                        row[4], // Close Price
                        row[5], // Adjusted Close
                        row[6], // Volume
                        row[7], // Returns
                        formatValue(shortMA), // Short MA
                        formatValue(longMA), // Long MA
                        formatValue(rsi), // RSI
                        formatValue(avgVolume), // Avg Volume
                        formatValue(buySignal), // Buy Signal
                        formatValue(sellSignal), // Sell Signal
                        position)); // Position
            }
    
            savePortfolioInfo(csvFileName, portfolio);
            Files.move(Paths.get(tempFilePath), Paths.get(inputFilePath), StandardCopyOption.REPLACE_EXISTING);
    
        } catch (IOException e) {
            System.err.println("Error applying strategy: " + e.getMessage());
        }
    }    


    /**
     * Saves the portfolio information to a shared file.
     */
    private static void savePortfolioInfo(String csvFileName, Map<String, String> portfolio) {
        String ticker = csvFileName.replace("processed_", "").replace(".csv", "");

        try {
            Map<String, Map<String, String>> portfolioData = loadPortfolioData();
            for (Map.Entry<String, String> entry : portfolio.entrySet()) {
                portfolioData.computeIfAbsent(entry.getKey(), k -> new LinkedHashMap<>()).put(ticker, entry.getValue());
            }
            writePortfolioData(portfolioData);

        } catch (IOException e) {
            System.err.println("Error saving portfolio info: " + e.getMessage());
        }
    }

    
    /**
     * Loads existing portfolio data from the CSV file.
     */
    private static Map<String, Map<String, String>> loadPortfolioData() throws IOException {
        Map<String, Map<String, String>> portfolioData = new LinkedHashMap<>();
        Path filePath = Paths.get(PORTFOLIO_FILE);
        
        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String[] headers = reader.readLine().split(",");
                String line;
                
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    String date = values[0];
                    Map<String, String> row = new LinkedHashMap<>();
                    
                    for (int i = 1; i < headers.length; i++) {
                        row.put(headers[i], i < values.length ? values[i] : "Neutral");
                    }
                    
                    portfolioData.put(date, row);
                }
            }
        }

        return portfolioData;
    }

    /**
     * Writes portfolio data back to the CSV file.
     */
    private static void writePortfolioData(Map<String, Map<String, String>> portfolioData) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(PORTFOLIO_FILE), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            Set<String> tickers = new LinkedHashSet<>();
            tickers.add("Date");
            portfolioData.values().forEach(row -> tickers.addAll(row.keySet()));
            writer.write(String.join(",", tickers) + "\n");
            
            for (String date : portfolioData.keySet()) {
                StringBuilder row = new StringBuilder(date);
                for (String ticker : tickers) {
                    if (!"Date".equals(ticker)) {
                        row.append(",").append(portfolioData.get(date).getOrDefault(ticker, "Neutral"));
                    }
                }
                writer.write(row + "\n");
            }
        }
    }

    private static Double calculateMovingAverage(List<Double> prices, int endIndex, int windowSize) {
        if (endIndex + 1 < windowSize) return null;
        return prices.subList(endIndex + 1 - windowSize, endIndex + 1).stream()
                .mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private static Double calculateRSI(List<Double> prices, int endIndex, int window) {
        if (endIndex + 1 < window) return null;
    
        List<Double> deltas = new ArrayList<>();
        for (int i = endIndex + 1 - window; i <= endIndex; i++) {
            if (i - 1 >= 0) { // Ensure the index is valid
                deltas.add(prices.get(i) - prices.get(i - 1));
            }
        }
    
        double gain = deltas.stream().filter(d -> d > 0).mapToDouble(d -> d).average().orElse(0.0);
        double loss = deltas.stream().filter(d -> d < 0).mapToDouble(Math::abs).average().orElse(0.0);
    
        if (loss == 0) return 100.0; // No losses in the window
        double rs = gain / loss;
        return 100 - (100 / (1 + rs));
    }    
    
    /**
     * Parses a string to double, throwing an error message for invalid values.
     */
    private static double parseDouble(String value, String errorMessage) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid value encountered: " + value); // debugging
            System.err.flush();  // Force flush of error stream
            throw new IllegalArgumentException(errorMessage, e);
        }
    }
    
    /** 
     * Formats a Double value to 2 decimal places or returns "NULL" if the value is null.
     */
    private static String formatValue(Double value) {
        return value != null ? String.format("%.2f", value) : "NULL";
    }

}
