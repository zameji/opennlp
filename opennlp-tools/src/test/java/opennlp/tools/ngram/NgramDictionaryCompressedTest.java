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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class NgramDictionaryCompressedTest {

  private class MockObject {

    public NgramDictionaryCompressed ngramDictionary;
    private String[][] grams = new String[11][];

    public MockObject() {

      Map<String, Integer> dict = new HashMap<>();
      dict.put("A", 0);
      dict.put("B", 1);
      dict.put("C", 2);
      dict.put("D", 3);

      ngramDictionary = new NgramDictionaryCompressed(3, dict, false);

      for (int i = 0; i < 4; i++) {
        ngramDictionary.add(new String[] {"A", "B", "C"});
        ngramDictionary.add(new String[] {"A", "B"});
        ngramDictionary.add(new String[] {"A"});
      }

      for (int i = 0; i < 3; i++) {
        ngramDictionary.add(new String[] {"B", "A", "C"});
        ngramDictionary.add(new String[] {"B", "A"});
        ngramDictionary.add(new String[] {"B"});
      }

      for (int i = 0; i < 2; i++) {
        ngramDictionary.add(new String[] {"B", "D", "C"});
        ngramDictionary.add(new String[] {"B", "D"});
        ngramDictionary.add(new String[] {"D"});
      }

      ngramDictionary.add(new String[] {"A", "B", "A"});
      ngramDictionary.add(new String[] {"B", "C"});
      ngramDictionary.add(new String[] {"C"});

      ngramDictionary.compress();
      System.out.println(ngramDictionary.toString());
    }

    public NgramDictionary get() {
      return ngramDictionary;
    }

  }

  private NgramDictionary ngramDictionary;

  @Before
  public void doBefore() {
    MockObject m = new MockObject();
    ngramDictionary = m.get();
  }

  @Test
  public void getCorpusSize() {
    assertEquals(ngramDictionary.getCorpusSize(), 10);
  }

//  @Test
//  public void getNGramCount() {
//    assertEquals(4, ngramDictionary.getNGramCount(1));
//    assertEquals(4, ngramDictionary.getNGramCount(2));
//    assertEquals(4, ngramDictionary.getNGramCount(3));
//  }

  @Test
  public void testGetNGramCount() {
    assertEquals(1, ngramDictionary.getNGramCount(1, 4, 5));
    assertEquals(2, ngramDictionary.getNGramCount(1, 3, 4));
    assertEquals(1, ngramDictionary.getNGramCount(1, 1, 1));

    assertEquals(2, ngramDictionary.getNGramCount(2, 1, 2));
    assertEquals(1, ngramDictionary.getNGramCount(2, 1, 1));
    assertEquals(0, ngramDictionary.getNGramCount(2, 5, 6));

    assertEquals(3, ngramDictionary.getNGramCount(3, 2, 5));
    assertEquals(1, ngramDictionary.getNGramCount(3, 1, 1));
  }


  @Test
  public void getSiblings() {
    String[][] exp = {{"A", "B", "A"},
        {"A", "B", "C"}};

    assertArrayEquals(exp, ngramDictionary.getSiblings("A", "B"));

  }
//  @Test
//  public void getSiblingCount() {
//    assertEquals(5, ngramDictionary.getSiblingCount("A"));
//    assertEquals(5, ngramDictionary.getSiblingCount("F"));
//    assertEquals(3, ngramDictionary.getSiblingCount("B", "C"));
//    assertEquals(0, ngramDictionary.getSiblingCount("D", "B", "A"));
//    assertEquals(0, ngramDictionary.getSiblingCount("A", "F", "B"));
//  }

  @Test
  public void testGetSiblingCount() {
    assertEquals(5, ngramDictionary.getSiblingCount(new String[] {"A"}, 0, 1));
    assertEquals(5, ngramDictionary.getSiblingCount(new String[] {"B", "F"}, 1, 2));
    assertEquals(3, ngramDictionary.getSiblingCount(new String[] {"B", "C", "A"}, 0, 2));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"D", "B", "A"}, 0, 3));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"A", "F", "B"}, 0, 3));
  }

  @Test
  public void testGetSiblingCount1() {
    assertEquals(1, ngramDictionary.getSiblingCount(new String[] {"A"}, 0, 1, 2, 2));
    assertEquals(4, ngramDictionary.getSiblingCount(new String[] {"B", "F"}, 1, 2, 1, 5));
    assertEquals(3, ngramDictionary.getSiblingCount(new String[] {"B", "C", "A"}, 0, 2, 1, 5));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"D", "B", "A"}, 0, 3, 0, 0));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"A", "F", "B"}, 0, 3, 1, 5));

    assertEquals(1, ngramDictionary.getSiblingCount(new String[] {"A", "B", "F"}, 0, 3, 1, 1));
    assertEquals(0, ngramDictionary.getSiblingCount(new String[] {"A", "B", "F"}, 0, 3, 2, 2));
    assertEquals(1, ngramDictionary.getSiblingCount(new String[] {"A", "B", "F"}, 0, 3, 3,
        Integer.MAX_VALUE));

  }

  @Test
  public void get() {
    assertEquals(4, ngramDictionary.get("A", "B"));
    assertEquals(0, ngramDictionary.get("F"));
    assertEquals(0, ngramDictionary.get("A", "F", "B"));
  }

  @Test
  public void testGet() {
    assertEquals(4, ngramDictionary.get(new String[] {"A", "B", "F"}, 0, 1));
    assertEquals(0, ngramDictionary.get(new String[] {"A", "F", "B"}, 0, 2));
    assertEquals(1, ngramDictionary.get(new String[] {"A", "B", "C"}, 1, 3));
  }


  @Test
  public void testAdd() {
    ngramDictionary.add(new String[] {"A", "B", "F"}, 0, 3);
    assertEquals(1, ngramDictionary.get(new String[] {"A", "B", "F"}, 0, 3));
    assertEquals(0, ngramDictionary.get(new String[] {"F"}, 0, 1));
    assertEquals(0, ngramDictionary.get(new String[] {"A", "F", "B"}, 0, 2));
  }


}
