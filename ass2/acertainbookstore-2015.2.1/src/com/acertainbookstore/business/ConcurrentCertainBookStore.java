/**
 *
 */
package com.acertainbookstore.business;

import java.util.*;
import java.util.Map.Entry;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * ConcurrentCertainBookStore implements the bookstore and its functionality which is
 * defined in the BookStore
 */
public class ConcurrentCertainBookStore implements BookStore, StockManager {
	private Map<Integer, BookStoreBook> bookMap;
  private ConcurrentHashMap<Integer, ReadWriteLock> lockMap;
  ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private Lock readLock = readWriteLock.readLock();
  private Lock writeLock = readWriteLock.readLock();
	public ConcurrentCertainBookStore() {
		// Constructors are not synchronized
		bookMap = new HashMap<Integer, BookStoreBook>();
    lockMap = new ConcurrentHashMap<Integer, ReadWriteLock>();
	}

	public void addBooks(Set<StockBook> bookSet)
			throws BookStoreException {

		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check if all are there
    readLock.lock();
		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			String bookTitle = book.getTitle();
			String bookAuthor = book.getAuthor();
			int noCopies = book.getNumCopies();
			float bookPrice = book.getPrice();
			if (BookStoreUtility.isInvalidISBN(ISBN)
					|| BookStoreUtility.isEmpty(bookTitle)
					|| BookStoreUtility.isEmpty(bookAuthor)
					|| BookStoreUtility.isInvalidNoCopies(noCopies)
					|| bookPrice < 0.0) {
        readLock.unlock();
				throw new BookStoreException(BookStoreConstants.BOOK
						+ book.toString() + BookStoreConstants.INVALID);
			} else if (bookMap.containsKey(ISBN)) {
        readLock.unlock();
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.DUPLICATED);
			}
		}
    readLock.unlock();
    writeLock.lock();
		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			bookMap.put(ISBN, new BookStoreBook(book));
      lockMap.put(ISBN, new ReentrantReadWriteLock());
		}
    writeLock.unlock();
		return;
	}

	public void addCopies(Set<BookCopy> bookCopiesSet)
			throws BookStoreException {
		int ISBN, numCopies;

		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
    readLock.lock();
		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			if (BookStoreUtility.isInvalidISBN(ISBN)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                + BookStoreConstants.INVALID);
      }
			if (!bookMap.containsKey(ISBN)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                + BookStoreConstants.NOT_AVAILABLE);
      }
			if (BookStoreUtility.isInvalidNoCopies(numCopies))
				throw new BookStoreException(BookStoreConstants.NUM_COPIES
						+ numCopies + BookStoreConstants.INVALID);
		}
    readLock.unlock();
		BookStoreBook book;
		// Update the number of copies
    Set<Integer> isbnSet = new HashSet<>();
    for (BookCopy bookCopy : bookCopiesSet) {
      isbnSet.add(bookCopy.getISBN());
    }
    lockISBNSet(isbnSet, true);
		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			book = bookMap.get(ISBN);
			book.addCopies(numCopies);
		}
    unlockISBNSet(isbnSet, true);
	}

	public List<StockBook> getBooks() {
		List<StockBook> listBooks = new ArrayList<StockBook>();
    readLock.lock();
		Collection<BookStoreBook> bookMapValues = bookMap.values();
    readLock.unlock();
		for (BookStoreBook book : bookMapValues) {
			listBooks.add(book.immutableStockBook());
		}
		return listBooks;
	}

	public void updateEditorPicks(Set<BookEditorPick> editorPicks)
			throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		int ISBNVal;
    readLock.lock();
		for (BookEditorPick editorPickArg : editorPicks) {
			ISBNVal = editorPickArg.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBNVal)) {
        readLock.unlock();
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
						+ BookStoreConstants.INVALID);
      }
			if (!bookMap.containsKey(ISBNVal)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
                + BookStoreConstants.NOT_AVAILABLE);
      }
		}
    readLock.unlock();
    Set<Integer> isbnSet = new HashSet<>();
    for (BookEditorPick editorPickArg : editorPicks) {
      isbnSet.add(editorPickArg.getISBN());
    }
    lockISBNSet(isbnSet, true);
    for (BookEditorPick editorPickArg : editorPicks) {
      bookMap.get(editorPickArg.getISBN()).setEditorPick(
              editorPickArg.isEditorPick());
    }
    unlockISBNSet(isbnSet, true);
	}

	public void buyBooks(Set<BookCopy> bookCopiesToBuy)
			throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Check that all ISBNs that we buy are there first.
		int ISBN;
		BookStoreBook book;
		Boolean saleMiss = false;
    readLock.lock();
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
      ISBN = bookCopyToBuy.getISBN();
      if (bookCopyToBuy.getNumCopies() < 0) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.NUM_COPIES
                + bookCopyToBuy.getNumCopies()
                + BookStoreConstants.INVALID);
      }
      if (BookStoreUtility.isInvalidISBN(ISBN)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                + BookStoreConstants.INVALID);
      }
      if (!bookMap.containsKey(ISBN)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                + BookStoreConstants.NOT_AVAILABLE);
      }
    }
    readLock.unlock();

    Set<Integer> isbnSet = new HashSet<>();
    for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
      isbnSet.add(bookCopyToBuy.getISBN());
    }
    lockISBNSet(isbnSet, true);
    for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			book = bookMap.get(bookCopyToBuy.getISBN());
			if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
				book.addSaleMiss(); // If we cannot sell the copies of the book
									// its a miss
				saleMiss = true;
			}
		}

		// We throw exception now since we want to see how many books in the
		// order incurred misses which is used by books in demand
		if (saleMiss) {
      unlockISBNSet(isbnSet, true);
      throw new BookStoreException(BookStoreConstants.BOOK
              + BookStoreConstants.NOT_AVAILABLE);
    }
		// Then make purchase
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			book = bookMap.get(bookCopyToBuy.getISBN());
			book.buyCopies(bookCopyToBuy.getNumCopies());
		}
    unlockISBNSet(isbnSet, true);
	}


	public List<StockBook> getBooksByISBN(Set<Integer> isbnSet)
			throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
    readLock.lock();
		for (Integer ISBN : isbnSet) {
			if (BookStoreUtility.isInvalidISBN(ISBN)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                + BookStoreConstants.INVALID);
      }
			if (!bookMap.containsKey(ISBN)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                + BookStoreConstants.NOT_AVAILABLE);
      }
		}
    readLock.unlock();

		List<StockBook> listBooks = new ArrayList<StockBook>();

    lockISBNSet(isbnSet, false);
		for (Integer ISBN : isbnSet) {
			listBooks.add(bookMap.get(ISBN).immutableStockBook());
		}
    unlockISBNSet(isbnSet, false);

		return listBooks;
	}

	public List<Book> getBooks(Set<Integer> isbnSet)
			throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
    readLock.lock();
		for (Integer ISBN : isbnSet) {
			if (BookStoreUtility.isInvalidISBN(ISBN)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                + BookStoreConstants.INVALID);
      }
			if (!bookMap.containsKey(ISBN)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                + BookStoreConstants.NOT_AVAILABLE);
      }
		}
    readLock.unlock();

		List<Book> listBooks = new ArrayList<Book>();
    lockISBNSet(isbnSet, false);
		for (Integer ISBN : isbnSet) {
			listBooks.add(bookMap.get(ISBN).immutableBook());
		}
    unlockISBNSet(isbnSet, false);

		return listBooks;
	}

	public List<Book> getEditorPicks(int numBooks)
			throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks
					+ ", but it must be positive");
		}

		List<BookStoreBook> listAllEditorPicks = new ArrayList<BookStoreBook>();
		List<Book> listEditorPicks = new ArrayList<Book>();
    readLock.lock();
		Iterator<Entry<Integer, BookStoreBook>> it = bookMap.entrySet()
				.iterator();
		BookStoreBook book;

		// Get all books that are editor picks
		while (it.hasNext()) {
			Entry<Integer, BookStoreBook> pair = (Entry<Integer, BookStoreBook>) it
					.next();
			book = (BookStoreBook) pair.getValue();
			if (book.isEditorPick()) {
				listAllEditorPicks.add(book);
			}
		}
    readLock.unlock();

		// Find numBooks random indices of books that will be picked
		Random rand = new Random();
		Set<Integer> tobePicked = new HashSet<Integer>();
		int rangePicks = listAllEditorPicks.size();
		if (rangePicks <= numBooks) {
			// We need to add all the books
			for (int i = 0; i < listAllEditorPicks.size(); i++) {
				tobePicked.add(i);
			}
		} else {
			// We need to pick randomly the books that need to be returned
			int randNum;
			while (tobePicked.size() < numBooks) {
				randNum = rand.nextInt(rangePicks);
				tobePicked.add(randNum);
			}
		}

		// Get the numBooks random books
		for (Integer index : tobePicked) {
			book = listAllEditorPicks.get(index);
			listEditorPicks.add(book.immutableBook());
		}
		return listEditorPicks;

	}

	@Override
	public List<Book> getTopRatedBooks(int numBooks)
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	@Override
	public List<StockBook> getBooksInDemand()
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	@Override
	public void rateBooks(Set<BookRating> bookRating)
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public void removeAllBooks() throws BookStoreException {
		bookMap.clear();
	}

	public void removeBooks(Set<Integer> isbnSet)
			throws BookStoreException {

		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
    readLock.lock();
		for (Integer ISBN : isbnSet) {
			if (BookStoreUtility.isInvalidISBN(ISBN)) {
        readLock.unlock();
        throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                + BookStoreConstants.INVALID);
      }
			if (!bookMap.containsKey(ISBN))
        readLock.unlock();
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);
		}
    lockISBNSet(isbnSet, true);
		for (int isbn : isbnSet) {
			bookMap.remove(isbn);
		}
    unlockISBNSet(isbnSet, true);
    readLock.unlock();
	}

  public void lockISBNSet(Set<Integer> isbnSet, Boolean isWrite) {
    Integer[] isbnList = isbnSet.toArray(new Integer[isbnSet.size()]);
    Arrays.sort(isbnList);
    if (isWrite) {
      for (Integer isbn : isbnList) {
        lockMap.get(isbn).writeLock().lock();
      }
    } else {
      for (Integer isbn : isbnList) {
        lockMap.get(isbn).readLock().lock();
      }
    }
  }

  public void unlockISBNSet(Set<Integer> isbnSet, Boolean isWrite) {
    Integer[] isbnList = isbnSet.toArray(new Integer[isbnSet.size()]);
    Arrays.sort(isbnList);
    if (isWrite) {
      for (Integer isbn : isbnList) {
        lockMap.get(isbn).writeLock().unlock();
      }
    } else {
      for (Integer isbn : isbnList) {
        lockMap.get(isbn).readLock().unlock();
      }
    }
  }
}
