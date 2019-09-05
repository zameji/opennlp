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

package opennlp.tools.languagemodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NgramDictionary {

  private final int NUMBER_OF_LEVELS;
  private final List[][] levels;
  private final List[] counts;
  private Map<String, Integer> wordToID;
  private Map<Integer, String> IDToWord;
  private int vocabularySize = 0;

  /**
   * Initialize the trie structure
   *
   * @param n          number of levels
   * @param dictionary (optional) mapping of words to their integer IDs
   */
  NgramDictionary(int n, Map<String, Integer> dictionary) {

    NUMBER_OF_LEVELS = n;
    IDToWord = new HashMap<>();
    if (dictionary == null) {
      wordToID = new HashMap<>();
    } else {
      wordToID = dictionary;
      for (Map.Entry<String, Integer> w : dictionary.entrySet()) {
        IDToWord.put(w.getValue(), w.getKey());
      }
    }
    levels = new List[n][2];
    counts = new List[n];

    for (int i = 0; i < NUMBER_OF_LEVELS; i++) {
      List gramIDs = new ArrayList();
      List pointers = new ArrayList();
      pointers.add(0);  //set the start & end pointers
      List[] currentLevel = new List[] {gramIDs, pointers};
      levels[i] = currentLevel;
      counts[i] = new ArrayList();
    }
  }

  public void add(String[] gram) {
    add(gram, 0, gram.length);
  }

  public void add(String[] gram, Integer start, Integer end) {
    /*for each gram level, check that this word is present
    If yes, proceed to its children
     */

    int parentNode = 0;
    int rangeStart = 0;
    int rangeEnd = levels[0][0].size();
    for (int gramIndex = 0; gramIndex < end - start; gramIndex++) {

      int wordID = getWordID(gram[gramIndex + start]);    //get current word's ID

      int childNode = -1;
      if (rangeEnd - rangeStart > 0) {
        childNode = levels[gramIndex][0].subList(rangeStart, rangeEnd).
            indexOf(wordID) + rangeStart;
      }

      if (childNode - rangeStart > -1) {
        //word found, proceed to its children
        rangeEnd = (int) levels[gramIndex][1].get(childNode + 1);
        rangeStart = (int) levels[gramIndex][1].get(childNode);

      } else {
        //word not found, insert it
        //1. find where it should be inserted
        childNode = findInsert(levels[gramIndex][0], wordID, rangeStart, rangeEnd);

        //2. insert it there
        levels[gramIndex][0].add(childNode, wordID);
        counts[gramIndex].add(childNode, 0);

        //3. create a pointer to its child if there is one
        levels[gramIndex][1].add(childNode, levels[gramIndex][1].get(childNode));
        rangeStart = (int) levels[gramIndex][1].get(childNode);
        rangeEnd = (int) levels[gramIndex][1].get(childNode);

        //4. go level above, move the pointers behind it
        if (gramIndex > 0) {
          for (int i = parentNode + 1; i < levels[gramIndex - 1][1].size(); i++) {
            levels[gramIndex - 1][1].set(i, (int) levels[gramIndex - 1][1].get(i) + 1);
          }
        }
      }

      if (gramIndex == end - start - 1) {
        counts[gramIndex].set(childNode, (int) counts[gramIndex].get(childNode) + 1);
      }

      parentNode = childNode;

    }

  }

  public Integer get(String[] gram) {
    return get(gram, 0, gram.length);
  }

  public Integer get(String[] gram, Integer start, Integer end) {
    Integer childNode, currentToken = wordToID.get(gram[start]);
    if (currentToken == null) {
      return 0;
    }

    childNode = currentToken;

    int startIndex = (int) levels[0][1].get(childNode);
    int endIndex = (int) levels[0][1].get(childNode + 1);

    for (int i = 1; i < end - start; i++) {

      currentToken = getWordID(gram[i + start]);
      if (endIndex - startIndex == 0) {
        return 0;
      }

      childNode = levels[i][0].subList(startIndex, endIndex).indexOf(currentToken) + startIndex;
      if (childNode - startIndex == -1) {
        return 0;
      }

      startIndex = (int) levels[i][1].get(childNode);
      endIndex = (int) levels[i][1].get(childNode + 1);
    }

    return (int) counts[end - start - 1].get(childNode);

  }


  /**
   * Find where to insert a missing element
   *
   * @param ls   a list to insert into (sorted between low and high)
   * @param key  the element to insert
   * @param low  starting point for the search
   * @param high end point for the search
   * @return the index at which the key should be inserted
   */
  private int findInsert(List<Integer> ls, int key, int low, int high) {

    if (high - low <= 1) {
      if (high - low < 1) {
        return low;
      }
      return (ls.get(low) > key) ? low : high;
    }

    int median = (low + high) / 2;

    if (ls.get(median) < key) {
      return findInsert(ls, key, median, high);
    } else {
      return findInsert(ls, key, low, median);
    }

  }

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

  public String toString() {
    String rep = "";
    for (int i = 0; i < NUMBER_OF_LEVELS; i++) {
      rep += "CH: [";
      for (int j = 0; j < levels[i][0].size() - 1; j++) {
        rep += IDToWord.get(levels[i][0].get(j)) + ", ";
      }
      rep += IDToWord.get(levels[i][0].get(levels[i][0].size() - 1)) + "]";
      rep += "\n";
      rep += "PN: " + levels[i][1].toString() + "\n";
      rep += "CN: " + counts[i].toString() + "\n";
    }

    return rep;
  }
}
