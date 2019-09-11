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
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;


import opennlp.tools.util.Cache;

class NgramTrie implements Comparable<NgramTrie> {

  private final Cache<List<Integer>, Integer> childCache = new Cache(4);
  private final TreeMap<Integer, NgramTrie> children = new TreeMap<>();
  private final int id;
  private int count = 0;

  public NgramTrie(int id) {
    this.id = id;
  }

  private void addChildren(NgramTrie child) {
    children.put(child.getId(), child);
  }

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

  public void addChildren(int[] childIDs) {
    addChildren(childIDs, 0, childIDs.length);
  }

  public void removeChild(int childID) {
    children.remove(childID);
  }

  NgramTrie getChild(int childID) {
    return children.get(childID);
  }

  public int getChildrenCount() {
    return getChildrenCount(1);
  }

  public int getChildrenCount(int depth) {
    if (depth == 1) {
      //todo: we could return just children.size(), assuming that all n-grams are also
      // represented as n-1 grams, n-2 grams etc.
//      return getChildrenCount(depth, 1, Integer.MAX_VALUE);
      return children.size();
    } else {
      int childrenCount = 0;
      for (NgramTrie child : children.values()) {
        childrenCount += child.getChildrenCount(depth - 1, 1, Integer.MAX_VALUE);
      }
      return childrenCount;
    }
  }

  public int getChildrenCount(int depth, int minfreq, int maxfreq) {
    List<Integer> request = new ArrayList<>();
    request.add(depth);
    request.add(minfreq);
    request.add(maxfreq);

    if (childCache.containsKey(request)) {
      return childCache.get(request);
    }

    if (depth == 1) {
      int relevantChildren = 0;
      for (NgramTrie child : children.values()) {
        int c = child.getCount();
        if (minfreq <= c && c <= maxfreq) {
          relevantChildren++;
        }
      }
      childCache.put(request, relevantChildren);
      return relevantChildren;
    } else {
      int relevantChildren = 0;
      for (NgramTrie child : children.values()) {
        relevantChildren += child.getChildrenCount(depth - 1, minfreq, maxfreq);
      }

      childCache.put(request, relevantChildren);
      return relevantChildren;
    }
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

  public Collection<NgramTrie> getChildren() {
    return children.values();
  }

  @Override
  public int compareTo(NgramTrie o) {
    return id - o.getId();
  }
}
