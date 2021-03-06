/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.sun.xml.internal.ws.api.ha.StickyFeature;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    int numConcurrentWorkloadThreads = 2;
        String serverAddress = "http://localhost:8081";
    boolean localTest = true;
    List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
    List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

    // Initialize the RPC interfaces if its not a localTest, the variable is
    // overriden if the property is set
    String localTestProperty = System
        .getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
    localTest = (localTestProperty != null) ? Boolean
        .parseBoolean(localTestProperty) : localTest;

    BookStore bookStore = null;
    StockManager stockManager = null;
    if (localTest) {
      CertainBookStore store = new CertainBookStore();
      bookStore = store;
      stockManager = store;
    }
    else {
      stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
      bookStore = new BookStoreHTTPProxy(serverAddress);
    }

    // Generate data in the bookstore before running the workload
    initializeBookStoreData(bookStore, stockManager);
    while (numConcurrentWorkloadThreads <= 32) {
      if (numConcurrentWorkloadThreads == 32) {
        numConcurrentWorkloadThreads = 24;
      }
      ExecutorService exec = Executors
          .newFixedThreadPool(numConcurrentWorkloadThreads);

      for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
        WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
            stockManager);
        Worker workerTask = new Worker(config);
        // Keep the futures to wait for the result from the thread
        runResults.add(exec.submit(workerTask));
      }

      // Get the results from the threads using the futures returned
      for (Future<WorkerRunResult> futureRunResult : runResults) {
        WorkerRunResult runResult = futureRunResult.get(); // blocking call
        workerRunResults.add(runResult);
      }

      exec.shutdownNow(); // shutdown the executor

      // Finished initialization, stop the clients if not localTest
      if (!localTest) {
        ((BookStoreHTTPProxy) bookStore).stop();
        ((StockManagerHTTPProxy) stockManager).stop();
      }

      reportMetric(workerRunResults);
      numConcurrentWorkloadThreads *= 2;
    }
  }

  /**
   * Computes the metrics and prints them
   * 
   * @param workerRunResults
   */
  public static void reportMetric(List<WorkerRunResult> workerRunResults) {
    // TODO: You should aggregate metrics and output them for plotting here

    double totElapsed = 0;
    double aggrThroughput = 0f;

    for (WorkerRunResult res : workerRunResults) {
      if ((float) res.getSuccessfulInteractions() / res.getTotalRuns() * 100f > 99f
          && (float) res.getTotalFrequentBookStoreInteractionRuns()
              / res.getTotalRuns() * 100 > 60f) {
        totElapsed += res.getElapsedTimeInNanoSecs() * Math.pow(10, -9);
        aggrThroughput += (double) res.getSuccessfulInteractions()
            / (res.getElapsedTimeInNanoSecs() * Math.pow(10, -9));
      }
    }

    List<String> lines = Arrays.asList("throughput=" + (int) aggrThroughput,
        "latency=" + totElapsed / workerRunResults.size());
    Path file = Paths.get("./test/dataOfflineNewNew.txt");
    try {
      Files.write(file, lines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Generate the data in bookstore before the workload interactions are run
   * 
   * Ignores the serverAddress if its a localTest
   * 
   */
  public static void initializeBookStoreData(BookStore bookStore,
      StockManager stockManager) throws BookStoreException {
    Set<StockBook> initBooks = new HashSet<>();
    for (int i = 1; i <= 9; i++) {
      initBooks.add(new ImmutableStockBook(i, "BookPrjct" + i, "MonkeyNum" + i,
          i, 10000, 0, 0, 0, true));
    }
    stockManager.addBooks(initBooks);
  }
}
