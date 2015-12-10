package com.acertainbookstore.client.workloads;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

  private static final Random ran = new Random();
  private static final char[] chars;
  private int titleLength;
  private int authorLength;
  
  static {
    StringBuilder tmp = new StringBuilder();
    for(char c = '0'; c <= '9'; c++) {
      tmp.append(c);
    }
    for(char c = 'a'; c <= 'z';c++) {
      tmp.append(c);
      tmp.append(Character.toUpperCase(c));
    }
    chars = tmp.toString().toCharArray();
  }
  
  public BookSetGenerator(int titleLength, int authorLength) {
    this.titleLength = titleLength;
    this.authorLength = authorLength;
  }

  /**
   * Returns num randomly selected isbns from the input set
   * 
   * Combination of negative algorithm and
   * 
   * @param num
   * @return
   */
  public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
    List<Integer> l = new ArrayList<>(isbns);
    Collections.shuffle(l);
    return new HashSet<Integer>(l.subList(0, num));
  }

  /**
   * Return num stock books. For now return an ImmutableStockBook
   * 
   * @param num
   * @return
   */
  public Set<StockBook> nextSetOfStockBooks(int num) {
    Set<StockBook> res = new HashSet<>();
    while (res.size() < num) {
      char[] author = new char[this.authorLength];
      char[] title = new char[this.titleLength];
      for(int i = 0; i < authorLength; i++) {
        author[i] = chars[ran.nextInt(chars.length)];
      }
      for(int i = 0; i < titleLength; i++) {
        title[i] = chars[ran.nextInt(chars.length)];
      }
      res.add(new ImmutableStockBook(ran.nextInt(Integer.MAX_VALUE-1)+1, title.toString(), author.toString(), 1, 100,
          0, 0, 0, true));
    }
    return res;
  }

}
