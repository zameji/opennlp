package opennlp.tools.ngram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NgramDictionaryCompressed extends NgramDictionaryHashed {

  private final int[][] gramIDsArrayRaw;
  private final int[][] pointersArrayRaw;
  private final int[][] countsArrayRaw;

  public NgramDictionaryCompressed(int ngram, Map<String, Integer> dictionary) {

    super(ngram, dictionary);

    //Create the structure for compression, leave it empty for now

    gramIDsArrayRaw = new int[NUMBER_OF_LEVELS][];
    pointersArrayRaw = new int[NUMBER_OF_LEVELS][];
    countsArrayRaw = new int[NUMBER_OF_LEVELS][];
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
    int startIndex = (int) pointersArrayRaw[0][childNode];
    int endIndex = (int) pointersArrayRaw[0][childNode + 1];

    for (int i = 1; i < end - start; i++) {

      currentToken = wordToID.get(gram[i + start]);

      //ngram not over, but no children recorded
      if (endIndex - startIndex == 0 || currentToken == null) {
        return 0;
      }

      childNode = findIndex(gramIDsArrayRaw[i], currentToken, startIndex, endIndex);
      if (childNode == -1) {
        return 0;
      }

      if (i < end - start - 1) {
        startIndex = (int) pointersArrayRaw[i][childNode];
        endIndex = (int) pointersArrayRaw[i][childNode + 1];
      }
    }
    return (int) countsArrayRaw[end - start - 1][childNode];

  }

  /**
   * Get the size of the corpus
   *
   * @return The size
   */
  @Override
  public int getCorpusSize() {
    int size = 0;
    int lemmaCount = countsArrayRaw[0].length;
    for (int i = 0; i < lemmaCount; i++) {
      size += (int) countsArrayRaw[0][i];
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
    return gramIDsArrayRaw[gramSize].length;
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
    for (int i = 0; i < countsArrayRaw[gramSize - 1].length; i++) {
      int currentCount = countsArrayRaw[gramSize - 1][i];
      if (currentCount == frequency) {
        totalCount++;
      }

    }
    return totalCount;
  }

  private int findIndex(int[] arr, Integer key, Integer low, Integer high) {

    int middle = (low + high) / 2;
    if (high < low) {
      return -1;
    }

    if (key == arr[middle]) {
      return middle;
    } else if (key < arr[middle]) {
      return findIndex(
          arr, key, low, middle);
    } else {
      return findIndex(
          arr, key, middle + 1, high);
    }

  }

  private void addChild(DictionaryNode child, int level, List[] gramIDsList, List[] pointersList,
                        List[] countsList) {
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

    gramIDsList[level].add(child.getId());
    pointersList[level].add((int) pointersList[level].get(pointersList[level].size() - 1) + childCount);
    countsList[level].add(child.getCount());

    for (DictionaryNode currentChild : currentNodeChildren) {
      addChild(currentChild, level + 1, gramIDsList, pointersList,
          countsList);
    }
  }

  /**
   * Translate the dictionary from a hashed one into the compressed form.
   * This incurs some penalty on the speed, but improves memory footprint.
   * It can be only used once the dictionary is static, i.e. no more children
   * will be added.
   */
  public void compress() {

    List[] gramIDsList = new List[NUMBER_OF_LEVELS];
    List[] pointersList = new List[NUMBER_OF_LEVELS];
    List[] countsList = new List[NUMBER_OF_LEVELS];

    for (int n = 0; n < NUMBER_OF_LEVELS; n++) {
      List<Integer> currentGramIDs = new ArrayList<Integer>();
      List<Integer> currentPointers = new ArrayList<Integer>();
      currentPointers.add(0);  //set the start & end pointers

      gramIDsList[n] = currentGramIDs;
      pointersList[n] = currentPointers;
      countsList[n] = new ArrayList<Integer>();

    }

    DictionaryNode root = super.getRoot();
    List<DictionaryNode> currentNodeChildren = root.getChildren();
    Collections.sort(currentNodeChildren);


    for (DictionaryNode child : currentNodeChildren) {
      addChild(child, 0, gramIDsList, pointersList,
          countsList);
      root.removeChild(child.getId());
    }
    super.removeRoot();

    for (int n = 0; n < NUMBER_OF_LEVELS; n++) {
      List<Integer> temp = gramIDsList[n];
      gramIDsArrayRaw[n] = temp.stream().mapToInt(k -> k).toArray();
      temp = pointersList[n];
      pointersArrayRaw[n] = temp.stream().mapToInt(k -> k).toArray();
      temp = countsList[n];
      countsArrayRaw[n] = temp.stream().mapToInt(k -> k).toArray();
    }

    gramIDsList = null;
    pointersList = null;
    countsList = null;

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
      for (int j = 0; j < gramIDsArrayRaw[i].length - 1; j++) {
        rep += IDToWord.get(gramIDsArrayRaw[i][j]) + ", ";
//        rep += levels[i][0].get(j) + ", ";
      }
      rep += IDToWord.get(gramIDsArrayRaw[i][gramIDsArrayRaw[i].length - 1]) + "]";
      rep += "\n";
      rep += "PN: " + Arrays.toString(pointersArrayRaw[i]) + "\n";
      rep += "CN: " + Arrays.toString(countsArrayRaw[i]) + "\n";
    }

    return rep;
  }
}
