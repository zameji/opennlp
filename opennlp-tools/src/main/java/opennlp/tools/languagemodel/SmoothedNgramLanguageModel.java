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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import opennlp.tools.ngram.NgramDictionary;
import opennlp.tools.ngram.NgramDictionaryCompressed;
import opennlp.tools.ngram.NgramDictionaryHashed;
import opennlp.tools.util.Cache;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.StringList;
import opennlp.tools.util.model.BaseModel;

public class SmoothedNgramLanguageModel extends BaseModel implements LanguageModel {

  private static final int DEFAULT_N = 3;
  private static final String DEFAULT_SMOOTHING = "Chen-Goodman";
  private final boolean compressDictionary;
  private final NgramDictionary ngrams;
  private final int n;
  private final Cache<String[], Double> LRUCache;
  private final NgramEstimator ngramEstimator;

  public SmoothedNgramLanguageModel(String language, int n, String smoothing,
                                    NgramDictionary ngrams, Boolean compressDictionary) {
    super("smoothed Ngram model", language, null);
    this.n = n;
    this.ngrams = ngrams;

    if (smoothing == null) {
      this.ngramEstimator = new NgramEstimator(DEFAULT_SMOOTHING, ngrams, n);
    } else {
      System.out.println("Smoothed model of " + n + "-grams");
      this.ngramEstimator = new NgramEstimator(smoothing, ngrams, n);
    }

    this.compressDictionary = compressDictionary;

    //todo: make the cache capacity smarter (e.g. cover x% of the data)
    this.LRUCache = new Cache<>(1000);

  }

  /**
   * Train an ngram model with predefined smoothing
   *
   * @param in
   * @param factory
   * @return
   * @throws IOException
   */
  public static SmoothedNgramLanguageModel train(ObjectStream<String[]> in,
                                                 NgramLMFactory factory) throws IOException {

    //1st step: create dictionary
    System.out.println("Building the dictionary");
    Map<String, Integer> dictionary = buildDictionary(in);

    System.out.println("Collecting ngrams");
    NgramDictionary ngrams = collectNgrams(in, dictionary, factory.getNgramSize(),
        factory.getCompression());

    return new SmoothedNgramLanguageModel(factory.getSmoothing(), factory.getNgramSize(),
        factory.getSmoothing(),
        ngrams, factory.getCompression());
  }

  /**
   * Creates a dictionary of the words in the corpus
   *
   * @param in A stream of tokenized documents
   * @return A dictionary assigning each token an integer alias
   * @throws IOException If it fails to read the input
   */
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

  /**
   * Collects the ngrams found in the corpus, notes their size
   *
   * @param in                   A stream of tokenized documents
   * @param dictionary           A dictionary assigning each token an integer alias
   * @param ngramSize            The maximum size of ngrams to be collected (lower order n-grams will be
   *                             included
   * @param compressedDictionary Should the ngrams be stored in a way that minimizes space?
   * @return A dictionary assigning each ngram a count
   * @throws IOException If it fails to read the input
   */
  private static NgramDictionary collectNgrams(ObjectStream<String[]> in, Map<String,
      Integer> dictionary, Integer ngramSize, Boolean compressedDictionary) throws IOException {

    NgramDictionary ngrams;

    if (compressedDictionary) {
      ngrams = new NgramDictionaryCompressed(ngramSize, dictionary);
    } else {
      ngrams = new NgramDictionaryHashed(dictionary);
    }

    String[] next = in.read();

    while (next != null) {

      for (int n = ngramSize; n > 0; n--) {
        for (int i = 0; i + n <= next.length; i++) {
          ngrams.add(next, i, i + n);
        }
      }
      next = in.read();
    }

    //todo: find a more elegant way of compressing?
    // How to do this without adding the method compress() to the interface?
    if (compressedDictionary) {
      try {
        NgramDictionaryCompressed ngramsCompressible = (NgramDictionaryCompressed) ngrams;
        ngramsCompressible.compress();
        ngrams = ngramsCompressible;
      } catch (Exception e) {
        System.err.println(e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
      }
    }

    //todo: make the ngram dictionary immutable

    return ngrams;

  }

  @Deprecated
  public double calculateProbability(StringList tokens) {
    String[] tokenArray = new String[tokens.size()];
    for (int i = 0; i < tokens.size(); i++) {
      tokenArray[i] = tokens.getToken(i);
    }
    return calculateProbability(tokenArray);
  }

  @Override
  public double calculateProbability(String... tokens) {
    if (LRUCache.containsKey(tokens)) {
      return LRUCache.get(tokens);
    }
    double prob = ngramEstimator.calculateProbability(tokens);
    LRUCache.put(tokens, prob);
    return prob;
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
