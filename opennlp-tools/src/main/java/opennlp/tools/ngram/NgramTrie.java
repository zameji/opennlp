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

class NgramTrie implements Comparable<NgramTrie> {

  private final Map<Integer, NgramTrie> children = new HashMap<>();
  private int[] childCache;   //cache the number of children with frequencies 1, 2, >=3 (for Chen-Goodman)
  private final int id;
  private int count = 0;

  public NgramTrie(int id) {
    this.id = id;
    childCache = new int[] {-1, -1, -1};
  }

  /**
   * Add a given node to the children of this node
   *
   * @param child The child to add
   */
  private void addChildren(NgramTrie child) {
    children.put(child.getId(), child);
  }

  /**
   * Add an ngram defined as an array of wordIDs. E.g. the ngram ["A", "good", "day"]
   * could be passed as [0, 5, 7]
   *
   * @param childIDs A (possibly) larger ngram
   */
  public void addChildren(int[] childIDs) {
    addChildren(childIDs, 0, childIDs.length);
  }

  /**
   * Add an ngram defined as an array of wordIDs. E.g. the ngram ["A", "good", "day"]
   * could be passed as [0, 5, 7]
   *
   * @param childIDs A (possibly) larger ngram
   * @param start    The beginning of the relevant ngram
   * @param end      The end of the relevant ngram
   */
  public void addChildren(int[] childIDs, int start, int end) {

    if (end - start == 1) {
      NgramTrie child = getChild(childIDs[start]);
      if (child != null) {
        child.increment();
      } else {
        NgramTrie newChild = new NgramTrie(childIDs[start]);
        newChild.increment();
        addChildren(newChild);
      }
    } else {
      NgramTrie child = getChild(childIDs[start]);
      if (child != null) {
        child.addChildren(childIDs, start + 1, end);
      } else {
        NgramTrie newChild = new NgramTrie(childIDs[start]);
        addChildren(newChild);
        newChild.addChildren(childIDs, start + 1, end);
      }
    }
  }


  /**
   * Remove a given word ID from the children of this node
   *
   * @param childID
   */
  public void removeChild(int childID) {
    children.remove(childID);
  }

  /**
   * Extract a node by its ID
   *
   * @param childID The word ID of the node
   * @return The node
   */
  NgramTrie getChild(int childID) {
    return children.get(childID);
  }

  /**
   * Get the number of children of this node with frequency > 0
   * Requires that in training all n-grams were also passed as n-1, n-2 ... n-n+1-grams
   *
   * @return The number of children
   */
  public int getChildrenCount() {
    return getChildrenCount(1);
  }

  /**
   * Get the number of children (at a given depth) of this node with frequency > 0
   * Requires that in training all n-grams were also passed as n-1, n-2 ... n-n+1-grams
   *
   * @param depth The depth; if 1, direct children will be found, if 2, the children-of-children, etc.
   * @return The number of children
   */
  public int getChildrenCount(int depth) {
    if (depth == 1) {
      return children.size();
    } else {
      int childrenCount = 0;
      for (NgramTrie child : children.values()) {
        childrenCount += child.getChildrenCount(depth - 1, 1, Integer.MAX_VALUE);
      }
      return childrenCount;
    }
  }

  /**
   * Get the number of children (at a given depth) of this node with frequency minfreq <= frequency <= maxfreq
   * Requires that in training all n-grams were also passed as n-1, n-2 ... n-n+1-grams
   *
   * @param depth   The depth; if 1, direct children will be found, if 2, the children-of-children, etc.
   * @param maxfreq the lowest accepted frequency
   * @param minfreq the highest accepted frequency
   * @return The number of children
   */
  public int getChildrenCount(int depth, int minfreq, int maxfreq) {

    if (depth == 1) {
      if (minfreq == 1 && maxfreq == 1 && childCache[0] >= 0) {
        return childCache[0];
      } else if (minfreq == 2 && maxfreq == 2 && childCache[1] >= 0) {
        return childCache[1];
      } else if (minfreq == 3 && maxfreq == Integer.MAX_VALUE && childCache[2] >= 0) {
        return childCache[2];
      }
      int relevantChildren = 0;
      for (NgramTrie child : children.values()) {
        int c = child.getCount();
        if (minfreq <= c && c <= maxfreq) {
          relevantChildren++;
        }
      }
      if (minfreq == 1 && maxfreq == 1) {
        childCache[0] = relevantChildren;
      } else if (minfreq == 2 && maxfreq == 2) {
        childCache[1] = relevantChildren;
      } else if (minfreq == 3 && maxfreq == Integer.MAX_VALUE) {
        childCache[2] = relevantChildren;
      }
      return relevantChildren;
    } else {
      int relevantChildren = 0;
      for (NgramTrie child : children.values()) {
        relevantChildren += child.getChildrenCount(depth - 1, minfreq, maxfreq);
      }

      return relevantChildren;
    }
  }

  /**
   * Increase the count of this node by 1
   */
  public void increment() {
    count++;
  }

  /**
   * Decrease the count of this node by 1
   */
  public void decrement() {
    count--;
  }

  /**
   * Get the frequency of this node
   *
   * @return The frequency
   */
  public int getCount() {
    return count;
  }

  /**
   * Get the word ID of this node
   *
   * @return The word ID
   */
  public int getId() {
    return id;
  }

  /**
   * Get all children of this node
   *
   * @return the children
   */
  public Collection<NgramTrie> getChildren() {
    return children.values();
  }

  @Override
  public int compareTo(NgramTrie o) {
    return id - o.getId();
  }

  @Override
  public boolean equals(Object o){
    if (!(o instanceof NgramTrie)) return false;

    return (this.id == ((NgramTrie) o).id);
  }
}
