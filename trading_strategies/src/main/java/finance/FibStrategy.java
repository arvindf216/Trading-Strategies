package finance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Fibonacci retracement trading strategy

public class FibStrategy {

    private static final String DATA_FOLDER = "data/";

    /**
     * Applies the Fibonacci retracement trading strategy.
     */
    public static void applyFibStrategy(String csvFileName, int period, int atrPeriod) {
        String inputFilePath = DATA_FOLDER + csvFileName;
        String tempFilePath = DATA_FOLDER + "temp_" + csvFileName;
    
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFilePath));
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(tempFilePath))) {
    
            String header = reader.readLine();
            if (header == null) throw new IOException("Empty CSV file");
    
            writer.write(header + ",Swing High,Swing Low,Fib 23.6,Fib 38.2,Fib 50,Fib 61.8,Fib 78.6,ATR\n");

            List<String[]> allRows = new ArrayList<>();
            List<Double> closePrices = new ArrayList<>();
            List<Double> highPrices = new ArrayList<>();
            List<Double> lowPrices = new ArrayList<>();
    
            // Read and store all rows
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                allRows.add(columns);
                closePrices.add(parseDouble(columns[4], "Invalid Close price")); // Close at index 4
                highPrices.add(parseDouble(columns[2], "Invalid High price")); // High at index 2
                lowPrices.add(parseDouble(columns[3], "Invalid Low price")); // Low at index 3
            }

            // Generate indicators and signals
            for (int i = 0; i < allRows.size(); i++) {
                Double swingHigh = calculateMax(highPrices, i, period);
                Double swingLow = calculateMin(lowPrices, i, period);
                if (swingHigh == null || swingLow == null) {
                    continue; // Skip processing for rows with insufficient data
                }            

                Double atr = null;
                if (i >= atrPeriod) {
                    atr = calculateATR(highPrices, lowPrices, closePrices, i, atrPeriod);
                } else {
                    continue; // Skip processing for rows with insufficient data for ATR
                }                
            
                Map<String, Double> fibLevels = calculateFibLevels(swingHigh, swingLow);
                if (fibLevels.isEmpty()) {
                    continue; // Skip processing for rows with missing Fibonacci levels
                }
    
                String[] row = allRows.get(i);
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        row[0], // Date
                        row[1], // Open Price
                        row[2], // High Price
                        row[3], // Low Price
                        row[4], // Close Price
                        row[5], // Adjusted Close
                        row[6], // Volume
                        row[7], // Returns
                        formatValue(swingHigh), // Swing High
                        formatValue(swingLow), // Swing Low
                        formatValue(fibLevels.get("Fib 23.6")),
                        formatValue(fibLevels.get("Fib 38.2")),
                        formatValue(fibLevels.get("Fib 50")),
                        formatValue(fibLevels.get("Fib 61.8")),
                        formatValue(fibLevels.get("Fib 78.6")),
                        formatValue(atr))); // ATR
            }
            Files.move(Paths.get(tempFilePath), Paths.get(inputFilePath), StandardCopyOption.REPLACE_EXISTING);
    
        } catch (IOException e) {
            System.err.println("Error applying strategy: " + e.getMessage());
        }
    }    

    /** 
     * Calculates the maximum value in a list of prices for a specified period.
     * Returns null if there are insufficient data points for the specified period.
     */
    private static Double calculateMax(List<Double> values, int endIndex, int period) {
        if (endIndex + 1 < period) return null;
        return values.subList(endIndex + 1 - period, endIndex + 1).stream().mapToDouble(v -> v).max().orElse(Double.NaN);
    }

    /** 
     * Calculates the minimum value in a list of prices for a specified period.
     * Returns null if there are insufficient data points for the specified period.
     */
    private static Double calculateMin(List<Double> values, int endIndex, int period) {
        if (endIndex + 1 < period) return null;
        return values.subList(endIndex + 1 - period, endIndex + 1).stream().mapToDouble(v -> v).min().orElse(Double.NaN);
    }

    /** 
     * Calculates the Fibonacci retracement levels based on the given high and low prices.
    * Returns a map of Fibonacci levels (23.6%, 38.2%, 50%, 61.8%, and 78.6%).
    */
    private static Map<String, Double> calculateFibLevels(Double high, Double low) {
        if (high == null || low == null) return Map.of();
        double diff = high - low;
        return Map.of(
                "Fib 23.6", high - 0.236 * diff,
                "Fib 38.2", high - 0.382 * diff,
                "Fib 50", high - 0.5 * diff,
                "Fib 61.8", high - 0.618 * diff,
                "Fib 78.6", high - 0.786 * diff
        );
    }

    /** 
     * Calculates the Average True Range (ATR) for a given period based on high, low, and close prices.
     * Returns null if there are insufficient data points for the specified period.
     */
    private static Double calculateATR(List<Double> highs, List<Double> lows, List<Double> closes, int endIndex, int period) {
        if (endIndex + 1 < period) return null;
        double atr = 0.0;
        for (int i = endIndex + 1 - period; i <= endIndex; i++) {
            double highLow = highs.get(i) - lows.get(i);
            double highClose = Math.abs(highs.get(i) - closes.get(i - 1));
            double lowClose = Math.abs(lows.get(i) - closes.get(i - 1));
            atr += Math.max(highLow, Math.max(highClose, lowClose));
        }
        return atr / period;
    }
    
    /**
     * Parses a string to double, throwing an error message for invalid values.
     */
    private static double parseDouble(String value, String errorMessage) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
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
