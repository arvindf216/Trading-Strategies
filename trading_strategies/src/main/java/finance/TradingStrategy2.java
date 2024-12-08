package finance;

import java.io.*;
import java.nio.file.*;
import java.util.*;

// Exponential moving average based trading strategy

public class TradingStrategy2 {

    private static final String DATA_FOLDER = "data/";
    private static final String PORTFOLIO_FILE = DATA_FOLDER + "portfolio.csv";

    /**
     * Applies a moving average trading strategy and updates the portfolio.
     */
    public static void applyEmaStrategy(String csvFileName, int shortWindow, int longWindow) {
        String inputFilePath = DATA_FOLDER + csvFileName;
        String tempFilePath = DATA_FOLDER + "temp_" + csvFileName;
    
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFilePath));
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(tempFilePath))) {
    
            String header = reader.readLine();
            if (header == null) throw new IOException("Empty CSV file");
    
            writer.write(header + ",ShortEMA,Long EMA,Signal,Position\n");
    
            List<String[]> allRows = new ArrayList<>();
            List<Double> adjClosePrices = new ArrayList<>();
            Map<String, String> portfolio = new LinkedHashMap<>();
    
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                allRows.add(columns);
                adjClosePrices.add(parseDouble(columns[5], "Invalid Adjusted Close price")); // Adjusted Close at index 5
            }
    
            // Generate signals and update the portfolio
            List<Double> shortEmaList = calculateEMA(adjClosePrices, shortWindow);
            List<Double> longEmaList = calculateEMA(adjClosePrices, longWindow);

            for (int i = 0; i < allRows.size(); i++) {
                Double shortEMA = shortEmaList.get(i);
                Double longEMA = longEmaList.get(i);
    
                Double signal = 0.0;
                if (shortEMA != null && longEMA != null) {
                    signal = (shortEMA > longEMA) ? 1.0 : (shortEMA < longEMA) ? -1.0 : 0.0;
                }
    
                String position;
                if (signal == 1) {
                    position = "Long";
                } else if (signal == -1) {
                    position = "Short";
                } else {
                    position = "Neutral";
                }
    
                portfolio.put(allRows.get(i)[0], position);
                String[] row = allRows.get(i);
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                row[0], // Date
                row[1], // Open Price
                row[2], // High Price
                row[3], // Low Price
                row[4], // Close Price
                row[5], // Adjusted Close
                row[6], // Volume
                row[7], // Returns
                formatValue(shortEMA), // Short EMA
                formatValue(longEMA), // Long EMA
                formatValue(signal), // Signal
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

    /**
     * Calculates EMA for a list of prices.
     */
    private static List<Double> calculateEMA(List<Double> prices, int windowSize) {
        List<Double> emaList = new ArrayList<>();
        if (prices.isEmpty() || windowSize <= 0) return emaList;

        Double multiplier = 2.0 / (windowSize + 1);
        Double previousEma = null;

        for (int i = 0; i < prices.size(); i++) {
            Double price = prices.get(i);
            if (i < windowSize - 1) {
                emaList.add(null); // Not enough data for EMA
                continue;
            }
            if (previousEma == null) {
                previousEma = prices.subList(0, windowSize).stream()
                        .mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
            } else {
                previousEma = (price - previousEma) * multiplier + previousEma;
            }
            emaList.add(previousEma);
        }
        return emaList;
    }
    
    /**
     * Parses a string to double, throwing an error message for invalid values.
     */
    private static double parseDouble(String value, String errorMessage) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
    
    /** 
     * Formats a Double value to 2 decimal places or returns "NULL" if the value is null.
     */
    private static String formatValue(Double value) {
        return value != null ? String.format("%.2f", value) : "NULL";
    }

}
