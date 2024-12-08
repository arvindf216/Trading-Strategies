
# Trading Strategies

This repository contains implementations of popular trading strategies tested on daily data from 50 high-performing US stocks listed on Nasdaq, covering diverse sectors such as Technology, Healthcare, and Financials. The daily time series data spans the past five years and was fetched using the [YH Finance API](https://rapidapi.com/sparior/api/yahoo-finance15) hosted on RapidAPI. Detailed results and analysis for these strategies can be found in `Results and Analysis.pdf`.

---

## How to Run

To use the code, first obtain a free API key by registering on RapidAPI and subscribing to the free plan for **YH Finance**. Replace the placeholder `"your_api_key"` with your API key in the first line of the `YHFinanceDataFetcher` class, located at `trading_strategies/src/main/java/finance/YHFinanceDataFetcher.java`.

Next, make the necessary adjustments in the `MainApp.java` file (detailed steps provided below) located at `trading_strategies/src/main/java/finance`. Once the adjustments are complete, run the following Maven command from the root directory (`trading_strategies/`), which contains the `pom.xml` file:

```bash
mvn clean compile exec:java
```

Ensure Maven is installed and properly configured on your system.

---

### Steps to Execute the Code

1. **Fetch Stock Data**:
   - Run the Maven command above to fetch the stock data. The data will be stored in the `data` folder, with each stock saved as a CSV file named after its ticker (`{ticker}.csv`).

2. **Disable Redundant API Calls**:
   - After fetching the data, comment out the initial code block in `MainApp.java` above the line marked with:  
     `/* !! Once stock data is fetched, comment out the above portion !! */`.  
     This prevents repetitive API calls for already-fetched data.

3. **Select a Trading Strategy**:
   - Uncomment the code block corresponding to the desired trading strategy. For instance, to run **Trading Strategy 3**, uncomment the section starting with:  
     `// // Trading Strategy 3: based on Moving Averages, RSI, and Volume`  
     and ending with:  
     `// // Trading Strategy 3 ends here...`,
   and then run the Maven command mentioned above. Ensure only one strategy block is uncommented at a time.
