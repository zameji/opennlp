package opennlp.tools.languagemodel;

import opennlp.tools.ngram.NgramDictionary;

public class NgramEstimator {

  private final String algorithm;
  private final NgramDictionary ngramDictionary;
  private final int vocabularySize;

  public NgramEstimator(String algorithm, NgramDictionary ngramDictionary) {
    this.algorithm = algorithm;
    this.ngramDictionary = ngramDictionary;
    vocabularySize = ngramDictionary.getCorpusSize();
  }


  public double calculateProbability(String... ngram) {

    switch (algorithm) {
      case "ch":
        return maximumLikelihood(ngram);
      default:
        return maximumLikelihood(ngram);
    }
  }

  private double maximumLikelihood(String... tokens) {

    double c = ngramDictionary.get(tokens, 0, tokens.length);
    if (c == 0) {
      return c;
    }
    if (tokens.length > 1) {
      return c / ngramDictionary.get(tokens, 0, tokens.length - 1);
    } else {
      return c / vocabularySize;
    }
  }

}
