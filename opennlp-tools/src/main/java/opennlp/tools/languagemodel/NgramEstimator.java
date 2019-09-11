package opennlp.tools.languagemodel;

import opennlp.tools.ngram.NgramDictionary;

public class NgramEstimator {

  private final NgramDictionary ngramDictionary;

  private class chenGoodmanEstimator implements probabilityEstimator {

    private final int wordCount;
    private final int NUMBER_OF_LEVELS;
    private final NgramDictionary ngramDictionary;
    private double[][] D;
    private double[] c_sum;

    public chenGoodmanEstimator(NgramDictionary ngramDictionary, Integer ngramDepth) {
      this.ngramDictionary = ngramDictionary;
      NUMBER_OF_LEVELS = ngramDepth;

      // Get the discounting parameter D (Eq. 17 of Chen & Goodman 1999)
      double[][] n = new double[NUMBER_OF_LEVELS][4]; //needed to get the estimates of D
      for (int depth = 0; depth < NUMBER_OF_LEVELS; depth++) {
        for (int freq = 0; freq < 4; freq++) {
          n[depth][freq] = ngramDictionary.getNGramCount(depth + 1, freq + 1, freq + 1);
        }
      }

      double[] Y = new double[NUMBER_OF_LEVELS];
      for (int depth = 0; depth < NUMBER_OF_LEVELS; depth++) {
        Y[depth] = n[depth][0] / (n[depth][0] + 2 * n[depth][1] + EPSILON);
      }

//      System.out.println("n");
//      for (int i=0; i<NUMBER_OF_LEVELS;i++){
//        System.out.println(Arrays.toString(n[i]));
//      }
//
//      System.out.println("Y");
//      System.out.println(Arrays.toString(Y));


      D = new double[NUMBER_OF_LEVELS][4];
      for (int depth = 0; depth < NUMBER_OF_LEVELS; depth++) {
        D[depth][0] = 0.0;
        for (int freq = 1; freq <= 3; freq++) {
          double currentD =
              (freq) - (((freq + 1) * Y[depth]) * (n[depth][freq - 1] / n[depth][freq] + EPSILON));
          D[depth][freq] = currentD;
        }
      }

      // counts of partial gram variability: for n-gram Wi-n+1...Wi,
      // this is the sum of counts of all distinct Wi-n+1...Wi-1 for all Wi's
      // i.e. the count of distinct Wi-n+1...Wi
      c_sum = new double[NUMBER_OF_LEVELS];
      for (int depth = 0; depth < NUMBER_OF_LEVELS; depth++) {
        double c = ngramDictionary.getNGramCountSum(depth + 1);
        c_sum[depth] = c;
      }

      wordCount = ngramDictionary.getCorpusSize();

//      System.out.println("D");
//      for (int i=0; i<NUMBER_OF_LEVELS;i++){
//        System.out.println(Arrays.toString(D[i]));
//      }
//
//      System.out.println("_________\nC sum");
//      System.out.println(Arrays.toString(c_sum));


    }

    @Override
    public double calculate_probability(String... tokens) {
      return calculate_probability(tokens, 0, tokens.length);
    }

    private double calculate_probability(String[] tokens, int start, int end) {

      double c = ngramDictionary.get(tokens, start, end);
      double discount;
      if (c < 3) {
        discount = D[end - start - 1][(int) c];
      } else {
        discount = D[end - start - 1][3];
      }
      double prob = (c - discount) / c_sum[end - start - 1];
      if (end - start == 1) {
        return prob;
      } else {
        double gamma = 0;
        for (int freq = 1; freq <= 3; freq++) {
          int maxfreq = (freq == 3) ? Integer.MAX_VALUE : freq;
          double siblingCount = ngramDictionary
              .getSiblingCount(tokens, start, end, freq, maxfreq);
          gamma += (D[end - start - 1][freq] * siblingCount) / c_sum[end - start - 1];
        }
        return prob + gamma * calculate_probability(tokens, start + 1, end);
      }
    }


  }

  private class maximumLikelihoodEstimator implements probabilityEstimator {

    private final int wordCount;
    private final int NUMBER_OF_LEVELS;
    private final NgramDictionary ngramDictionary;

    public maximumLikelihoodEstimator(NgramDictionary ngramDictionary, Integer ngramDepth) {
      this.ngramDictionary = ngramDictionary;
      NUMBER_OF_LEVELS = ngramDepth;
      wordCount = ngramDictionary.getCorpusSize();
    }

    @Override
    public double calculate_probability(String... tokens) {
      double c = ngramDictionary.get(tokens, 0, tokens.length);
      if (c == 0) {
        return c;
      }
      if (tokens.length > 1) {
        return c / ngramDictionary.get(tokens, 0, tokens.length - 1);
      } else {
        return c / wordCount;
      }
    }

  }

  private final probabilityEstimator estimator;
  private final double EPSILON = 0.00000001;

  public NgramEstimator(String algorithm, NgramDictionary ngramDictionary, int ngramDepth) {
    this.estimator = getEstimator(algorithm, ngramDictionary, ngramDepth);
    this.ngramDictionary = ngramDictionary;

  }

  public double calculateProbability(String... ngram) {
    return estimator.calculate_probability(ngram);
  }

  private probabilityEstimator getEstimator(String algorithm, NgramDictionary ngramDictionary, int ngramDepth) {
    switch (algorithm.charAt(0)) {
      //chen-goodman
      case 'c':
      case 'C':
        return new chenGoodmanEstimator(ngramDictionary, ngramDepth);

      //maximum-likelihood
      default:
        return new maximumLikelihoodEstimator(ngramDictionary, ngramDepth);
    }

  }


  private interface probabilityEstimator {

    double calculate_probability(String... tokens);

  }

}
