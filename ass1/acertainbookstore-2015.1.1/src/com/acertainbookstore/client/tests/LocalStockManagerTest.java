package com.acertainbookstore.client.tests;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class LocalStockManagerTest extends StockManagerTest {

  private static StockManager storeManager;
  private static BookStore client;

  public LocalStockManagerTest() {
    super(client, storeManager);
  }

  @BeforeClass
  public static void setUpBeforeClass() {
    CertainBookStore store = new CertainBookStore();
    storeManager = store;
    client = store;
    try {
      storeManager.removeAllBooks();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws BookStoreException {
    storeManager.removeAllBooks();
  }
}