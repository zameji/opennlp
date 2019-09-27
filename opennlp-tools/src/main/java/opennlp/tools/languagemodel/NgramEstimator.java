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

import java.util.Arrays;

import opennlp.tools.ngram.NgramDictionary;

public class NgramEstimator {

  private class chenGoodmanEstimator implements probabilityEstimator {

    private final int wordCount;
    private double[][] D;


    public chenGoodmanEstimator() {

      //On creation we cache the discounts to be used for estimation
      // Get the discounting parameter D (Eq. 17 of Chen & Goodman 1999)
      D = getDiscounts();

      System.out.println("D");
      for (int depth = 0; depth < NUMBER_OF_LEVELS; depth++) {
        System.out.println(Arrays.toString(D[depth]));
      }

      wordCount = ngramDictionary.getCorpusSize();

    }

    /**
     * Get the sizes of the discounting parameters
     *
     * @return
     */
    private double[][] getDiscounts() {

      //todo: allow optimization on a held-out corpus

      double[][] n = new double[NUMBER_OF_LEVELS][4]; //needed to get the estimates of D
      for (int depth = 0; depth < NUMBER_OF_LEVELS; depth++) {
        for (int freq = 0; freq < 4; freq++) {
          n[depth][freq] = ngramDictionary.getNGramCount(depth + 1, freq + 1, freq + 1);
        }
      }

      double[] Y = new double[NUMBER_OF_LEVELS];
      for (int depth = 0; depth < NUMBER_OF_LEVELS; depth++) {
        Y[depth] = n[depth][0] / (n[depth][0] + 2 * n[depth][1]);
      }

      double[][] D = new double[NUMBER_OF_LEVELS][4];
      for (int depth = 0; depth < NUMBER_OF_LEVELS; depth++) {
        D[depth][0] = 0.0;
        for (int freq = 1; freq <= 3; freq++) {
          double currentD =
              (freq) - (((freq + 1) * Y[depth]) * (n[depth][freq] / n[depth][freq - 1]));
          D[depth][freq] = currentD;
        }
      }

      return D;
    }

    /**
     * Update the internal caches of the estimator. Use if new items were added to the ngramDictionary
     * or the counts changed.
     */
    @Override
    public void update() {
      D = getDiscounts();
    }

    /**
     * Get the probability of an ngram
     *
     * @param tokens The ngram
     * @return The probability
     */
    @Override
    public double calculateProbability(String... tokens) {
      int start = (tokens.length > NUMBER_OF_LEVELS) ? tokens.length - NUMBER_OF_LEVELS : 0;
      return calculateProbability(tokens, start, tokens.length);
    }

    /**
     * Get the probability through the maximum likelihood algorithm
     *
     * @param tokens A text
     * @param start  Starting index of the ngram to evaluate
     * @param end    End index of the ngram to evaluate
     * @return The probability
     */
    private double calculateProbability(String[] tokens, int start, int end) {
      if (end - start > NUMBER_OF_LEVELS) {
        return calculateProbability(tokens, start + 1, end);
      }
      double c = ngramDictionary.get(tokens, start, end);
      double discount;
      if (c < 3) {
        discount = D[end - start - 1][(int) c];
      } else {
        discount = D[end - start - 1][3];
      }
      double siblings = ngramDictionary.getSiblingCountSum(tokens, start, end);
      double prob = Math.max(c - discount, 0) / siblings;
      ;
      if (end - start == 1) {
        return prob;
      } else {
        if (siblings > 0) {
          double gamma = 0.0;

          for (int freq = 1; freq <= 3; freq++) {
            int maxfreq = (freq == 3) ? Integer.MAX_VALUE : freq;
            double siblingWithFreq = ngramDictionary
                .getSiblingCount(tokens, start, end, freq, maxfreq);
            gamma += (D[end - start - 1][freq] * siblingWithFreq) / siblings;
          }

          return prob + gamma * calculateProbability(tokens, start + 1, end);
        } else {
          return calculateProbability(tokens, start + 1, end);
        }
      }
    }

  }

  private class maximumLikelihoodEstimator implements probabilityEstimator {

    private int wordCount;

    public maximumLikelihoodEstimator() {
      wordCount = ngramDictionary.getCorpusSize();
    }

    /**
     * Get the probability through the maximum likelihood algorithm
     *
     * @param tokens The ngram to evaluate
     * @return The probability
     */
    @Override
    public double calculateProbability(String... tokens) {
      int start = (tokens.length > NUMBER_OF_LEVELS) ? tokens.length - NUMBER_OF_LEVELS : 0;
      return calculateProbability(tokens, start, tokens.length);
    }

    /**
     * Get the probability through the maximum likelihood algorithm
     *
     * @param tokens A text
     * @param start  Starting index of the ngram to evaluate
     * @param end    End index of the ngram to evaluate
     * @return The probability
     */
    private double calculateProbability(String[] tokens, int start, int end) {
      //if ngram longer than what we have, backoff to the longest possible
      //todo: should we rather return 0 (or throw an error?)
      if (end - start > NUMBER_OF_LEVELS) {
        return calculateProbability(tokens, start + 1, end);
      }
      double c = ngramDictionary.get(tokens, start, end);
      if (c == 0) {
        return c;
      }
      if (tokens.length > 1) {
        return c / ngramDictionary.get(tokens, start, end - 1);
      } else {
        return c / wordCount;
      }
    }

    /**
     * Update the cached data
     */
    public void update() {
      wordCount = ngramDictionary.getCorpusSize();
    }

  }

  private final NgramDictionary ngramDictionary;
  private final int NUMBER_OF_LEVELS;
  private final probabilityEstimator estimator;

  public NgramEstimator(String algorithm, NgramDictionary ngramDictionary, int ngramDepth) {
    this.ngramDictionary = ngramDictionary;
    NUMBER_OF_LEVELS = ngramDepth;
    this.estimator = getEstimator(algorithm);

  }

  /**
   * Get the probability of an n-gram
   *
   * @param ngram the ngram
   * @return the probability
   */
  public double calculateProbability(String... ngram) {
    return estimator.calculateProbability(ngram);
  }

  /**
   * Instantiate the estimator object
   *
   * @param algorithm       The algorithm to be used
   * @return An n-gram estimator
   */
  private probabilityEstimator getEstimator(String algorithm) {
    switch (algorithm.charAt(0)) {
      //chen-goodman
      case 'c':
      case 'C':
        return new chenGoodmanEstimator();

      //maximum-likelihood
      default:
        return new maximumLikelihoodEstimator();
    }

  }

  public void update() {
    estimator.update();
  }

  private interface probabilityEstimator {

    double calculateProbability(String... tokens);

    void update();

  }

}
