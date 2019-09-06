package opennlp.tools.ngram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NgramDictionaryCompressed extends NgramDictionaryHashed {

  private final List[][] levels;
  private final List[] compressedLevels;
  private final int[][] decompressionL;
  private final List[] counts;

  public NgramDictionaryCompressed(int ngram, Map<String, Integer> dictionary) {

    super(ngram, dictionary);

    //Create the structure for compression, leave it empty for now
    levels = new List[NUMBER_OF_LEVELS][2];
    compressedLevels = new List[NUMBER_OF_LEVELS];
    decompressionL = new int[NUMBER_OF_LEVELS][2];
    counts = new List[NUMBER_OF_LEVELS];
    for (int n = 0; n < NUMBER_OF_LEVELS; n++) {
      List gramIDs = new ArrayList();
      List pointers = new ArrayList();
      pointers.add(0);  //set the start & end pointers
      List[] currentLevel = new List[] {gramIDs, pointers};
      levels[n] = currentLevel;
      counts[n] = new ArrayList();
    }
  }

  @Override
  public int get(String... gram) {
    return get(gram, 0, gram.length);
  }

  @Override
  public int get(String[] gram, int start, int end) {

    Integer childNode, currentToken = wordToID.get(gram[start]);
    if (currentToken == null) {
      return 0;
    }

    childNode = currentToken;

    //todo: get the compression working
    int startIndex = (int) levels[0][1].get(childNode);
    int endIndex = (int) levels[0][1].get(childNode + 1);

    for (int i = 1; i < end - start; i++) {

      currentToken = wordToID.get(gram[i + start]);
      if (endIndex - startIndex == 0) {
        return 0;
      }

      childNode = findIndex(levels[i][0], currentToken, startIndex, endIndex);
      if (childNode - startIndex == -1) {
        return 0;
      }

      startIndex = (int) levels[i][1].get(childNode);
      endIndex = (int) levels[i][1].get(childNode + 1);
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
    for (int i = 0; i < counts[0].size(); i++) {
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
    return levels[gramSize][0].size();
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

  private int findIndex(List<Integer> ls, Integer key, Integer low, Integer high) {

    if (low > high) {
      return -1;
    }
    if (high - low == 1 && ls.get(low) == key) {
      return low;
    } else if (high - low == 1) {
      return high;
    }

    int median = (low + high) / 2;

    if (ls.get(median) == key) {
      return median;
    } else if (ls.get(median) < key) {
      return findIndex(ls, key, median, high);
    } else {
      return findIndex(ls, key, low, median);
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
    levels[level][0].add(child.getId());

    levels[level][1].add((int) levels[level][1].get(levels[level][1].size() - 1) + childCount);
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
      for (int j = 0; j < levels[i][0].size() - 1; j++) {
        rep += IDToWord.get(levels[i][0].get(j)) + ", ";
//        rep += levels[i][0].get(j) + ", ";
      }
      rep += IDToWord.get(levels[i][0].get(levels[i][0].size() - 1)) + "]";
      rep += "\n";
      rep += "PN: " + levels[i][1].toString() + "\n";
      rep += "CN: " + counts[i].toString() + "\n";
    }

    return rep;
  }
}
