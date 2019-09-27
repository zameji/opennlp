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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import opennlp.tools.ngram.NgramDictionary;
import opennlp.tools.ngram.NgramDictionaryCompressed;
import opennlp.tools.ngram.NgramDictionaryHashed;

public class NgramEstimatorTest {


  private class MockObject {

    private String[] grams;
    private NgramDictionaryCompressed ngramDictionaryCompressed;
    private NgramDictionaryHashed ngramDictionaryHashed;

    public MockObject() {

      Map<String, Integer> dict = new HashMap<>();
      dict.put("A", 0);
      dict.put("B", 1);
      dict.put("C", 2);
      dict.put("D", 3);

      ngramDictionaryCompressed = new NgramDictionaryCompressed(3, dict, false);
      ngramDictionaryHashed = new NgramDictionaryHashed(5, dict);

      for (int i = 0; i < 4; i++) {
        ngramDictionaryHashed.add(new String[] {"A", "B", "C"});
        ngramDictionaryHashed.add(new String[] {"A", "B"});
        ngramDictionaryHashed.add(new String[] {"A"});

        ngramDictionaryCompressed.add(new String[] {"A", "B", "C"});
        ngramDictionaryCompressed.add(new String[] {"A", "B"});
        ngramDictionaryCompressed.add(new String[] {"A"});
      }

      for (int i = 0; i < 3; i++) {
        ngramDictionaryHashed.add(new String[] {"B", "A", "C"});
        ngramDictionaryHashed.add(new String[] {"B", "A"});
        ngramDictionaryHashed.add(new String[] {"B"});

        ngramDictionaryCompressed.add(new String[] {"B", "A", "C"});
        ngramDictionaryCompressed.add(new String[] {"B", "A"});
        ngramDictionaryCompressed.add(new String[] {"B"});
      }

      for (int i = 0; i < 2; i++) {
        ngramDictionaryHashed.add(new String[] {"B", "D", "C"});
        ngramDictionaryHashed.add(new String[] {"B", "D"});
        ngramDictionaryHashed.add(new String[] {"D"});

        ngramDictionaryCompressed.add(new String[] {"B", "D", "C"});
        ngramDictionaryCompressed.add(new String[] {"B", "D"});
        ngramDictionaryCompressed.add(new String[] {"D"});
      }

      ngramDictionaryHashed.add(new String[] {"A", "B", "A"});
      ngramDictionaryHashed.add(new String[] {"B", "C"});
      ngramDictionaryHashed.add(new String[] {"C"});

      ngramDictionaryCompressed.add(new String[] {"A", "B", "A"});
      ngramDictionaryCompressed.add(new String[] {"B", "C"});
      ngramDictionaryCompressed.add(new String[] {"C"});

      ngramDictionaryCompressed.compress();
    }

    public NgramDictionary getHashed() {
      return ngramDictionaryHashed;
    }

    public NgramDictionary getCompressed() {
      return ngramDictionaryCompressed;
    }

  }

  private NgramDictionary ngramDictionaryCompressed;
  private NgramDictionary ngramDictionaryHashed;

  @Before
  public void doBefore() {
    MockObject m = new MockObject();
    ngramDictionaryCompressed = m.getCompressed();
    ngramDictionaryHashed = m.getHashed();

  }

  @Test
  public void calculateProbabilityChenGoodman() {
    System.out.println("__________");
    System.out.println("Compressed");
    NgramEstimator chenGoodman = new NgramEstimator("chen", ngramDictionaryCompressed, 3);
    System.out.println("Close to 1: " + chenGoodman.calculateProbability("A", "B", "C"));
    System.out.println("Close to 0: " + chenGoodman.calculateProbability("A", "B", "A"));
    System.out.println("Close to 0: " + chenGoodman.calculateProbability("A", "B", "D"));

    System.out.println("__________");
    System.out.println("Hashed");
    chenGoodman = new NgramEstimator("chen", ngramDictionaryHashed, 3);
    System.out.println("Close to 1: " + chenGoodman.calculateProbability("A", "B", "C"));
    System.out.println("Close to 0: " + chenGoodman.calculateProbability("A", "B", "A"));
    System.out.println("Close to 0: " + chenGoodman.calculateProbability("A", "B", "D"));
  }

  @Test
  public void calculateProbabilityMaximumLikelihood() {
    System.out.println("__________");
    System.out.println("Compressed");
    NgramEstimator ml = new NgramEstimator("ml", ngramDictionaryCompressed, 3);
    System.out.println("Close to 1: " + ml.calculateProbability("A", "B", "C"));
    System.out.println("Close to 0: " + ml.calculateProbability("A", "B", "A"));

    System.out.println("__________");
    System.out.println("Hashed");
    ml = new NgramEstimator("ml", ngramDictionaryHashed, 3);
    System.out.println("Close to 1: " + ml.calculateProbability("A", "B", "C"));
    System.out.println("Close to 0: " + ml.calculateProbability("A", "B", "A"));
  }

}
