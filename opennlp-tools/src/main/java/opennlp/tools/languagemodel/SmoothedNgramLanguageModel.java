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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.util.Cache;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.StringList;
import opennlp.tools.util.model.BaseModel;

public class SmoothedNgramLanguageModel extends BaseModel implements LanguageModel {

  private final int DEFAULT_N = 3;
  private final String DEFAULT_SMOOTHING = "Chen-Goodman";

  private final NgramDictionary ngrams;
  private final int n;
  private final String smoothing;
  private final Cache<String[], Double> LRUCache;

  public SmoothedNgramLanguageModel(String language, int n, String smoothing,
                                    NgramDictionary ngrams) {
    super("smoothed Ngram model", language, null);
    this.n = n;
    this.ngrams = ngrams;
    this.smoothing = smoothing;
    //todo: make the cache capacity smarter (e.g. cover x% of the data)
    this.LRUCache = new Cache<>(1000);
  }

  public static SmoothedNgramLanguageModel train(ObjectStream<String[]> in,
                                                 NgramLMFactory factory) throws IOException {

    //1st step: create dictionary
    System.out.println("Building the dictionary");
    Map<String, Integer> dictionary = buildDictionary(in);

    System.out.println("Collecting ngrams");
    NgramDictionary ngrams = collectNgrams(in, dictionary, factory.getNgramSize());

    return new SmoothedNgramLanguageModel(factory.getSmoothing(), 3, factory.getSmoothing(), ngrams);
  }

  private static Map<String, Integer> buildDictionary(ObjectStream<String[]> in) throws IOException {

    class Token implements Comparable<Token> {

      public final String word;
      public int count;

      public Token(String word, Integer count) {
        this.word = word;
        this.count = count;
      }

      @Override
      public int compareTo(Token other) {
        return (this.count - other.count);
      }
    }


    Map<String, Integer> dictionary = new HashMap<>();

    String[] tokens = in.read();
    while (tokens != null) {
      for (String token : tokens) {
        if (dictionary.containsKey(token)) {
          dictionary.replace(token, dictionary.get(token) + 1);
        } else {
          dictionary.put(token, 1);
        }
      }

      tokens = in.read();
    }

    in.reset();

    //2nd step: sort the dictionary
    Token[] vocabulary = new Token[dictionary.size()];
    int i = 0;
    for (Map.Entry<String, Integer> token : dictionary.entrySet()) {
      vocabulary[i] = new Token(token.getKey(), token.getValue());
      i++;
    }

    Arrays.sort(vocabulary);

    //3rd step: give most frequent words the lowest IDs
    dictionary = new HashMap<>();
    for (int j = vocabulary.length - 1; j >= 0; j--) {
      dictionary.put(vocabulary[j].word, j);
    }

    return dictionary;

  }

  private static NgramDictionary collectNgrams(ObjectStream<String[]> in, Map<String,
      Integer> dictionary, Integer ngramSize) throws IOException {

    NgramDictionary ngrams = new NgramDictionary(ngramSize, dictionary);

    //add all ngrams of size 1<=n<=N from each array returned by the input stream
    //assume arrays are independent, do not go over their boundary
    String[] next = in.read();

    int readSentences = 0;
    int readWords = 0;
    long startTime = System.currentTimeMillis();
    long lastReportTime = startTime;
    List<Integer> speeds = new ArrayList<>();
    List<Long> times = new ArrayList<>();
    while (next != null) {
      long reportWindow = System.currentTimeMillis() - lastReportTime;
      if (reportWindow > 1000) {
        speeds.add(readWords);
        times.add(reportWindow);
        lastReportTime = System.currentTimeMillis();
      }
//      if (readSentences % 100 == 0){
//        System.out.println("\tSentences: " + readSentences);
//      }
//      if (readWords % 10000 == 0){
//        System.out.println("\tWords: " + readWords);
//      }
      //go backwards to create the words in the first round
      for (int n = ngramSize; n > 0; n--) {
        for (int i = 0; i + n <= next.length; i++) {
          ngrams.add(next, i, i + n);
        }
      }
      readWords += next.length;
      readSentences++;
      next = in.read();

    }

    long endTime = System.currentTimeMillis();
    System.out.println("Processed " + readWords + "words " +
        "in " + (endTime - startTime) / 1000 + " seconds.\nPerformance stats:" +
        "Words/second: " + readWords / ((endTime - startTime) / 1000) +
        "\nSentences/second: " + readSentences / ((endTime - startTime) / 1000) +
        "\nwords/sentence: " + readWords / readSentences);
//    for (int i=1; i < times.size();i++){
//      System.out.println(i + ": " + (double) (speeds.get(i)-speeds.get(i-1))/times.get(i) + " wps");
//    }
    System.out.println();
    //todo: ngrams.compress();
    //System.out.println(ngrams.toString());
    return ngrams;

  }


  @Deprecated
  public double calculateProbability(StringList tokens) {
    return 0;
  }

  @Override
  public double calculateProbability(String... tokens) {
//    if (LRUCache.containsKey(tokens)) {
//      return LRUCache.get(tokens);
//    }
    double prob;
    switch (smoothing) {
      case "Chen-Goodman":
        prob = chenGoodman(tokens);
        break;
      default:
        prob = maximumLikelihood(tokens);
        break;
    }
//    LRUCache.put(tokens, prob);
    return prob;
  }

  private double chenGoodman(String... tokens) {
    double prob = maximumLikelihood(tokens);//todo: implement chen-goodman

    return prob;
  }

  private double maximumLikelihood(String... tokens) {
    double c = ngrams.get(tokens, 0, tokens.length);
    if (c == 0) {
      return c;
    }
    if (tokens.length > 1) {
      return c / ngrams.get(tokens, 0, tokens.length - 1);
    } else {
      return c / 100000;
    }
  }

  @Deprecated
  public StringList predictNextTokens(StringList tokens) {
    return null;
  }

  @Override
  public String[] predictNextTokens(String... tokens) {
    return new String[0];
  }
}
