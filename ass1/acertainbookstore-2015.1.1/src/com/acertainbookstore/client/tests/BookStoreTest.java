package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.ImmutableBook;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * Test class to test the BookStore interface
 * 
 */
public abstract class BookStoreTest {

	private static final int TEST_ISBN = 3044560;
	private static final int TEST_ISBN_2 = 305000;
	private static final int NUM_COPIES = 5;
	private StockManager storeManager;
	private BookStore client;

  protected BookStoreTest(BookStore client, StockManager storeManager) {
    this.client = client;
    this.storeManager = storeManager;
  }


	/**
	 * Helper method to add some books
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones",
				"George RR Testin'", (float) 10, copies, 0, 4, 12, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit",
				"JK Unit", (float) 10, NUM_COPIES, 0, 0, 0, false);
	}

	/**
	 * Helper method to get the default book used by initializeBooks
	 */
	public StockBook getDefaultSecondBook() {
		return new ImmutableStockBook(TEST_ISBN_2, "BooksTitle", "Some",
				(float) 10, NUM_COPIES, 0, 2, 10, false);
	}

	/**
	 * Method to add a book, executed before every test case is run
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		booksToAdd.add(getDefaultSecondBook());
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
	 * Tests basic buyBook() functionality
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));
		booksToBuy.add(new BookCopy(TEST_ISBN_2, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 2);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();
		assertTrue(bookInList.getISBN() == addedBook.getISBN()
				&& bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor())
				&& bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getSaleMisses() == addedBook.getSaleMisses()
				&& bookInList.getAverageRating() == addedBook
						.getAverageRating()
				&& bookInList.getTimesRated() == addedBook.getTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());

	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid isbn
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		// Check pre and post state are same
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Test that only books that are in the store can be rated.
	 */
	@Test
	public void testRateNoneExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to rate a book with invalid isbn
		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 1)); // valid
		booksToRate.add(new BookRating(TEST_ISBN - 2, 1)); // invalid

		// Try to rate the books
		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		// Check pre and post state are same
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Test only accept valid ISBN
	 */
	@Test
	public void testRateNonsenseISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to rate a book with invalid isbn
		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 1)); // valid
		booksToRate.add(new BookRating('a', 1)); // invalid

		// Try to rate the books
		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		// Check pre and post state are same
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Test only valid ratings can be given.
	 * 
	 * @throws BookStoreException
	 */
	@Test
	public void testGiveInvalidRating() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to rate a book with too high rating
		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN_2,
				BookStoreConstants.MAX_RATING)); // valid
		booksToRate.add(new BookRating(TEST_ISBN,
				BookStoreConstants.MAX_RATING + 1)); // invalid

		// Try to buy the books
		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		// Try to rate a book with a too low rating
		booksToRate.clear();
		booksToRate.add(new BookRating(TEST_ISBN_2,
				BookStoreConstants.MIN_RATING)); // valid
		booksToRate.add(new BookRating(TEST_ISBN,
				BookStoreConstants.MIN_RATING - 1)); // invalid

		// Try to rate the books
		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		// Check pre and post state are same
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Test that rating books aggregates their total rating.
	 * 
	 * @throws BookStoreException
	 */
	@Test
	public void testGiveValidRating() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();
		addBooks(TEST_ISBN - 1, 2);

		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN - 1,
				BookStoreConstants.MAX_RATING - 1)); // add 4

		// Try to rate the books
		try {
			client.rateBooks(booksToRate);

		} catch (BookStoreException ex) {
			fail();
		}

		booksToRate.clear();
		booksToRate.add(new BookRating(TEST_ISBN - 1,
				BookStoreConstants.MIN_RATING + 2)); // add 2

		// Try to rate the books
		try {
			client.rateBooks(booksToRate);

		} catch (BookStoreException ex) {
			fail();
		}

		// check that the added book has been rated and is in the store
		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		Set<Integer> addedISBN = new HashSet<>();
		addedISBN.add(TEST_ISBN - 1);
		StockBook addedBook = storeManager.getBooksByISBN(addedISBN).get(0);

		assertTrue(booksInStorePostTest.contains(addedBook)
				&& addedBook.getAverageRating() == (float) 3
				&& addedBook.getTimesRated() == 6
				&& addedBook.getTotalRating() == 18);

		booksInStorePostTest.remove(addedBook);

		// Check pre and post state are same after removing rated book
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Test that we cannot ask for less than one top rated books.
	 * 
	 * @throws BookStoreException
	 */
	@Test
	public void testTopRatedNegativeNumBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy the books
		try {
			client.getTopRatedBooks(-1);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same after removing rated book
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	private ImmutableBook makeImmutatableBook(StockBook b) {
		return new ImmutableBook(b.getISBN(), b.getTitle(), b.getAuthor(),
				b.getPrice());
	}

	/**
	 * Test that we get k highest rated books
	 */
	@Test
	public void testKTopRated() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();
		List<Book> kTopRated = null;
		List<Book> topRated = new ArrayList<>();
		addBooks(TEST_ISBN - 1, NUM_COPIES);

		// Try to buy the books
		try {
			kTopRated = client.getTopRatedBooks(1);
		} catch (BookStoreException ex) {
			fail();
		}

		topRated.add(makeImmutatableBook(getDefaultSecondBook()));

		assertTrue(kTopRated.size() == 1 && kTopRated.containsAll(topRated));

		// Try to buy the books
		try {
			kTopRated = client.getTopRatedBooks(2);
		} catch (BookStoreException ex) {
			fail();
		}

		topRated.add(new ImmutableBook(TEST_ISBN-1, "Test of Thrones",
				"George RR Testin'", (float) 10));

		assertTrue(kTopRated.size() == 2 && kTopRated.containsAll(topRated));

		// Try to buy the books
		try {
			kTopRated = client.getTopRatedBooks(3);
		} catch (BookStoreException ex) {
			fail();
		}

		topRated.add(makeImmutatableBook(getDefaultBook()));

		assertTrue(kTopRated.size() == 3 && kTopRated.containsAll(topRated));

		
		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		Set<Integer> addedISBN = new HashSet<>();
		addedISBN.add(TEST_ISBN - 1);
		StockBook addedBook = storeManager.getBooksByISBN(addedISBN).get(0);
		booksInStorePostTest.remove(addedBook);
		
		// Check pre and post state are same after removing rated book
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Test that all books are given if more that all books in the store are
	 * requested
	 * @throws BookStoreException 
	 */
	public void testAllTopRatedBook() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();
		List<Book> kTopRated = null;
		List<Book> topRated = new ArrayList<>();
		addBooks(TEST_ISBN - 1, NUM_COPIES);

		// Try to buy the books
		try {
			kTopRated = client.getTopRatedBooks(7);
		} catch (BookStoreException ex) {
			fail();
		}

		topRated.add(makeImmutatableBook(getDefaultSecondBook()));
		topRated.add(makeImmutatableBook(getDefaultBook()));
		assertTrue(kTopRated.size() == 2 && kTopRated.containsAll(topRated));

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with isbn which does not exist
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		// Check pre and post state are same
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());

	}

	/**
	 * Tests that you can't buy more books than there are copies
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());

	}

	/**
	 * Tests that you can't buy a negative number of books
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());

	}

	/**
	 * Tests that all books can be retrieved
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());
		booksAdded.add(getDefaultSecondBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1,
				"The Art of Computer Programming", "Donald Knuth", (float) 300,
				NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2,
				"The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES,
				0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other
		assertTrue(listBooks.containsAll(booksAdded)
				&& listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1,
				"The Art of Computer Programming", "Donald Knuth", (float) 300,
				NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2,
				"The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES,
				0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN

		List<Book> books = client.getBooks(isbnList);
		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd)
				&& books.size() == booksToAdd.size());

	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());

	}
}

