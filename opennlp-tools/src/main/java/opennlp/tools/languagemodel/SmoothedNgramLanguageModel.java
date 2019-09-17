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


import opennlp.tools.ngram.NgramDictionary;
import opennlp.tools.ngram.NgramDictionaryCompressed;
import opennlp.tools.ngram.NgramDictionaryHashed;
import opennlp.tools.util.Cache;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.StringList;
import opennlp.tools.util.model.BaseModel;
import opennlp.tools.util.model.SerializableArtifact;

public class SmoothedNgramLanguageModel extends BaseModel implements LanguageModel, SerializableArtifact {

  private static final int DEFAULT_N = 3;
  private static final String DEFAULT_SMOOTHING = "Chen-Goodman";
  private final boolean compressDictionary;
  private final NgramDictionary ngrams;
  private Map<String, Integer> vocabulary;
  private final int n;
  private final Cache<String[], Double> LRUCache;
  private final NgramEstimator ngramEstimator;

  public SmoothedNgramLanguageModel(String language, int n, String smoothing,
                                    Boolean compressDictionary, NgramDictionary ngrams,
                                    Map<String, Integer> vocabulary) {
    super("smoothed Ngram model", language, null);
    this.n = n;
    this.ngrams = ngrams;
    this.vocabulary = vocabulary;

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

    System.out.println("Building the dictionary");
    Map<String, Integer> dictionary = buildDictionary(in, factory.getMinUnigramFrequency());

    System.out.println("Collecting ngrams");
    //we make the vocabulary static, because we have already seen all words during buildDictionary()
    NgramDictionary ngrams = collectNgrams(in, dictionary, factory.getNgramSize(),
        factory.getCompression(), true);

    return new SmoothedNgramLanguageModel(factory.getSmoothing(), factory.getNgramSize(),
        factory.getSmoothing(), factory.getCompression(), ngrams, dictionary);
  }

  /**
   * Creates a dictionary of the words in the corpus
   *
   * @param in A stream of tokenized documents
   * @return A dictionary assigning each token an integer alias
   * @throws IOException If it fails to read the input
   */
  private static Map<String, Integer> buildDictionary(ObjectStream<String[]> in,
                                                      Integer minUnigramFrequency) throws IOException {

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
    //do this only until word frequency > minUnigramFrequency
    dictionary = new HashMap<>();
    for (int j = vocabulary.length - 1; j >= 0; j--) {
      if (vocabulary[j].count < minUnigramFrequency) {
        break;
      }
      dictionary.put(vocabulary[j].word, j);
    }
    dictionary.put("<OOV>", dictionary.size());

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
   * @throws IOException if it fails to read the input
   */
  private static NgramDictionary collectNgrams(ObjectStream<String[]> in, Map<String,
      Integer> dictionary, Integer ngramSize, Boolean compressedDictionary, Boolean staticVocabulary) throws IOException {

    NgramDictionary ngrams;

    //We make the vocabulary static, because we've already seen all the items in the stream
    if (compressedDictionary) {
      ngrams = new NgramDictionaryCompressed(ngramSize, dictionary, staticVocabulary);
    } else {
      ngrams = new NgramDictionaryHashed(dictionary, staticVocabulary);
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

  public List[] calculateProbabilities(String... tokens) {
    List[] results = new List[2];
    results[0] = new ArrayList<String[]>();
    results[1] = new ArrayList<Double>();

    if (tokens == null) {
      return results;
    }

    int depth = Math.min(tokens.length, n);

    for (int i = 0; i + depth <= tokens.length; i++) {
      String[] gram = new String[depth];
      for (int j = 0; j < depth; j++) {
        gram[j] = (vocabulary.containsKey(tokens[i + j])) ? tokens[i + j] : "<OOV>";
      }
      results[0].add(gram);
      results[1].add(calculateProbability(gram));
    }
    return results;
  }

  @Deprecated
  public StringList predictNextTokens(StringList tokens) {
    if (tokens == null) {
      return null;
    }
    StringList result = null;
    String[] tokenArray = new String[tokens.size()];

    for (int i = 0; i < tokens.size(); i++) {
      tokenArray[i] = tokens.getToken(i);
    }

    String[] resultArray = predictNextTokens(tokenArray);

    if (resultArray != null) {
      result = new StringList(resultArray);
    }

    return result;

  }

  @Override
  public String[] predictNextTokens(String... tokens) {
    if (tokens == null) {
      return null;
    }

    String[] result = null;
    String[][] possibleContinuations = ngrams.getSiblings(tokens);

    int attempt = 1;
    while (possibleContinuations == null) {
      if (attempt > tokens.length) {
        return result;
      }
      possibleContinuations = ngrams.getSiblings(tokens, attempt, tokens.length);
      attempt++;
    }

    double maxProbability = Double.MIN_VALUE;
    for (int i = 0; i < possibleContinuations.length; i++) {
      String[] continuation = possibleContinuations[i];
      double prob = calculateProbability(continuation);

      if (prob > maxProbability) {
        result = continuation;
        maxProbability = prob;
      }
    }

    return result;
  }

  @Override
  public Class<?> getArtifactSerializerClass() {
    return null;
  }
}
