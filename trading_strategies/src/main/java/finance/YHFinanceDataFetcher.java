package finance;

import okhttp3.*;
import com.google.gson.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YHFinanceDataFetcher {
    private static final String API_KEY = "your_api_key";
    private static final String API_HOST = "yahoo-finance15.p.rapidapi.com";
    private static final String BASE_URL = "https://yahoo-finance15.p.rapidapi.com/api/v1/markets/stock/history";
    private static final String DATA_FOLDER = "data/";

    /**
     * Adds a returns column to the input CSV file and saves it as a processed file.
     */
    public static void addReturnsColumn(String csvFileName) {
        String inputFilePath = DATA_FOLDER + csvFileName;
        String outputFilePath = DATA_FOLDER + "processed_" + csvFileName;
    
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFilePath))) {
            String header = reader.readLine();
            if (header == null) throw new IOException("Empty CSV file");
    
            List<String[]> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                rows.add(columns);
            }
    
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))) {
                writer.write(header + ",Returns\n");
    
                Double previousAdjClose = null;
                for (String[] columns : rows) {
                    double adjClosePrice = parseDouble(columns[5], "Invalid Adjusted Close price");
                    Double returnVal = (previousAdjClose != null) ? (adjClosePrice - previousAdjClose) / previousAdjClose 
                                                                  : null;
                    writer.write(String.format("%s,%s%n", String.join(",", columns), 
                    returnVal != null ? String.format("%.2f", returnVal) : "NULL"));
                    previousAdjClose = adjClosePrice;
                }
            }
            // System.out.println("Processed file saved as: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
        }
    }

    /** 
     * Fetches historical stock data for the provided tickers and saves it as CSV files.
     */
    public static void fetchStockData(OkHttpClient client, String[] tickers) {
        try {
            //ensure folder exists
            Files.createDirectories(Paths.get(DATA_FOLDER));
            for (String ticker : tickers) {
                fetchAndSaveStockData(client, ticker);
            }
            System.out.println("Historical data fetched and saved successfully.");
        } catch (IOException e) {
            System.err.println("Error creating data folder: " + e.getMessage());
        }
    }
    
    /** 
     * Fetches stock data for a single ticker and saves it as a CSV file.
     */
    private static void fetchAndSaveStockData(OkHttpClient client, String ticker) {
        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
        .addQueryParameter("symbol", ticker)
        .addQueryParameter("interval", "1d") // Daily data
        .addQueryParameter("diffandsplits", "false")
        .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-RapidAPI-Key", API_KEY)
                .addHeader("X-RapidAPI-Host", API_HOST)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                // System.out.println("Fetched data for " + ticker);
                // System.out.println("Debugging" + responseBody);
                saveDataToCSV(ticker, responseBody);
                // System.out.println("Data for " + ticker + " saved successfully.");
            } else {
                System.err.println("Failed to fetch data for " + ticker + ": " + response.message());
            }
        } catch (IOException e) {
            System.err.println("Error fetching data for " + ticker + ": " + e.getMessage());
        }
    }

    /** 
     * Saves the fetched stock data to a CSV file.
     */
    private static void saveDataToCSV(String ticker, String jsonData) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
    
        if (!jsonObject.has("body")) {
            System.err.println("Error: Time series data not found for " + ticker);
            return;
        }
    
        JsonObject body = jsonObject.getAsJsonObject("body");
        String filePath = DATA_FOLDER + ticker + ".csv";
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            //header
            writer.write("Date,Open Price,High Price,Low Price,Close Price,Adjusted Close,Volume\n");

            // iterating over "body"
            for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
                JsonObject priceData = entry.getValue().getAsJsonObject();

                //retrieving all data and writing to file 
                String date = priceData.has("date") ? priceData.get("date").getAsString() : "";
                String open = priceData.has("open") ? priceData.get("open").getAsString() : "";
                String high = priceData.has("high") ? priceData.get("high").getAsString() : "";
                String low = priceData.has("low") ? priceData.get("low").getAsString() : "";
                String close = priceData.has("close") ? priceData.get("close").getAsString() : "";
                String adjustedClose = priceData.has("adjclose") ? priceData.get("adjclose").getAsString() : "";
                String volume = priceData.has("volume") ? priceData.get("volume").getAsString() : "";
                writer.write(String.join(",", date, open, high, low, close, adjustedClose, volume) + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error saving data for " + ticker + ": " + e.getMessage());
        }
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
}
