package opennlp.tools.ngram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NgramDictionaryCompressed implements NgramDictionary {

  private final int NUMBER_OF_LEVELS;
  private final Map<String, Integer> wordToID;
  private final Map<Integer, String> IDToWord;
  private NgramTrie root;
  private int vocabularySize = 0;
  private boolean compressed = false;
  
  private final int[][] gramIDsArrayRaw;
  private final int[][] pointersArrayRaw;
  private final int[][] countsArrayRaw;

  public NgramDictionaryCompressed(int ngram, Map<String, Integer> dictionary) {

    root = new NgramTrie(0);
    NUMBER_OF_LEVELS = ngram;
    IDToWord = new HashMap<>();
    if (dictionary == null) {
      wordToID = new HashMap<>();
    } else {
      wordToID = dictionary;
      for (Map.Entry<String, Integer> word : wordToID.entrySet()) {
        IDToWord.put(word.getValue(), word.getKey());
      }

      vocabularySize = wordToID.size();
    }

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
    Integer childNode, currentToken;

    currentToken = wordToID.get(gram[start]);
    if (currentToken == null) {
      return 0;
    }

    childNode = currentToken;

    //todo: get the compression working
    int startIndex = pointersArrayRaw[0][childNode];
    int endIndex = pointersArrayRaw[0][childNode + 1];

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
        startIndex = pointersArrayRaw[i][childNode];
        endIndex = pointersArrayRaw[i][childNode + 1];
      }
    }
    return countsArrayRaw[end - start - 1][childNode];

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
      size += countsArrayRaw[0][i];
    }
    return size;
  }

  @Override
  public int getNGramCountSum(int n) {
    int sum = 0;
    for (int i = 0; i < countsArrayRaw[n - 1].length; i++) {
      sum += countsArrayRaw[n - 1][i];
    }
    return sum;
  }

  /**
   * Get the number of various ngram of a given size
   *
   * @param gramSize The size of the ngram (i.e. n)
   * @return The number of various ngrams of this size
   */
  public int getNGramCount(int gramSize) {
    return getNGramCount(gramSize, 1, Integer.MAX_VALUE);
  }

  public int getNGramCount(int gramSize, int minfreq, int maxfreq) {
    int c = 0;
    for (int count : countsArrayRaw[gramSize - 1]) {
      if (minfreq <= count && count <= maxfreq) {
        c++;
      }
    }
    return c;
  }

  @Override
  public int getSiblingCount(String... ngram) {
    return getSiblingCount(ngram, 0, ngram.length);
  }

  @Override
  public int getSiblingCount(String[] ngram, int start, int end) {

    if (end - start == 1) {
      int siblings = 0;
      for (int child : countsArrayRaw[0]) {
        if (child > 0) {
          siblings++;
        }
      }
      return siblings;
    }

    Integer wordId = wordToID.get(ngram[start]);

    if (wordId == null) {
      return 0;
    }

    int wordIndex = wordId;
    int startPointer;
    int endPointer;
    startPointer = pointersArrayRaw[0][wordIndex];
    endPointer = pointersArrayRaw[0][wordIndex + 1];

    int level = 1;
    while (end - (start + level) > 1) {
      wordId = wordToID.get(ngram[level + start]);
      if (wordId == null) {
        return 0;
      }

      if (startPointer == endPointer) {
        break;
      }

      wordIndex = findIndex(gramIDsArrayRaw[level], wordId, startPointer, endPointer);
      if (wordIndex < 0) {
        return 0;
      }

      startPointer = pointersArrayRaw[level][wordIndex];
      endPointer = pointersArrayRaw[level][wordIndex + 1];
      level++;
    }

    return endPointer - startPointer;

  }


  @Override
  public int getSiblingCount(String[] ngram, int start, int end, int minfreq, int maxfreq) {

    if (end - start == 1) {
      int siblings = 0;
      for (int child : countsArrayRaw[0]) {
        if (minfreq <= child && child <= maxfreq) {
          siblings++;
        }
      }
      return siblings;
    }

    Integer wordId = wordToID.get(ngram[start]);

    if (wordId == null) {
      return 0;
    }

    int wordIndex = wordId;
    int startPointer;
    int endPointer;
    startPointer = pointersArrayRaw[0][wordIndex];
    endPointer = pointersArrayRaw[0][wordIndex + 1];

    int level = 1;
    while (end - (start + level) > 1) {
      wordId = wordToID.get(ngram[level + start]);
      if (wordId == null) {
        return 0;
      }

      if (startPointer == endPointer) {
        break;
      }

      wordIndex = findIndex(gramIDsArrayRaw[level], wordId, startPointer, endPointer);
      if (wordIndex < 0) {
        return 0;
      }

      startPointer = pointersArrayRaw[level][wordIndex];
      endPointer = pointersArrayRaw[level][wordIndex + 1];
      level++;
    }

    int siblings = 0;
    for (int i = startPointer; i < endPointer; i++) {
      if (minfreq <= countsArrayRaw[level][i] && countsArrayRaw[level][i] <= maxfreq && countsArrayRaw[level][i] > 0) {
        siblings++;
      }
    }
    return siblings;

  }

  /**
   * Add a new ngram to the dictionary (or increase its count by one)
   *
   * @param gram The ngram to add
   */
  @Override
  public void add(String[] gram) {
    add(gram, 0, gram.length);
  }

  /**
   * Add a new ngram to the dictionary (or increase its count by one).
   * The ngram is defined as a range of strings within a larger document
   *
   * @param gram  The document in which the ngram is located
   * @param start Start of the ngram
   * @param end   End of the ngram
   */
  @Override
  public void add(String[] gram, Integer start, Integer end) {
    if (compressed) {
      System.err.println("Could not add n-gram into compressed array. Compress only after adding.");
      return;
    }

    int[] gramInt = new int[end - start];
    for (int i = 0; i < end - start; i++) {
      gramInt[i] = getWordID(gram[i + start]);
    }

    root.addChildren(gramInt);
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

  private void addChild(NgramTrie child, int level, List[] gramIDsList, List[] pointersList,
                        List[] countsList) {
    if (level >= NUMBER_OF_LEVELS) {
      return;
    }

    //start at the first node in the vocabulary that is present
    List<NgramTrie> currentNodeChildren = child.getChildren();
    Collections.sort(currentNodeChildren);

    int childCount = currentNodeChildren.size();

    gramIDsList[level].add(child.getId());
    pointersList[level].add((int) pointersList[level].get(pointersList[level].size() - 1) + childCount);
    countsList[level].add(child.getCount());

    for (NgramTrie currentChild : currentNodeChildren) {
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
      List<Integer> currentGramIDs = new ArrayList<>();
      List<Integer> currentPointers = new ArrayList<>();
      currentPointers.add(0);  //set the start & end pointers

      gramIDsList[n] = currentGramIDs;
      pointersList[n] = currentPointers;
      countsList[n] = new ArrayList<Integer>();

    }

    List<NgramTrie> currentNodeChildren = root.getChildren();
    Collections.sort(currentNodeChildren);


    for (NgramTrie child : currentNodeChildren) {
      addChild(child, 0, gramIDsList, pointersList,
          countsList);
      root.removeChild(child.getId());
    }

    root = null;

    for (int n = 0; n < NUMBER_OF_LEVELS; n++) {
      List<Integer> temp = gramIDsList[n];
      gramIDsArrayRaw[n] = temp.stream().mapToInt(k -> k).toArray();
      temp = pointersList[n];
      pointersArrayRaw[n] = temp.stream().mapToInt(k -> k).toArray();
      temp = countsList[n];
      countsArrayRaw[n] = temp.stream().mapToInt(k -> k).toArray();
    }

    compressed = true;
    //todo: get the compression working

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

  /**
   * For each word, get its ID
   *
   * @param word The word to get ID of
   * @return The ID
   */
  private int getWordID(String word) {
    Integer id = wordToID.get(word);
    if (id == null) {
      id = vocabularySize;
      wordToID.put(word, id);
      IDToWord.put(id, word);
      vocabularySize++;
    }
    return id;
  }

}
