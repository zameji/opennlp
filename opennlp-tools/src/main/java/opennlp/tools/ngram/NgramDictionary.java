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

public interface NgramDictionary {

  /**
   * Get the size of the corpus
   *
   * @return The size
   */
  int getCorpusSize();

  /**
   * Get the number of ngrams at a given depth with frequency minfreq <= f <= maxfreq
   * @param n > 0, size of n-gram
   * @param minfreq Lowest accepted frequency
   * @param maxfreq Highest accepted frequency
   * @return The count of n-grams with matching frequency
   */
  int getNGramCount(int n, int minfreq, int maxfreq);

  /**
   * Get the number of various ngram of a given size
   *
   * @param n The size of the ngram (i.e. n)
   * @return The number of various ngrams of this size
   */
  int getNGramCountSum(int n);

  /**
   * Get the number of distinct ngrams that differ from the one submitted only by the last element
   * @param arrayWithGram A text document
   * @param start The beginning of the relevant n-gram
   * @param end The end of the relevant n-gram
   * @return The number of distinct ngrams
   */
  int getSiblingCount(String[] arrayWithGram, int start, int end);

  /**
   * Get the number of distinct ngrams that differ from the one submitted only by the last element
   * and have frequency that fulfills minfreq <= frequency <= maxfreq
   * @param arrayWithGram A text document
   * @param start The beginning of the relevant n-gram
   * @param end The end of the relevant n-gram
   * @param minfreq Lowest accepted frequency
   * @param maxfreq Highest accepted frequency
   * @return The number of distinct ngrams
   */
  int getSiblingCount(String[] arrayWithGram, int start, int end, int minfreq, int maxfreq);

  /**
   * Get the sum of frequencies of all ngrams that differ from the current one only by the last element
   * @param arrayWithGram A text document
   * @param start The beginning of the relevant n-gram
   * @param end The end of the relevant n-gram
   * @return The summed frequency
   */
  int getSiblingCountSum(String[] arrayWithGram, int start, int end);

  /**
   * Add a new n-gram (or increase its count by 1). All n-grams must also be added as n-1, n-2, ...
   * n-n+1-grams. If items were added after the creation of an NgramEstimator object, its update() method
   * must be called before prediction/probability calculation.
   * @param gram Ngram to add
   */
  void add(String... gram);

  /**
   * Add a new n-gram (or increase its count by 1). All n-grams must also be added as n-1, n-2, ...
   * n-n+1-grams. If items were added after the creation of an NgramEstimator object, its update() method
   * must be called before prediction/probability calculation.
   * @param gram A large n-gram
   * @param start The beginning of the relevant n-gram
   * @param end The end of the relevant ngram
   */
  void add(String[] gram, Integer start, Integer end);

  /**
   * Get the frequency of an ngram
   * @param gram Ngram to find
   */
  int get(String... gram);

  /**
   * Get the frequency of an ngram
   * @param gram A large n-gram
   * @param start The beginning of the relevant n-gram
   * @param end The end of the relevant ngram
   */
  int get(String[] gram, int start, int end);

  /**
   * Get all ngrams that differ from the current one only by the last element
   * @param ngram Ngram to find
   * @return All ngrams that differ from the one submitted by the last element, sorted by the word ID of
   * the last element
   */
  String[][] getSiblings(String... ngram);

  /**
   * Get all ngrams that differ from the current one only by the last element
   * @param ngram A large n-gram
   * @param start The beginning of the relevant n-gram
   * @param end The end of the relevant ngram
   * @return All ngrams that differ from the one submitted by the last element, sorted by the word ID of
   * the last element
   */
  String[][] getSiblings(String[] ngram, int start, int end);
}
