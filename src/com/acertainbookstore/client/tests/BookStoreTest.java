package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
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
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				storeManager = store;
				client = store;
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
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
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
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
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
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
	
	/**
	 * Tests rateBooks() as well as getTopRatedBooks(), as both are needed for
	 * verification anyway.
	 */
	@Test
	public void testRateBooks() throws BookStoreException {

		HashSet<StockBook> booksToAdd = new HashSet<StockBook>(2);
		booksToAdd.add(new ImmutableStockBook(
				TEST_ISBN + 1, "Elements", "Euclid", (float) 666, NUM_COPIES, 0, 0, 0,
				false));
		booksToAdd.add(new ImmutableStockBook(
				TEST_ISBN + 2, "War and peace", "Lev Tolstoy", (float) 123, NUM_COPIES,
				0, 0, 0, false));
		storeManager.addBooks(booksToAdd);

		HashSet<BookRating> validRatings = new HashSet<BookRating>(2);
		ArrayList<BookRating> invalidRatings = new ArrayList<BookRating>(2);
		validRatings.add(new BookRating(TEST_ISBN, 1));
		validRatings.add(new BookRating(TEST_ISBN + 1, 5));
		validRatings.add(new BookRating(TEST_ISBN + 2, 2));
		invalidRatings.add(new BookRating(-3, 1));
		invalidRatings.add(new BookRating(TEST_ISBN + 3, 2));
		invalidRatings.add(new BookRating(TEST_ISBN + 1, -5));

		// Positive test
		client.rateBooks(validRatings);
		List<Book> topRated = client.getTopRatedBooks(1);
		assertEquals(topRated.get(0).getISBN(), TEST_ISBN + 1);
		topRated = client.getTopRatedBooks(999);
		assertTrue(topRated.size() == 3);
		assertTrue(topRated.get(0).getISBN() == TEST_ISBN + 1);
		assertTrue(topRated.get(1).getISBN() == TEST_ISBN + 2);
		assertTrue(topRated.get(2).getISBN() == TEST_ISBN);

		// Negative test
		for (BookRating invalid : invalidRatings) {
			try {
				HashSet<BookRating> invalidIsbn = new HashSet<BookRating>(1);
				invalidIsbn.add(invalid);
				client.rateBooks(invalidIsbn);
				fail();
			} catch (BookStoreException err) {
				;
			}
		}

	}
	
	/**
	 * Tests that a single valid rating is processed correctly.
     *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
    public void testRateSingle() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.get(0).getISBN() == TEST_ISBN);

		Set<BookRating> ratingList = new HashSet<>();
		ratingList.add(new BookRating(TEST_ISBN, 5)); // Book is missing

		client.rateBooks(ratingList);
		List<StockBook> books = storeManager.getBooks();
		StockBook defaultBook = books.get(0); //Get the first and only book
		assertTrue(defaultBook.getTotalRating() == 5
				&& defaultBook.getNumTimesRated() == 1);
    }

	/**
	 * Test that multiple ratings accumulate on a book
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateMultipleTimes() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.get(0).getISBN() == TEST_ISBN);

		HashSet<BookRating> ratingList1 = new HashSet<>();
		ratingList1.add(new BookRating(TEST_ISBN, 3)); // Book is missing

		HashSet<BookRating> ratingList2 = new HashSet<>();
		ratingList2.add(new BookRating(TEST_ISBN, 2)); // Book is missing

		HashSet<BookRating> ratingList3 = new HashSet<>();
		ratingList3.add(new BookRating(TEST_ISBN, 4)); // Book is missing

		client.rateBooks(ratingList1);
		client.rateBooks(ratingList2);
		client.rateBooks(ratingList3);

		List<StockBook> books = storeManager.getBooks();
		StockBook defaultBook = books.get(0); //Get the first and only book

		assertTrue(defaultBook.getTotalRating() == 9
				&& defaultBook.getNumTimesRated() == 3);
	}

	/**
	 * Tests that books cannot be rated if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<BookRating> ratingList = new HashSet<>();
		ratingList.add(new BookRating(TEST_ISBN, 3)); // valid
		ratingList.add(new BookRating(-1, 5)); // invalid

		try {
			client.rateBooks(ratingList);
			fail("rateBooks does not fail on invalid ISBN rating");
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> books = storeManager.getBooks();
		StockBook defaultBook = books.get(0); //Get the first and only book
		// Test that no ratings have changed
		assertTrue(defaultBook.getNumTimesRated() == 0 &&
			defaultBook.getTotalRating() == 0);
	}

	/**
	 * Tests that books cannot be rated if a rating is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateInvalidRating() throws BookStoreException {
		addBooks(TEST_ISBN + 1, 5);

		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<BookRating> ratingList = new HashSet<>();
		ratingList.add(new BookRating(TEST_ISBN, 3)); // valid
		ratingList.add(new BookRating(TEST_ISBN+1, 6)); // invalid

		try {
			client.rateBooks(ratingList);
			fail("rateBooks does not fail on invalid rating");
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		// Test that no ratings have changed
	}

	/**
	 * Test that trying to rate a book not in the store causes an error.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateMissingBook() throws BookStoreException {

		HashSet<BookRating> ratingList = new HashSet<>();
		ratingList.add(new BookRating(TEST_ISBN+1, 3)); // Book is missing

		try {
			client.rateBooks(ratingList);
			fail("rateBooks does not fail on missing book");
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		// Test that no ratings have changed
	}

	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
