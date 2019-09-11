package opennlp.tools.ngram;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NgramDictionaryHashedTest {

  private class MockObject {

    private String[][] grams = new String[11][];
    private NgramDictionary ngramDictionary;

    public MockObject() {

      Map<String, Integer> dict = new HashMap<>();
      dict.put("A", 0);
      dict.put("B", 1);
      dict.put("C", 2);
      dict.put("D", 3);

      ngramDictionary = new NgramDictionaryHashed(dict);

      for (int i = 0; i < 4; i++) {
        ngramDictionary.add("A", "B", "C");
        ngramDictionary.add("A", "B");
        ngramDictionary.add("A");
      }

      for (int i = 0; i < 3; i++) {
        ngramDictionary.add("B", "A", "C");
        ngramDictionary.add("B", "A");
        ngramDictionary.add("B");
      }

      for (int i = 0; i < 2; i++) {
        ngramDictionary.add("B", "D", "C");
        ngramDictionary.add("B", "D");
        ngramDictionary.add("D");
      }

      ngramDictionary.add("A", "B", "A");
      ngramDictionary.add("B", "C");
      ngramDictionary.add("C");

    }

    public NgramDictionary get() {
      return ngramDictionary;
    }

  }

  private NgramDictionary ngramDictionary;

  @Before
  public void doBefore() {
    MockObject m = new MockObject();
    ngramDictionary = m.get();
  }

  @Test
  public void getCorpusSize() {
    assertEquals(ngramDictionary.getCorpusSize(), 10);
  }

  @Test
  public void getNGramCount() {
    assertEquals(4, ngramDictionary.getNGramCount(1));
    assertEquals(4, ngramDictionary.getNGramCount(2));
    assertEquals(4, ngramDictionary.getNGramCount(3));
  }

  @Test
  public void testGetNGramCount() {
    assertEquals(1, ngramDictionary.getNGramCount(1, 4, 5));
    assertEquals(2, ngramDictionary.getNGramCount(1, 3, 4));
    assertEquals(1, ngramDictionary.getNGramCount(1, 0, 1));

    assertEquals(2, ngramDictionary.getNGramCount(2, 1, 2));
    assertEquals(1, ngramDictionary.getNGramCount(2, 0, 1));
    assertEquals(0, ngramDictionary.getNGramCount(2, 5, 6));

    assertEquals(3, ngramDictionary.getNGramCount(3, 2, 5));
    assertEquals(1, ngramDictionary.getNGramCount(3, 0, 1));
  }

  @Test
  public void getSiblingCount() {
    assertEquals(4, ngramDictionary.getSiblingCount("A"));
    assertEquals(4, ngramDictionary.getSiblingCount("F"));
    assertEquals(3, ngramDictionary.getSiblingCount("B", "C"));
    assertEquals(0, ngramDictionary.getSiblingCount("D", "B", "A"));
    assertEquals(0, ngramDictionary.getSiblingCount("A", "F", "B"));
  }

  @Test
  public void testGetSiblingCount() {
    assertEquals(4, ngramDictionary.getSiblingCount(new String[] {"A"}, 0, 1));
    assertEquals(4, ngramDictionary.getSiblingCount(new String[] {"B", "F"}, 1, 2));
    assertEquals(3, ngramDictionary.getSiblingCount(new String[] {"B", "C", "A"}, 0, 2));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"D", "B", "A"}, 0, 3));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"A", "F", "B"}, 0, 3));
  }

  @Test
  public void testGetSiblingCount1() {
    assertEquals(1, ngramDictionary.getSiblingCount(new String[] {"A"}, 0, 1, 2, 2));
    assertEquals(4, ngramDictionary.getSiblingCount(new String[] {"B", "F"}, 1, 2, 1, 5));
    assertEquals(3, ngramDictionary.getSiblingCount(new String[] {"B", "C", "A"}, 0, 2, 0, 5));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"D", "B", "A"}, 0, 3, 0, 0));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"A", "F", "B"}, 0, 3, 0, 5));

    assertEquals(1, ngramDictionary.getSiblingCount(new String[] {"A", "B", "F"}, 0, 3, 1, 1));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"A", "B", "F"}, 0, 3, 2, 2));
    assertEquals(1, ngramDictionary.getSiblingCount(new String[] {"A", "B", "F"}, 0, 3, 3,
        Integer.MAX_VALUE));
  }

  @Test
  public void get() {
    assertEquals(4, ngramDictionary.get("A", "B"));
    assertEquals(0, ngramDictionary.get("F"));
    assertEquals(0, ngramDictionary.get("A", "F", "B"));
  }

  @Test
  public void testGet() {
    assertEquals(4, ngramDictionary.get(new String[] {"A", "B", "F"}, 0, 1));
    assertEquals(0, ngramDictionary.get(new String[] {"A", "F", "B"}, 0, 2));
    assertEquals(1, ngramDictionary.get(new String[] {"A", "B", "C"}, 1, 3));
  }

  @Test
  public void add() {
    ngramDictionary.add(new String[] {"A", "B", "F"});
    assertEquals(ngramDictionary.get(new String[] {"A", "B", "F"}, 0, 3), 1);
    assertEquals(ngramDictionary.get(new String[] {"A", "F", "B"}, 0, 2), 0);
  }

  @Test
  public void testAdd() {
    ngramDictionary.add(new String[] {"A", "B", "F"}, 0, 3);
    assertEquals(1, ngramDictionary.get(new String[] {"A", "B", "F"}, 0, 3));
    assertEquals(0, ngramDictionary.get(new String[] {"F"}, 0, 1));
    assertEquals(0, ngramDictionary.get(new String[] {"A", "F", "B"}, 0, 2));
  }


}
