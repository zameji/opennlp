/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opennlp.tools.ngram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NgramDictionaryCompressed implements NgramDictionary {

  private final int NUMBER_OF_LEVELS;
  private final boolean STATICVOCABULARY;
  private final Map<String, Integer> wordToID;
  private final Map<Integer, String> IDToWord;
  private final int[][] gramIDsArrayRaw;
  private final int[][] pointersArrayRaw;
  private final int[][] countsArrayRaw;
  private NgramTrie root;
  private int vocabularySize = 0;
  private boolean compressed = false;

  public NgramDictionaryCompressed(int ngram, Map<String, Integer> dictionary, boolean staticVocabulary) {

    root = new NgramTrie(0);
    NUMBER_OF_LEVELS = ngram;
    STATICVOCABULARY = staticVocabulary;

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
  public int get(String... ngram) {
    return get(ngram, 0, ngram.length);
  }

  @Override
  public int get(String[] ngram, int start, int end) {

    Integer childNode, currentToken;

    currentToken = wordToID.get(ngram[start]);
    if (currentToken == null) {
      currentToken = wordToID.get("<OOV>");
      if (currentToken == null || currentToken > pointersArrayRaw.length) {
        return 0;
      }
    }

    childNode = currentToken;

    //todo: get the compression working
    int startIndex = pointersArrayRaw[0][childNode];
    int endIndex = pointersArrayRaw[0][childNode + 1];

    for (int i = 1; i < end - start; i++) {

      currentToken = wordToID.get(ngram[i + start]);

      //ngram not over, but no children recorded
      if (endIndex - startIndex == 0) {
        return 0;
      }

      if (currentToken == null) {
        currentToken = wordToID.get("<OOV>");
        if (currentToken == null) {
          return 0;
        }
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

  @Override
  public String[][] getSiblings(String... ngram) {
    return getSiblings(ngram, 0, ngram.length);
  }

  @Override
  public String[][] getSiblings(String[] ngram, int start, int end) {

    String[][] result;
    if (end - start >= NUMBER_OF_LEVELS) {
      return null;
    }

    int[] lastIndexRange = findLastIndexRange(ngram, start, end);
    if (lastIndexRange[0] < 0 || lastIndexRange[0] == lastIndexRange[1]) {
      return null;
    }

    int startPointer;
    int endPointer;

    if (end - start > 1) {
      Integer wordID = wordToID.get(ngram[end - 1]);
      if (wordID == null && wordToID.get("<OOV>") == null) {
        return null;
      } else if (wordID == null) {
        wordID = wordToID.get("<OOV>");
      }

      int wordPointer = findIndex(gramIDsArrayRaw[end - start - 1], wordID, lastIndexRange[0],
          lastIndexRange[1]);

      if (wordPointer < 0) {
        return null;
      }
      startPointer = pointersArrayRaw[end - start - 1][wordPointer];
      endPointer = pointersArrayRaw[end - start - 1][wordPointer + 1];
    } else {
      startPointer = lastIndexRange[0];
      endPointer = lastIndexRange[1];
    }


    result = new String[endPointer - startPointer][end - start + 1];
    for (int i = startPointer; i < endPointer; i++) {
      System.arraycopy(ngram, start, result[i - startPointer], 0, end - start);
      result[i - startPointer][end - start] = IDToWord.get(gramIDsArrayRaw[end - start][i]);
    }

    return result;

  }

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
  public int getNGramCountSum(int depth) {
    int sum = 0;
    for (int i = 0; i < countsArrayRaw[depth - 1].length; i++) {
      sum += countsArrayRaw[depth - 1][i];
    }
    return sum;
  }

  @Override
  public int getNGramCount(int gramSize, int minfreq, int maxfreq) {
    int c = 0;
    for (int count : countsArrayRaw[gramSize - 1]) {
      if (minfreq <= count && count <= maxfreq) {
        c++;
      }
    }
    return c;
  }

  public int getSiblingCount(String... ngram) {
    return getSiblingCount(ngram, 0, ngram.length);
  }

  @Override
  public int getSiblingCount(String[] ngram, int start, int end) {

    if (end - start == 1) {
      return countsArrayRaw[0].length;
    }

    int[] lastIndexRange = findLastIndexRange(ngram, start, end);
    if (lastIndexRange[0] < 0) {
      return 0;
    }

    return lastIndexRange[1] - lastIndexRange[0];

  }


  @Override
  public int getSiblingCount(String[] ngram, int start, int end, int minfreq, int maxfreq) {
    if (minfreq < 1) {
      System.err.println("[WARNING] Counting n-grams with frequency < 1. This may produce " +
          "unexpected results.");
    }
    if (end - start == 1) {
      int siblings = 0;
      for (int child : countsArrayRaw[0]) {
        if (minfreq <= child && child <= maxfreq) {
          siblings++;
        }
      }
      return siblings;
    }

    int[] lastIndexRange = findLastIndexRange(ngram, start, end);
    if (lastIndexRange[0] < 0) {
      return 0;
    }

    int siblings = 0;
    for (int i = lastIndexRange[0]; i < lastIndexRange[1]; i++) {
      if (minfreq <= countsArrayRaw[end - start - 1][i] &&
          countsArrayRaw[end - start - 1][i] <= maxfreq) {
        siblings++;
      }
    }
    return siblings;

  }

  @Override
  public int getSiblingCountSum(String[] ngram, int start, int end) {
    if (end - start == 1) {
      int siblings = 0;
      for (int child : countsArrayRaw[0]) {
        siblings += child;
      }
      return siblings;
    }

    int[] lastIndexRange = findLastIndexRange(ngram, start, end);
    if (lastIndexRange[0] < 0) {
      return 0;
    }


    int siblings = 0;
    for (int i = lastIndexRange[0]; i < lastIndexRange[1]; i++) {
      siblings += countsArrayRaw[end - start - 1][i];
    }
    return siblings;

  }

  /**
   * Add a new ngram to the dictionary (or increase its count by one)
   *
   * @param ngram The ngram to add
   */
  @Override
  public void add(String... ngram) {
    add(ngram, 0, ngram.length);
  }

  /**
   * Add a new ngram to the dictionary (or increase its count by one).
   * The ngram is defined as a range of strings within a larger document
   *
   * @param ngram The document in which the ngram is located
   * @param start Start of the ngram
   * @param end   End of the ngram
   */
  @Override
  public void add(String[] ngram, Integer start, Integer end) {
    if (compressed) {
      if (end - start > NUMBER_OF_LEVELS) {
        System.err.println("Adding failed: n-gram too long");
        return;
      }
      addIntoCompressed(ngram, start, end);

      return;
    }

    int[] gramInt = new int[end - start];
    for (int i = 0; i < end - start; i++) {
      gramInt[i] = getWordID(ngram[i + start]);
    }

    root.addChildren(gramInt);
  }

  /**
   * Insert an ngram after the dictionary has been compressed. May be very slow!
   *
   * @param ngram A tokenized document
   * @param start The beginning of the relevant ngram
   * @param end   The end of the relevant ngram
   */
  private void addIntoCompressed(String[] ngram, int start, int end) {
    int parentNode = 0;
    int rangeStart = 0;
    int rangeEnd = gramIDsArrayRaw[0].length;
    for (int gramIndex = 0; gramIndex < end - start; gramIndex++) {

      int wordID = getWordID(ngram[gramIndex + start]);    //get current word's ID

      int childNode = -1;
      if (rangeEnd - rangeStart > 0) {
        childNode = findIndex(gramIDsArrayRaw[gramIndex], wordID, rangeStart, rangeEnd) + rangeStart;
      }

      if (childNode - rangeStart > -1) {
        //word found, proceed to its children
        rangeEnd = pointersArrayRaw[gramIndex][childNode + 1];
        rangeStart = pointersArrayRaw[gramIndex][childNode];

      } else {
        //word not found, insert it
        //1. find where it should be inserted
        childNode = findInsert(gramIDsArrayRaw[gramIndex], wordID, rangeStart, rangeEnd);

        //2. insert it there
        gramIDsArrayRaw[gramIndex] = insertIntoArray(gramIDsArrayRaw[gramIndex], wordID, childNode);
        countsArrayRaw[gramIndex] = insertIntoArray(countsArrayRaw[gramIndex], 0, childNode);

        //3. create a pointer to its child if there is one
        pointersArrayRaw[gramIndex] = insertIntoArray(pointersArrayRaw[gramIndex],
            pointersArrayRaw[gramIndex][childNode], childNode);

        rangeStart = pointersArrayRaw[gramIndex][childNode];
        rangeEnd = pointersArrayRaw[gramIndex][childNode];

        //4. go level above, move the pointers behind it
        if (gramIndex > 0) {
          for (int i = parentNode + 1; i < pointersArrayRaw[gramIndex - 1].length; i++) {
            pointersArrayRaw[gramIndex - 1][i]++;
          }
        }
      }

      if (gramIndex == end - start - 1) {
        countsArrayRaw[gramIndex][childNode]++;
      }

      parentNode = childNode;

    }
  }

  /**
   * Add all children of the present node to the compressed structure
   *
   * @param node         A ngram trie node
   * @param level        The n-gram depth at which this node is found
   * @param gramIDsList  A list of word IDs for each level of depth
   * @param pointersList A list of pointers to the next level for each level of depth
   * @param countsList   A list of frequencies for each level of depth
   */
  private void addChild(NgramTrie node, int level, List[] gramIDsList, List[] pointersList,
                        List[] countsList) {
    if (level >= NUMBER_OF_LEVELS) {
      return;
    }

    //start at the first node in the vocabulary that is present
    Collection<NgramTrie> children = node.getChildren();
    List<NgramTrie> childrenList = new ArrayList<>();
    for (NgramTrie child : children) {
      childrenList.add(child);
    }
    Collections.sort(childrenList);
    int childCount = childrenList.size();

    gramIDsList[level].add(node.getId());
    pointersList[level].add((int) pointersList[level].get(pointersList[level].size() - 1) + childCount);
    countsList[level].add(node.getCount());

    for (NgramTrie currentChild : childrenList) {
      addChild(currentChild, level + 1, gramIDsList, pointersList,
          countsList);
    }
  }

  /**
   * Translate the dictionary from a hashed one into the compressed form.
   * This incurs some penalty on the speed, but improves memory footprint.
   * Use it as late as possible, because adding new n-grams will become much slower.
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

    if (wordToID.get("<OOV>") == null) {
      System.out.println("[INFO] Adding the <OOV> tag to the vocabulary.");
      wordToID.put("<OOV>", wordToID.size());
      IDToWord.put(IDToWord.size(), "<OOV>");
    }

    Collection<NgramTrie> children = root.getChildren();
    if (root.getChild(wordToID.get("<OOV>")) == null) {
      System.out.println("[INFO] Adding the <OOV> tag with frequency 0 to handle unseen words elegantly.");
      root.addChildren(new int[] {wordToID.get("<OOV>")});
      root.getChild(wordToID.get("<OOV>")).decrement();
    }

    List<NgramTrie> childrenList = new ArrayList<>();
    for (NgramTrie child : children) {
      childrenList.add(child);
    }
    Collections.sort(childrenList);

    for (NgramTrie child : childrenList) {
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

  /**
   * Utility for debugging. Do not use with large dictionaries.
   * @return A string representation of the dictionary.
   */
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
      if (!STATICVOCABULARY) {
        id = vocabularySize;
        wordToID.put(word, id);
        IDToWord.put(id, word);
        vocabularySize++;
      } else {
        id = wordToID.get("<OOV>");
        if (id == null) {
          System.err.println("Static vocabulary without the \"<OOV>\" tag cannot deal " +
              "with words not in the vocabulary. Adding the \"<OOV>\" tag to the vocabulary.");
          id = vocabularySize;
          wordToID.put("<OOV>", id);
          IDToWord.put(id, "<OOV>");
        }
      }
    }
    return id;
  }

  /**
   * Find the index of an element
   *
   * @param arr  The array to search in between [low; high), must be sorted in ascending
   *             order between low and high
   * @param key  The key to find
   * @param low  The lower boundary of the search
   * @param high The upper boundary of the search
   * @return The index at which this key can be found or -1
   */
  private int findIndex(int[] arr, Integer key, Integer low, Integer high) {

    int middle = (low + high) >>> 1;

    if (high <= low) {
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

  /**
   * Find where to insert an element
   *
   * @param arr  The array to search in between [low; high), must be sorted in ascending
   *             order between low and high
   * @param key  The key to find
   * @param low  The lower boundary of the search
   * @param high The upper boundary of the search
   * @return The index at which the key should be inserted in order to keep the sorting
   */
  private int findInsert(int[] arr, int key, int low, int high) {

    if (high - low <= 1) {
      if (high - low < 1) {

        return low;
      }

      return (arr[low] > key) ? low : high;
    }

    int median = (low + high) >>> 1;

    if (arr[median] < key) {
      return findInsert(arr, key, median, high);
    } else {
      return findInsert(arr, key, low, median);
    }

  }

  /**
   * Insert a value into an array at a given index
   *
   * @param arr   Array to insert into
   * @param value The value to insert
   * @param index The position to insert at
   * @return A new array with the value inserted
   */
  private int[] insertIntoArray(int[] arr, int value, int index) {
    if (arr.length == 0) {
      return new int[] {value};
    }

    int[] extended = new int[arr.length + 1];
    System.arraycopy(arr, 0, extended, 0, index);
    extended[index] = value;
    if (index < arr.length) {
      System.arraycopy(arr, index, extended, index + 1, arr.length - index);
    }
    return extended;
  }

  /**
   * Find the range of pointers between which the last element of the ngram should
   * be found in the compressed structure
   *
   * @param ngram A (possibly) larger ngram
   * @param start The beginning of the relevant ngram
   * @param end   The end of the relevant ngram
   * @return The range between which the last element should be located in the gramIDpointers array;
   * if not found and <OOV> tag doesn't exist, the range will be -1,-1
   */
  private int[] findLastIndexRange(String[] ngram, int start, int end) {
    Integer wordId = wordToID.get(ngram[start]);
    if (wordId == null) {
      wordId = wordToID.get("<OOV>");
      if (wordId == null) {
        return new int[] {-1, -1};
      }
    }

    int wordIndex = wordId;
    int startPointer;
    int endPointer;
    startPointer = pointersArrayRaw[0][wordIndex];
    endPointer = pointersArrayRaw[0][wordIndex + 1];

    int level = 1;
    while (end - (start + level) > 1 && startPointer != endPointer) {
      wordId = wordToID.get(ngram[level + start]);
      if (wordId == null) {
        wordId = wordToID.get("<OOV>");
        if (wordId == null) {
          return new int[] {-1, -1};
        }
      }

      wordIndex = findIndex(gramIDsArrayRaw[level], wordId, startPointer, endPointer);
      if (wordIndex < 0) {
        return new int[] {-1, -1};
      }

      startPointer = pointersArrayRaw[level][wordIndex];
      endPointer = pointersArrayRaw[level][wordIndex + 1];
      level++;
    }

    return new int[] {startPointer, endPointer};

  }

}


