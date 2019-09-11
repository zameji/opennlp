package opennlp.tools.ngram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NgramTrie implements Comparable<NgramTrie> {

  private final Map<Integer, NgramTrie> children = new HashMap<>();
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
      return getChildrenCount(depth, 1, Integer.MAX_VALUE);
    } else {
      int childrenCount = 0;
      for (Map.Entry<Integer, NgramTrie> child : children.entrySet()) {
        childrenCount += child.getValue().getChildrenCount(depth - 1, 1, Integer.MAX_VALUE);
      }
      return childrenCount;
    }
  }


  public int getChildrenCount(int depth, int minfreq, int maxfreq) {
    if (depth == 1) {
      int relevantChildren = 0;
      for (Map.Entry<Integer, NgramTrie> child : children.entrySet()) {
        int c = child.getValue().getCount();
        if (minfreq <= c && c <= maxfreq) {
          relevantChildren++;
        }
      }
      return relevantChildren;
    } else {
      int relevantChildren = 0;
      for (Map.Entry<Integer, NgramTrie> child : children.entrySet()) {
        relevantChildren += child.getValue().getChildrenCount(depth - 1, minfreq, maxfreq);
      }
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

  public List<NgramTrie> getChildren() {
    List<NgramTrie> childrenList = new ArrayList<>();
    for (Map.Entry<Integer, NgramTrie> child : children.entrySet()) {
      childrenList.add(child.getValue());
    }
    return childrenList;
  }

  @Override
  public int compareTo(NgramTrie o) {
    return id - o.getId();
  }
}
