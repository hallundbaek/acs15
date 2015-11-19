package com.acertainbookstore.business;

import java.util.Comparator;

class RateComparator implements Comparator<BookStoreBook> {

	@Override
	public int compare(BookStoreBook b1, BookStoreBook b2) {
		Float b = new Float(b1.getAverageRating());
		return b.compareTo(new Float(b2.getAverageRating()));
	}

}
