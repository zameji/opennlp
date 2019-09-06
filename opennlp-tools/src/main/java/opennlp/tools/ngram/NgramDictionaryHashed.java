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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NgramDictionaryHashed implements NgramDictionary {

  class DictionaryNode implements Comparable<DictionaryNode> {

    private final Map<Integer, DictionaryNode> children = new HashMap<>();
    private final int id;
    private int count = 0;

    public DictionaryNode(int id) {
      this.id = id;
    }

    public DictionaryNode getChild(int childID) {
      return children.get(childID);
    }

    public int getChildrenCount() {
      return getChildrenCount(1);
    }

    public int getChildrenCount(int depth) {
      if (depth == 1) {
        return children.size();
      } else {
        int childrenCount = 0;
        for (Map.Entry<Integer, DictionaryNode> child : children.entrySet()) {
          childrenCount += child.getValue().getChildrenCount(depth - 1);
        }
        return childrenCount;
      }
    }

    public int getChildrenCount(int depth, int frequency) {
      if (depth == 1) {
        int relevantChildren = 0;
        for (Map.Entry<Integer, DictionaryNode> child : children.entrySet()) {
          if (child.getValue().getCount() == frequency) {
            relevantChildren++;
          }
        }
        return relevantChildren;
      } else {
        int relevantChildren = 0;
        for (Map.Entry<Integer, DictionaryNode> child : children.entrySet()) {
          relevantChildren += child.getValue().getChildrenCount(depth - 1, frequency);
        }
        return relevantChildren;
      }
    }

    public void addChild(int childID) {
      children.put(childID, new DictionaryNode(childID));
    }

    public void removeChild(int childID) {
      children.remove(childID);
    }

    public void increment() {
      count++;
    }

    public int getCount() {
      return count;
    }

    public int getId() {
      return id;
    }

    public List<DictionaryNode> getChildren() {
      List<DictionaryNode> childrenList = new ArrayList<>();
      for (Map.Entry<Integer, DictionaryNode> child : children.entrySet()) {
        childrenList.add(child.getValue());
      }
      return childrenList;
    }

    @Override
    public int compareTo(DictionaryNode o) {
      return id - o.getId();
    }
  }

  final int NUMBER_OF_LEVELS;
  final Map<String, Integer> wordToID;
  final Map<Integer, String> IDToWord;
  private final DictionaryNode root;
  private int vocabularySize = 0;

  /**
   * Initialize the trie structure
   *
   * @param n          number of levels
   * @param dictionary (optional) mapping of words to their integer IDs
   */
  public NgramDictionaryHashed(int n, Map<String, Integer> dictionary) {
    root = new DictionaryNode(0);
    NUMBER_OF_LEVELS = n;
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
  }

  public DictionaryNode getRoot() {
    return root;
  }

  /**
   * Get the size of the corpus
   *
   * @return The size
   */
  @Override
  public int getCorpusSize() {
    List<DictionaryNode> unigrams = root.getChildren();
    int size = 0;
    for (DictionaryNode unigram : unigrams) {
      size += unigram.getCount();
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
    return root.getChildrenCount(gramSize);
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
    return root.getChildrenCount(gramSize, frequency);
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
   * @param doc   The document in which the ngram is located
   * @param start Start of the ngram
   * @param end   End of the ngram
   */
  @Override
  public void add(String[] doc, Integer start, Integer end) {
    /*for each gram level, check that this word is present
    If yes, proceed to its children
     */

    DictionaryNode currentNode = root;
    DictionaryNode childNode;
    for (int gramIndex = 0; gramIndex < end - start; gramIndex++) {

      int wordID = getWordID(doc[gramIndex + start]);    //get current word's ID

      childNode = currentNode.getChild(wordID);

      if (childNode == null) {
        currentNode.addChild(wordID);
        childNode = currentNode.getChild(wordID);
      }

      currentNode = childNode;

    }

    currentNode.increment();

  }

  /**
   * Get the frequency of an ngram in the dictionary
   *
   * @param gram An ngram with length <= NUMBER_OF_LEVELS
   * @return Its count
   */
  @Override
  public int get(String... gram) {
    return get(gram, 0, gram.length);
  }

  /**
   * Get the frequency of an ngram passed as a chunk of a document
   *
   * @param doc   A document
   * @param start The start of the ngram in the document
   * @param end   The end of the ngram in the document
   * @return Its count
   */
  @Override
  public int get(String[] doc, int start, int end) {
    Integer currentToken = wordToID.get(doc[start]);
    DictionaryNode currentNode = root;
    if (currentToken == null) {
      return 0;
    }

    for (int i = 0; i < end - start; i++) {
      currentNode = currentNode.getChild(wordToID.get(doc[i + start]));
      if (currentNode == null) {
        return 0;
      }
    }

    return currentNode.getCount();

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
