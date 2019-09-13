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


import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NgramDictionaryTest {

  @Test
  public void test_uncompressed() {
    NgramDictionaryHashed t = new NgramDictionaryHashed(null);
    String[] s = new String[] {"A", "B", "C", "D"};
    t.add(s, 0, 3);
    t.add(s, 1, 4);
    t.add(s, 0, 2);
    t.add(s, 1, 3);
    t.add(s, 2, 4);
    t.add(s, 0, 1);
    t.add(s, 1, 2);
    t.add(s, 2, 3);
    t.add(s, 3, 4);
    System.out.println(t.toString());

    Map<String, Integer> dict = new HashMap<>();
    dict.put("A", 0);
    dict.put("B", 1);
    dict.put("C", 2);
    dict.put("D", 3);

    t = new NgramDictionaryHashed(dict);
    String[] trigrams = new String[] {"A", "AA", "AAC", "AC", "B", "BB", "BBC", "BBD",
        "BC", "BCD", "BD", "CA", "CD", "DB", "DBB", "DBC", "DDD"};
    for (String trigram : trigrams) {
      t.add(trigram.split(""));
    }

    System.out.println(t.toString());
  }

  @Test
  public void test_compressed() {
    NgramDictionaryCompressed t = new NgramDictionaryCompressed(3, null);
    String[] s = new String[] {"A", "B", "C", "D"};
    t.add(s, 0, 3);
    t.add(s, 1, 4);
    t.add(s, 0, 2);
    t.add(s, 1, 3);
    t.add(s, 2, 4);
    t.add(s, 0, 1);
    t.add(s, 1, 2);
    t.add(s, 2, 3);
    t.add(s, 3, 4);
    t.compress();
    System.out.println(t.toString());

    Map<String, Integer> dict = new HashMap<>();
    dict.put("A", 0);
    dict.put("B", 1);
    dict.put("C", 2);
    dict.put("D", 3);

    t = new NgramDictionaryCompressed(3, dict);
    String[] trigrams = new String[] {"A", "AA", "AAC", "AC", "B", "BB", "BBC", "BBD",
        "BC", "BCD", "BD", "CA", "CD", "DB", "DBB", "DBC", "DDD"};
    for (String trigram : trigrams) {
      t.add(trigram.split(""));
    }

    t.compress();
    System.out.println(t.toString());
  }

  @Test
  public void getCount() {
    NgramDictionaryCompressed t = new NgramDictionaryCompressed(5, null);
    t.add(new String[] {"A", "B", "C", "D", "E"});
    t.add(new String[] {"A", "B", "C"});
    t.add(new String[] {"B", "C", "D"});
    t.add(new String[] {"A", "B"});
    t.add(new String[] {"B", "C"});
    t.add(new String[] {"B", "C"});
    t.add(new String[] {"C", "D"});
    t.add(new String[] {"A"});
    t.add(new String[] {"B"});
    t.add(new String[] {"C"});
    t.add(new String[] {"D"});

    t.compress();
    System.out.println(t.toString());

    assertEquals(1, (int) t.get(new String[] {"D"}));
    assertEquals(2, (int) t.get(new String[] {"B", "C"}));
    assertEquals(1, (int) t.get(new String[] {"B", "C", "D"}));
    assertEquals(0, (int) t.get(new String[] {"D", "C", "D"}));
    assertEquals(2, (int) t.get(new String[] {"B", "C", "D"}, 0, 2));
    assertEquals(1, (int) t.get(new String[] {"B", "C", "D"}, 0, 1));
    assertEquals(1, (int) t.get(new String[] {"A", "B", "C", "D", "E"}));
    assertEquals(0, (int) t.get(new String[] {"A", "B", "C", "D", "F"}));

    t.add("B", "C");
    t.add("D", "C", "D");
//    assertEquals(3, (int) t.get(new String[] {"B", "C"}));
    assertEquals(1, (int) t.get(new String[] {"D", "C", "D"}));

    NgramDictionaryHashed th = new NgramDictionaryHashed(null);
    th.add(new String[] {"A", "B", "C"});
    th.add(new String[] {"B", "C", "D"});
    th.add(new String[] {"A", "B"});
    th.add(new String[] {"B", "C"});
    th.add(new String[] {"B", "C"});
    th.add(new String[] {"C", "D"});
    th.add(new String[] {"A"});
    th.add(new String[] {"B"});
    th.add(new String[] {"C"});
    th.add(new String[] {"D"});


    assertEquals(1, (int) th.get(new String[] {"D"}));
    assertEquals(2, (int) th.get(new String[] {"B", "C"}));
    assertEquals(1, (int) th.get(new String[] {"B", "C", "D"}));
    assertEquals(0, (int) th.get(new String[] {"D", "C", "D"}));
    assertEquals(0, (int) th.get(new String[] {"E"}));
    assertEquals(2, (int) th.get(new String[] {"B", "C", "D"}, 0, 2));
    assertEquals(1, (int) th.get(new String[] {"B", "C", "D"}, 0, 1));

  }
}
