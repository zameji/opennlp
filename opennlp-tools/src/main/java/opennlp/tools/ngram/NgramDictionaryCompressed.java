package opennlp.tools.ngram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NgramDictionaryCompressed extends NgramDictionaryHashed {

  private final List[] gramIDs;
  private final List[] pointers;
  //  private final List[] compressedLevels;
//  private final int[][] decompressionL;
  private final List[] counts;

  public NgramDictionaryCompressed(int ngram, Map<String, Integer> dictionary) {

    super(ngram, dictionary);

    //Create the structure for compression, leave it empty for now
    gramIDs = new List[NUMBER_OF_LEVELS];
    pointers = new List[NUMBER_OF_LEVELS];
//    compressedLevels = new List[NUMBER_OF_LEVELS];
//    decompressionL = new int[NUMBER_OF_LEVELS][2];
    counts = new List[NUMBER_OF_LEVELS];

    for (int n = 0; n < NUMBER_OF_LEVELS; n++) {
      List<Integer> currentGramIDs = new ArrayList<Integer>();
      List<Integer> currentPointers = new ArrayList<Integer>();
      currentPointers.add(0);  //set the start & end pointers

      gramIDs[n] = currentGramIDs;
      pointers[n] = currentPointers;
      counts[n] = new ArrayList<Integer>();
    }
  }

  @Override
  public int get(String... gram) {
    return get(gram, 0, gram.length);
  }

  @Override
  public int get(String[] gram, int start, int end) {
    Integer childNode = 0, currentToken = 0;

    currentToken = wordToID.get(gram[start]);
    if (currentToken == null) {
      return 0;
    }

    childNode = currentToken;

    //todo: get the compression working
    int startIndex = (int) pointers[0].get(childNode);
    int endIndex = (int) pointers[0].get(childNode + 1);

    for (int i = 1; i < end - start; i++) {

      currentToken = wordToID.get(gram[i + start]);

      //ngram not over, but no children recorded
      if (endIndex - startIndex == 0 || currentToken == null) {
        return 0;
      }

      childNode = findIndex(gramIDs[i], currentToken, startIndex, endIndex);
      if (childNode == -1) {
        return 0;
      }

      if (i < end - start - 1) {
        startIndex = (int) pointers[i].get(childNode);
        endIndex = (int) pointers[i].get(childNode + 1);
      }
    }
    return (int) counts[end - start - 1].get(childNode);

  }

  /**
   * Get the size of the corpus
   *
   * @return The size
   */
  @Override
  public int getCorpusSize() {
    int size = 0;
    int lemmaCount = counts[0].size();
    for (int i = 0; i < lemmaCount; i++) {
      size += (int) counts[0].get(i);
    }
    return size;
  }

  /**
   * Get the number of various ngram of a given size
   *
   * @param gramSize The size of the ngram (i.e. n)
   * @return The number of various ngrams of this size
   */
  @Override
  public int getNGramCount(int gramSize) {
    return gramIDs[gramSize].size();
  }

  /**
   * Get the number of various ngram of a given size and frequency
   *
   * @param gramSize  The size of the ngram (i.e. n)
   * @param frequency The frequency with which the ngram should occur
   * @return The number of various ngrams of this size
   */
  @Override
  public int getNGramCount(int gramSize, int frequency) {
    int totalCount = 0;
    for (int i = 0; i < counts[gramSize - 1].size(); i++) {
      int currentCount = (int) counts[gramSize - 1].get(i);
      if (currentCount == frequency) {
        totalCount++;
      }

    }
    return totalCount;
  }

/*
  Once compression works, we'll need to search in arrays, too
  private int findIndex(int[] ls, Integer key, Integer low, Integer high) {

    if (low > high) {
      return -1;
    }
    if (high - low == 1 && ls[low] == key) {
      return low;
    } else if (high - low == 1) {
      return high;
    }

    int median = (low + high) / 2;
    if (ls[median] == key) {
      return median;
    } else if (ls[median] < key) {
      return findIndex(ls, key, median, high);
    } else {
      return findIndex(ls, key, low, median);
    }

  }
*/

  private int findIndex(List<Integer> ls, Integer key, Integer low, Integer high) {


    int middle = (low + high) / 2;
    if (high < low) {
      return -1;
    }

    if (key == (int) ls.get(middle)) {
      return middle;
    } else if (key < (int) ls.get(middle)) {
      return findIndex(
          ls, key, low, middle);
    } else {
      return findIndex(
          ls, key, middle + 1, high);
    }

  }

  private void addChild(DictionaryNode child, int level) {
    if (level >= NUMBER_OF_LEVELS) {
      return;
    }

    //start at the first node in the vocabulary that is present
    List<DictionaryNode> currentNodeChildren = child.getChildren();
    Collections.sort(currentNodeChildren);

    int childCount = currentNodeChildren.size();
    //This may lead to Integer overflow, use BigInteger?
//    int previousId = (levels[level][0].size() > 0) ?
//        (int) levels[level][0].get(levels[level][0].size() - 1) : 0;

    gramIDs[level].add(child.getId());
    pointers[level].add((int) pointers[level].get(pointers[level].size() - 1) + childCount);
    counts[level].add(child.getCount());

    for (DictionaryNode currentChild : currentNodeChildren) {
      addChild(currentChild, level + 1);
    }
  }

  /**
   * Translate the dictionary from a hashed one into the compressed form.
   * This incurs some penalty on the speed, but improves memory footprint.
   * It can be only used once the dictionary is static, i.e. no more children
   * will be added.
   */
  public void compress() {

    DictionaryNode root = super.getRoot();
    List<DictionaryNode> currentNodeChildren = root.getChildren();
    Collections.sort(currentNodeChildren);
    for (DictionaryNode child : currentNodeChildren) {
      addChild(child, 0);
      root.removeChild(child.getId());
    }
    super.removeRoot();

    //todo: get the compression working
//
//    //compress the individual levels
//    for (int n=0; n<NUMBER_OF_LEVELS;n++){
//      compressedLevels[n] = new ArrayList();
//      decompressionL[n] = new int[2];
//      for (int i=0; i<2; i++){
//        List<Integer> temp = levels[n][i];
//        int[] compressible = temp.stream().mapToInt(k -> k).toArray();
//        byte[] compressed = EliasFano.compress(compressible, 0, levels[n][i].size());
//        compressedLevels[n].add(compressed);
//        decompressionL[n][i] = EliasFano.getL(compressible[compressible.length-1],
//            compressible.length);
//        System.out.println("Raw length: "+compressible.length);
//        System.out.println("Compressed length: "+compressed.length);
//        levels[n][i] = new ArrayList();
//      }
//    }

  }

  public String toString() {
    Map<Integer, String> IDToWord = new HashMap<>();
    for (Map.Entry<String, Integer> w : wordToID.entrySet()) {
      IDToWord.put(w.getValue(), w.getKey());
    }
    String rep = "";
    for (int i = 0; i < NUMBER_OF_LEVELS; i++) {
      rep += "CH: [";
      for (int j = 0; j < gramIDs[i].size() - 1; j++) {
        rep += IDToWord.get(gramIDs[i].get(j)) + ", ";
//        rep += levels[i][0].get(j) + ", ";
      }
      rep += IDToWord.get(gramIDs[i].get(gramIDs[i].size() - 1)) + "]";
      rep += "\n";
      rep += "PN: " + pointers[i].toString() + "\n";
      rep += "CN: " + counts[i].toString() + "\n";
    }

    return rep;
  }
}
