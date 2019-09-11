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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NgramDictionaryHashed implements NgramDictionary {

  private final Map<String, Integer> wordToID;
  private final NgramTrie root;
  private int vocabularySize = 0;

  /**
   * Initialize the trie structure
   *
   * @param dictionary (optional) mapping of words to their integer IDs
   */
  public NgramDictionaryHashed(Map<String, Integer> dictionary) {
    root = new NgramTrie(0);
    if (dictionary == null) {
      wordToID = new HashMap<>();
    } else {
      wordToID = dictionary;
      vocabularySize = wordToID.size();
    }
  }

  public NgramTrie getRoot() {
    return root;
  }

  /**
   * Get the size of the corpus
   *
   * @return The size
   */
  @Override
  public int getCorpusSize() {

    Collection<NgramTrie> children = root.getChildren();

    int size = 0;
    for (NgramTrie unigram : children) {
      size += unigram.getCount();
    }
    return size;
  }

  @Override
  public int getNGramCount(int depth) {
    return root.getChildrenCount(depth);
  }

  /**
   * Get the number of various ngram of a given size and frequency
   *
   * @param depth   The size of the ngram (i.e. n)
   * @param minfreq The minimum frequency with which the ngram should occur
   * @param maxfreq The maximum frequency with which the ngram should occur
   * @return The number of various ngrams of this size
   */
  @Override
  public int getNGramCount(int depth, int minfreq, int maxfreq) {
    return root.getChildrenCount(depth, minfreq, maxfreq);
  }

  @Override
  public int getNGramCountSum(int n) {
    return getNGramCountSum(n, root);
  }

  private int getNGramCountSum(int n, NgramTrie node) {
    int sum = 0;
    if (n == 1) {

      Collection<NgramTrie> children = node.getChildren();
      for (NgramTrie child : children) {
        sum += child.getCount();
      }

      return sum;
    } else {
      Collection<NgramTrie> children = node.getChildren();
      for (NgramTrie child : children) {
        sum += getNGramCountSum(n - 1, child);
      }
    }
    return sum;
  }

  @Override
  public int getSiblingCount(String... childNgram) {
    return getSiblingCountByNode(childNgram, 0, childNgram.length, root);
  }

  @Override
  public int getSiblingCount(String[] ngram, int start, int end) {
    return getSiblingCountByNode(ngram, start, end, root);
  }

  @Override
  public int getSiblingCount(String[] arrayWithChildNgram, int start, int end, int minfreq,
                             int maxfreq) {
    return getSiblingCountByNode(arrayWithChildNgram, start, end, minfreq, maxfreq, root);
  }

  private int getSiblingCountByNode(String[] arrayWithChildNgram, int start, int end,
                                    NgramTrie currentNode) {

    if (end - start == 1) {
      return currentNode.getChildrenCount();
    }

    Integer currentToken = wordToID.get(arrayWithChildNgram[start]);
    if (currentToken == null) {
      return 0;
    } else {
      NgramTrie nextNode = currentNode.getChild(currentToken);
      if (nextNode == null) {
        return 0;
      } else {
        return getSiblingCountByNode(arrayWithChildNgram, start + 1, end, nextNode);
      }
    }
  }

  private int getSiblingCountByNode(String[] arrayWithChildNgram, int start, int end, int minfreq,
                                    int maxfreq, NgramTrie currentNode) {

    if (end - start == 1) {
      return currentNode.getChildrenCount(1, minfreq, maxfreq);
    }

    Integer currentToken = wordToID.get(arrayWithChildNgram[start]);
    if (currentToken == null) {
      return 0;
    } else {
      NgramTrie nextNode = currentNode.getChild(currentToken);
      if (nextNode == null) {
        return 0;
      } else {
        return getSiblingCountByNode(arrayWithChildNgram, start + 1, end, minfreq, maxfreq, nextNode);
      }
    }

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
    /*for each gram level, check that this word is present
    If yes, proceed to its children
     */
    int[] gramInt = new int[end - start];
    for (int i = 0; i < end - start; i++) {
      gramInt[i] = getWordID(gram[i + start]);
    }

    root.addChildren(gramInt);

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
    NgramTrie currentNode = root;
    if (currentToken == null) {
      return 0;
    }

    for (int i = 0; i < end - start; i++) {
      Integer id = wordToID.get(doc[i + start]);
      if (id == null) {
        return 0;
      }
      currentNode = currentNode.getChild(id);
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
      vocabularySize++;
    }
    return id;
  }

}
