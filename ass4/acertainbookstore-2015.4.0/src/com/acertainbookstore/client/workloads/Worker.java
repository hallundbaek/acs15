/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {
	private WorkloadConfiguration configuration = null;
	private int numSuccessfulFrequentBookStoreInteraction = 0;
	private int numTotalFrequentBookStoreInteraction = 0;

	public Worker(WorkloadConfiguration config) {
		configuration = config;
	}

	/**
	 * Run the appropriate interaction while trying to maintain the configured
	 * distributions
	 * 
	 * Updates the counts of total runs and successful runs for customer
	 * interaction
	 * 
	 * @param chooseInteraction
	 * @return
	 */
	private boolean runInteraction(float chooseInteraction) {
		try {
			if (chooseInteraction < configuration
					.getPercentRareStockManagerInteraction()) {
				runRareStockManagerInteraction();
			} else if (chooseInteraction < configuration
					.getPercentFrequentStockManagerInteraction()) {
				runFrequentStockManagerInteraction();
			} else {
				numTotalFrequentBookStoreInteraction++;
				runFrequentBookStoreInteraction();
				numSuccessfulFrequentBookStoreInteraction++;
			}
		} catch (BookStoreException ex) {
			return false;
		}
		return true;
	}

	/**
	 * Run the workloads trying to respect the distributions of the interactions
	 * and return result in the end
	 */
	public WorkerRunResult call() throws Exception {
		int count = 1;
		long startTimeInNanoSecs = 0;
		long endTimeInNanoSecs = 0;
		int successfulInteractions = 0;
		long timeForRunsInNanoSecs = 0;

		Random rand = new Random();
		float chooseInteraction;

		// Perform the warmup runs
		while (count++ <= configuration.getWarmUpRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			runInteraction(chooseInteraction);
		}

		count = 1;
		numTotalFrequentBookStoreInteraction = 0;
		numSuccessfulFrequentBookStoreInteraction = 0;

		// Perform the actual runs
		startTimeInNanoSecs = System.nanoTime();
		while (count++ <= configuration.getNumActualRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			if (runInteraction(chooseInteraction)) {
				successfulInteractions++;
			}
		}
		endTimeInNanoSecs = System.nanoTime();
		timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
		return new WorkerRunResult(successfulInteractions,
				timeForRunsInNanoSecs, configuration.getNumActualRuns(),
				numSuccessfulFrequentBookStoreInteraction,
				numTotalFrequentBookStoreInteraction);
	}

	/**
	 * Runs the new stock acquisition interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runRareStockManagerInteraction() throws BookStoreException {
		List<StockBook> snapShot = this.configuration.getStockManager().getBooks();
		Set<Integer> bookStoreISBNS = new HashSet<>();
		for(int i = 0; i < snapShot.size(); i++) {
		  bookStoreISBNS.add(snapShot.get(i).getISBN());
		}
		Set<StockBook> potBooks = this.configuration.getBookSetGenerator().nextSetOfStockBooks(configuration.getNumBooksToAdd());
		Set<StockBook> booksToBeAdded = new HashSet<>();
		for(StockBook potBook: potBooks) {
		  if(!bookStoreISBNS.contains(potBook.getISBN())) {
		    booksToBeAdded.add(potBook);
		  }
		}
		
		this.configuration.getStockManager().addBooks(booksToBeAdded);
	}

	/**
	 * Runs the stock replenishment interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runFrequentStockManagerInteraction() throws BookStoreException {
		// TODO: Add code for Stock Replenishment Interaction
	}

	/**
	 * Runs the customer interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runFrequentBookStoreInteraction() throws BookStoreException {
		List<Book> potBooks = configuration.getBookStore().getEditorPicks(configuration.getNumEditorPicksToGet());
		Set<Integer> isbns = new HashSet<>();
		for(Book b : potBooks) {
		  isbns.add(b.getISBN());
		}
		
		Set<Integer> isbnsToBuy = configuration.getBookSetGenerator().sampleFromSetOfISBNs(isbns,configuration.getNumBooksToBuy());
		Set<BookCopy> booksToBuy = new HashSet<>();
		for(Integer isbn : isbnsToBuy) {
		  booksToBuy.add(new BookCopy(isbn, configuration.getNumBookCopiesToBuy()));
		}
		
		configuration.getBookStore().buyBooks(booksToBuy);
	}

}
