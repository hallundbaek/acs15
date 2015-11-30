package com.acertainbookstore.client.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableBook;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

public class ConcurrentBookStoreTest {
  private static BookStore client;
  private static StockManager storeManager;
  private static final int TEST_ISBN = 123456;
  private static final int NUM_COPIES = 5;
  private static final int NUM_REPS = 1000;

  @BeforeClass
  public static void setUpBeforeClass() {
    try {
      ConcurrentCertainBookStore store = new ConcurrentCertainBookStore();
      storeManager = store;
      client = store;
      storeManager.removeAllBooks();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws BookStoreException {
    storeManager.removeAllBooks();
  }

  /**
   * Helper method to get the default book used by initializeBooks
   */
  public StockBook getDefaultStockBook() {
    return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit",
        "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0, false);
  }

  public Book getDefaultBook() {
    return new ImmutableBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit",
        NUM_COPIES);
  }

  /**
   * Method to add a book, executed before every test case is run
   */
  @Before
  public void initializeBooks() throws BookStoreException {
    Set<StockBook> booksToAdd = new HashSet<StockBook>();
    booksToAdd.add(getDefaultStockBook());
    storeManager.addBooks(booksToAdd);
  }

  /**
   * Method to clean up the book store, execute after every test case is run
   */
  @After
  public void cleanupBooks() throws BookStoreException {
    storeManager.removeAllBooks();
  }

  /**
   * Tests before and after atomicity with concurrent calls of book-store
   * service
   * 
   * @throws BookStoreException
   */
  @Test
  public void testBefAftAtom() throws BookStoreException {
    List<StockBook> booksInStorePreTest = storeManager.getBooks();
    Set<BookCopy> booksToBuy = new HashSet<>();
    booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

    Thread c1 = new Thread(new Test1BookClient(NUM_REPS, booksToBuy));
    Thread c2 = new Thread(new Test1StockClient(NUM_REPS, booksToBuy));

    c1.start();
    c2.start();

    try {
      c1.join();
    } catch (InterruptedException e) {
      fail();
    }
    try {
      c2.join();
    } catch (InterruptedException e) {
      fail();
    }

    List<StockBook> booksInStorePostTest = storeManager.getBooks();
    assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
        && booksInStorePreTest.size() == booksInStorePostTest.size());
  }

  /**
   * Consistency of concurrent calls of book-store service
   * 
   * @throws BookStoreException
   */
  @Test
  public void testConsistency() throws BookStoreException {
    List<StockBook> stockedSnap = storeManager.getBooks();
    Set<BookCopy> booksToBuy = new HashSet<>();
    booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));
    client.buyBooks(booksToBuy);
    List<StockBook> boughtSnap = storeManager.getBooks();
    storeManager.removeAllBooks();
    initializeBooks();
    Thread mutator = new Test2Mutator(NUM_REPS, booksToBuy);
    Thread checker = new Test2Checker(boughtSnap, stockedSnap);
    checker.start();
    mutator.start();
    try {
      mutator.join();
    } catch (InterruptedException e) {
      fail();
    }
    if (Thread.interrupted())
      fail();
    else
      checker.interrupt();
  }

  /**
   * Test that a thread is allowed to complete.
   * @throws BookStoreException 
   */
  @Test
  public void testSerilizability() throws BookStoreException {
    List<Thread> ts = new ArrayList<Thread>();
    Set<BookCopy> buyThreeBooks = new HashSet<>();
    buyThreeBooks.add(new BookCopy(TEST_ISBN, NUM_COPIES-2));
    Set<BookCopy> buyFourBooks = new HashSet<>();
    buyFourBooks.add(new BookCopy(TEST_ISBN, NUM_COPIES-1));
    Set<BookCopy> buyAllBooks = new HashSet<>();
    buyAllBooks.add(new BookCopy(TEST_ISBN, NUM_COPIES));
    
    Thread buyThree = new TestBuyer(buyThreeBooks);
    ts.add(buyThree);
    Thread buyFour = new TestBuyer(buyFourBooks);
    ts.add(buyFour);
    Thread buyAll = new TestBuyer(buyAllBooks);
    ts.add(buyAll);
    
    Collections.shuffle(ts);

    for(Thread t : ts) {
      t.start();
    }
    
    for(Thread t: ts) {
      try {
        t.join();
      } catch (InterruptedException e) {

      }
    }
        
    List<StockBook> booksInStorePostTest = storeManager.getBooks();
    StockBook book = booksInStorePostTest.get(0);
    assertTrue(book.getNumCopies() == 0 || book.getNumCopies() == 1 || book.getNumCopies() == 2);
  }

  /**
   *
   */
  
  
  
  protected class Test1BookClient extends Thread {
    volatile int reps;
    volatile Set<BookCopy> booksToBuy;

    public Test1BookClient(int reps, Set<BookCopy> booksToBuy) {
      this.reps = reps;
      this.booksToBuy = this.booksToBuy;
    }

    public void run() {
      int r = this.reps;
      while (r >= 0) {
        try {
          client.buyBooks(booksToBuy);
          r -= 1;
        } catch (BookStoreException e) {
        }
      }
    }
  }

  protected class Test1StockClient extends Thread {
    volatile int reps;
    volatile Set<BookCopy> booksToAdd;

    public Test1StockClient(int reps, Set<BookCopy> booksToAdd) {
      this.reps = reps;
      this.booksToAdd = booksToAdd;
    }

    public void run() {
      int r = this.reps;
      while (r >= 0) {
        try {
          storeManager.addCopies(booksToAdd);
          r -= 1;
        } catch (BookStoreException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  protected class Test2Mutator extends Thread {
    volatile int reps;
    volatile Set<BookCopy> booksToBuy;

    public Test2Mutator(int reps, Set<BookCopy> booksToBuy) {
      this.reps = reps;
      this.booksToBuy = booksToBuy;
    }

    public void run() {
      int r = this.reps;
      while (r > 0) {
        try {
          client.buyBooks(booksToBuy);
        } catch (BookStoreException e) {
        }
        try {
          storeManager.addCopies(booksToBuy);
        } catch (Exception e) {
        }
        r -= 1;
      }
    }
  }

  protected class Test2Checker extends Thread {
    volatile List<StockBook> boughtSnap;
    volatile List<StockBook> stockedSnap;

    public Test2Checker(List<StockBook> boughtSnap, List<StockBook> stockedSnap) {
      this.boughtSnap = boughtSnap;
      this.stockedSnap = stockedSnap;
    }

    public void run() {
      while (!Thread.interrupted()) {
        List<StockBook> snap = null;
        try {
          snap = storeManager.getBooks();
        } catch (BookStoreException ex) {

        }
        if (snap != null) {
          if (!snap.equals(boughtSnap) && !snap.equals(stockedSnap)) {
            Thread.currentThread().interrupt();
          }
          ;
        }
      }
    }
  }

  protected class TestBuyer extends Thread {
    Set<BookCopy> booksToBuy;
    
    public TestBuyer(Set<BookCopy> booksToBuy) {
      this.booksToBuy = booksToBuy;
    }
    
    public void run() {
      try {
        client.buyBooks(booksToBuy);
      } catch (BookStoreException e) {
        Thread.currentThread().interrupt();
      }
      return;
    }
  }

}
