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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NgramDictionaryHashed implements NgramDictionary {

  private final boolean STATICVOCABULARY;
  private final int NUMBER_OF_LEVELS;
  private final Map<String, Integer> wordToID;
  private final Map<Integer, String> IDToWord;
  private final NgramTrie root;
  private int vocabularySize = 0;

  public NgramDictionaryHashed(int ngram, Map<String, Integer> dictionary) {
    this(ngram, dictionary, false);
  }

  /**
   * Initialize the trie structure
   *
   * @param dictionary (optional) mapping of words to their integer IDs
   */
  public NgramDictionaryHashed(int ngram, Map<String, Integer> dictionary, boolean staticVocabulary) {

    root = new NgramTrie(0);

    STATICVOCABULARY = staticVocabulary;
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
  }

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
  public int getNGramCount(int depth, int minfreq, int maxfreq) {
    return root.getChildrenCount(depth, minfreq, maxfreq);
  }

  @Override
  public int getNGramCountSum(int depth) {
    return getNGramCountSum(depth, root);
  }

  @Override
  public int getSiblingCount(String[] ngram, int start, int end) {
    NgramTrie lastNode = getLastNode(ngram, start, end, root);
    if (lastNode == null) {
      return 0;
    }

    return lastNode.getChildrenCount();

  }

  @Override
  public int getSiblingCount(String[] ngram, int start, int end, int minfreq,
                             int maxfreq) {
    NgramTrie lastNode = getLastNode(ngram, start, end, root);
    if (lastNode == null) {
      return 0;
    }

    return lastNode.getChildrenCount(1, minfreq, maxfreq);
  }

  @Override
  public int getSiblingCountSum(String[] ngram, int start, int end) {
    NgramTrie lastNode = getLastNode(ngram, start, end, root);
    if (lastNode == null) {
      return 0;
    }

    int childCount = 0;
    Collection<NgramTrie> children = lastNode.getChildren();
    for (NgramTrie child : children) {
      childCount += child.getCount();
    }
    return childCount;

  }

  @Override
  public String[][] getSiblings(String... ngram) {
    return getSiblings(ngram, 0, ngram.length);
  }

  @Override
  public String[][] getSiblings(String[] ngram, int start, int end) {

    if (end - start >= NUMBER_OF_LEVELS) {
      return null;
    }

    String[][] result;
    NgramTrie lastNode = getLastNode(ngram, start, end, root);

    if (lastNode == null) {
      return null;
    }

    Integer id = wordToID.get(ngram[end - 1]);
    if (id == null && wordToID.get("<OOV>") == null) {
      return null;
    } else if (id == null) {
      id = wordToID.get("<OOV>");
    }

    lastNode = lastNode.getChild(id);
    if (lastNode == null) {
      return null;
    }

    Collection<NgramTrie> siblingsUnsorted = lastNode.getChildren();
    NgramTrie[] siblings = new NgramTrie[siblingsUnsorted.size()];
    siblings = siblingsUnsorted.toArray(siblings);
    Arrays.sort(siblings);

    String[] siblingsArray = new String[siblings.length];
    int i = 0;
    for (NgramTrie sibling : siblings) {
      siblingsArray[i] = IDToWord.get(sibling.getId());
      i++;
    }

    result = new String[siblingsArray.length][end - start + 1];
    for (int j = 0; j < siblingsArray.length; j++) {
      System.arraycopy(ngram, start, result[j], 0, end - start);
      System.arraycopy(siblingsArray, j, result[j], end - start, 1);
    }

    return result;
  }

  @Override
  public void add(String... ngram) {
    add(ngram, 0, ngram.length);
  }

  @Override
  public void add(String[] ngram, Integer start, Integer end) {
    /*for each gram level, check that this word is present
    If yes, proceed to its children
     */
    int[] gramInt = new int[end - start];
    for (int i = 0; i < end - start; i++) {
      gramInt[i] = getWordID(ngram[i + start]);
    }

    root.addChildren(gramInt);

  }

  @Override
  public int get(String... ngram) {
    return get(ngram, 0, ngram.length);
  }

  @Override
  public int get(String[] ngram, int start, int end) {
    Integer currentToken = wordToID.get(ngram[start]);
    NgramTrie currentNode = root;
    if (currentToken == null) {
      currentToken = wordToID.get("<OOV>");
      if (currentToken == null) {
        return 0;
      }
    }

    for (int i = 0; i < end - start; i++) {
      Integer id = wordToID.get(ngram[i + start]);
      if (id == null) {
        id = wordToID.get("<OOV>");
        if (id == null) {
          return 0;
        }
      }

      currentNode = currentNode.getChild(id);
      if (currentNode == null) {
        return 0;
      }
    }

    return currentNode.getCount();

  }

  /**
   * Get the integer ID of a word
   *
   * @param word The word to get ID of
   * @return The ID, or the <OOV>-tag-ID if current word not found and the vocabulary static
   */
  private int getWordID(String word) {
    Integer id = wordToID.get(word);
    if (id == null) {
      if (!STATICVOCABULARY) {
        id = vocabularySize;
        wordToID.put(word, id);
//        IDToWord.put(id, word);
        vocabularySize++;
      } else {
        id = wordToID.get("<OOV>");
        if (id == null) {
          System.err.println("[INFO] Static vocabulary without the \"<OOV>\" tag cannot deal " +
              "with words not in the vocabulary. Adding the \"<OOV>\" tag to the vocabulary.");
          id = vocabularySize;
          wordToID.put("<OOV>", id);
//          IDToWord.put(id, "<OOV>");
        }
      }
    }
    return id;
  }

  /**
   * Get the node of the to which the last element of the ngram is a child
   * @param ngram An ngram
   * @param start The beginning of the relevant ngram
   * @param end The end of the relevant ngram
   * @param currentNode The node at which the search should start
   * @return The node to which the last element of the ngram is a child
   */
  private NgramTrie getLastNode(String[] ngram, int start, int end, NgramTrie currentNode) {
    if (end - start == 1) {
      return currentNode;
    }

    Integer currentToken = wordToID.get(ngram[start]);
    if (currentToken == null) {
      currentToken = wordToID.get("<OOV>");
      if (currentToken == null) {
        return null;
      }
    }

    NgramTrie nextNode = currentNode.getChild(currentToken);
    if (nextNode == null) {
      return null;
    } else {
      return getLastNode(ngram, start + 1, end, nextNode);

    }
  }

  /**
   * Get the sum of ngram counts at a certain depth below a certain node
   * @param depth How deep to progress below the current node
   * @param node The current node
   * @return The sum of counts of all ngrams that are children to the current node
   *          and have length = current node length + depth
   */
  private int getNGramCountSum(int depth, NgramTrie node) {
    int sum = 0;
    if (depth == 1) {

      Collection<NgramTrie> children = node.getChildren();
      for (NgramTrie child : children) {
        sum += child.getCount();
      }

      return sum;
    } else {
      Collection<NgramTrie> children = node.getChildren();
      for (NgramTrie child : children) {
        sum += getNGramCountSum(depth - 1, child);
      }
    }
    return sum;
  }

}
