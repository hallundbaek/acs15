package com.acertainbookstore.client.tests;

import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.server.BookStoreHTTPServer;
import com.acertainbookstore.utils.BookStoreException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class RPCBookStoreTest extends BookStoreTest {

  private static StockManager storeManager;
  private static BookStore client;
  private static Thread bookStoreHTTPServer;

  public RPCBookStoreTest() {
    super(client, storeManager);
  }

  @BeforeClass
  public static void setUpBeforeClass() {
    try {
      bookStoreHTTPServer = new Thread(new BookStoreHTTPServer());
      bookStoreHTTPServer.start();
      storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
      client = new BookStoreHTTPProxy("http://localhost:8081");
      storeManager.removeAllBooks();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("deprecation")
  @AfterClass
  public static void tearDownAfterClass() throws BookStoreException {
    storeManager.removeAllBooks();
    ((BookStoreHTTPProxy) client).stop();
    ((StockManagerHTTPProxy) storeManager).stop();
    bookStoreHTTPServer.stop();
  }
}