package finance;

import okhttp3.OkHttpClient;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.io.File;

public class MainApp {
    public static void main(String[] args) {

        System.out.println("Fetching historical data for 50 stocks...");

        String[] tickers = {
            // Technology (10 stocks)
            "AAPL", "MSFT", "NVDA", "GOOGL", "AMZN", "META", "ADBE", "TSLA", "ORCL", "AVGO",
            // Consumer Discretionary (7 stocks)
            "HD", "TSLA", "MCD", "NKE", "SBUX", "BKNG", "TGT",
            // Healthcare (7 stocks)
            "UNH", "JNJ", "PFE", "ABBV", "LLY", "TMO", "AMGN",
            // Financials (7 stocks)
            "JPM", "BAC", "GS", "MS", "V", "MA", "BLK",
            // Industrials (7 stocks)
            "BA", "CAT", "GE", "HON", "DE", "UNP", "LMT",
            // Energy (5 stocks)
            "XOM", "CVX", "COP", "SLB", "PSX",
            // Consumer Staples (4 stocks)
            "PG", "KO", "PEP", "COST",
            // Utilities (3 stocks)
            "NEE", "DUK", "SO"
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        try {
            YHFinanceDataFetcher.fetchStockData(client, tickers);
        } finally {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            if (client.cache() != null) {
                try {
                    client.cache().close();
                } catch (IOException e) {
                    System.err.println("Error closing cache: " + e.getMessage());
                }
            }
        }



        /* !! Once stock data is fetched, comment out the above portion !! */



        final String dataFolder = "data/";
        File folder = new File(dataFolder);
        // deleting old processed files
        File[] filesToDelete = folder.listFiles((dir, name) -> name.matches("^[a-z].*"));
        if (filesToDelete != null) {
            for (File file : filesToDelete) {
                if (file.delete()) {
                    // System.out.println("Deleted file: " + file.getName());
                } else {
                    // System.err.println("Failed to delete file: " + file.getName());
                }
            }
        }
        // processing stock data
        File[] csvFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles != null) {
            for (File csvFile : csvFiles) {
                String fileName = csvFile.getName();
                YHFinanceDataFetcher.addReturnsColumn(fileName);
            }
        }


        /* !! Trading Strategies !! */


        // // Trading Strategy 1 : based on Moving Averages

        // int shortWindow = 5; // Example short window size
        // int longWindow = 20; // Example long window size
        // File[] processedFiles = folder.listFiles((dir, name) -> name.startsWith("processed_") && name.endsWith(".csv"));
        // if (processedFiles != null) {
        //     for (File csvFile : processedFiles) {
        //         TradingStrategy1.applyMovingAverageStrategy(csvFile.getName(), shortWindow, longWindow);
        //     }
        // }

        // // Trade on Strategy
        // Backtester.TradeOnStrategy();

        // // Trading Strategy 1 ends here... 
        
        

        // // Trading Strategy 2 : based on Exponential Moving Averages

        // int shortWindow = 5; // Example short window size
        // int longWindow = 20; // Example long window size
        // File[] processedFiles = folder.listFiles((dir, name) -> name.startsWith("processed_") && name.endsWith(".csv"));
        // if (processedFiles != null) {
        //     for (File csvFile : processedFiles) {
        //         TradingStrategy2.applyEmaStrategy(csvFile.getName(), shortWindow, longWindow);
        //     }
        // }

        // // Trade on Strategy
        // Backtester.TradeOnStrategy();

        // // Trading Strategy 2 ends here... 



        // // Trading Strategy 3 : based on Moving Averages, RSI, and Volume

        // int shortWindow = 5; // Example short window size
        // int longWindow = 20; // Example long window size
        // int rsiWindow = 14; // Example rsi window size
        // int volumeWindow = 20; // Example volume window size
        // File[] processedFiles = folder.listFiles((dir, name) -> name.startsWith("processed_") && name.endsWith(".csv"));
        // if (processedFiles != null) {
        //     for (File csvFile : processedFiles) {
        //         TradingStrategy3.applyCustomStrategy(csvFile.getName(), shortWindow, longWindow, rsiWindow, volumeWindow);
        //     }
        // }

        // // Trade on Strategy
        // Backtester.TradeOnStrategy();

        // // Trading Strategy 3 ends here...



        // // Trading Strategy 4 : based on EMA, RSI, and Volume

        // int shortWindow = 5; // Example short window size
        // int longWindow = 20; // Example long window size
        // int rsiWindow = 14; // Example rsi window size
        // int volumeWindow = 20; // Example volume window size
        // File[] processedFiles = folder.listFiles((dir, name) -> name.startsWith("processed_") && name.endsWith(".csv"));
        // if (processedFiles != null) {
        //     for (File csvFile : processedFiles) {
        //         TradingStrategy4.applyCustomStrategy(csvFile.getName(), shortWindow, longWindow, rsiWindow, volumeWindow);
        //     }
        // }

        // // Trade on Strategy
        // Backtester.TradeOnStrategy();

        // // Trading Strategy 4 ends here...


        
        // // Fibonacci Retracement strategy

        // int period = 10; 
        // int atrPeriod = 7; 
        // double initialBalance = 100000;
        // double buyFullBelow = 0.5;
        // double buyHalfAbove = 0.25;
        // double sellFullAbove = 0.2;
        // double sellHalfAbove = 0.1;
        // double atrVolatilityThreshold = 0.2;
        // double stopLossPercent = 0.02;
        // double maxPositionSize = 0.05;

        // File[] processedFiles = folder.listFiles((dir, name) -> name.startsWith("processed_") && name.endsWith(".csv"));
        // if (processedFiles != null) {
        //     for (File csvFile : processedFiles) {
        //         FibStrategy.applyFibStrategy(csvFile.getName(), period, atrPeriod);
        //     }
        // }

        // FibBacktester.TradeOnStrategy(initialBalance, buyFullBelow, buyHalfAbove, sellFullAbove, sellHalfAbove, 
        //                                 atrVolatilityThreshold, stopLossPercent, maxPositionSize);

        // // Fibonacci retracement strategy ends here... 

    }
}