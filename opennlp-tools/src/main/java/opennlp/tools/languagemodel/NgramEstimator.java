package opennlp.tools.languagemodel;

import java.util.HashMap;
import java.util.Map;

import opennlp.tools.ngram.NgramDictionary;

public class NgramEstimator {

  private final String algorithm;
  private final NgramDictionary ngramDictionary;
  private final Map<String, Integer> constants;

  public NgramEstimator(String algorithm, NgramDictionary ngramDictionary) {
    this.algorithm = algorithm;
    this.ngramDictionary = ngramDictionary;
    constants = new HashMap<>();
    initialize();
  }

  /**
   * Initialize the constants that a given algorithm needs
   */
  private void initialize() {

    constants.put("corpus_size", ngramDictionary.getCorpusSize());

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

    int vocabularySize = constants.get("corpus_size");

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
