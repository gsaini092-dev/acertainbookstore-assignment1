package com.acertainbookstore.business;
import java.util.Comparator;

public class RatingsComparator implements Comparator<BookStoreBook> {

  public int compare(BookStoreBook book1, BookStoreBook book2) {

    if (book1.getAverageRating() > book2.getAverageRating()) {
      return 1;
    }

    else if (book1.getAverageRating() < book2.getAverageRating()) {
      return -1;
    }

    else {
      return 0;
    }

  }

  public boolean equals(BookStoreBook book1, BookStoreBook book2) {

    float epsilon = 0.000001f;
    float delta = Math.abs(book1.getAverageRating() - book2.getAverageRating());

    if (delta < epsilon) {
      return true;
    }

    else {
      return false;
    }

  }

}