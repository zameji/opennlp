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


import java.util.Map;

import opennlp.tools.util.BaseToolFactory;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ext.ExtensionLoader;

/**
 * The factory that provides {@link SmoothedNgramLanguageModel} default implementations and
 * resources. Users can extend this class if their application requires
 * overriding
 */
public class NgramLMFactory extends BaseToolFactory {

  private String languageCode;
  private String smoothing = "Chen-Goodman";
  private static final String COMPRESSION = "compression";
  private Integer ngramSize;

  private static final String NGRAM_SIZE = "ngramSize";
  private static final String SMOOTHING = "smoothing";
  private Boolean compression = false;

  /**
   * Creates a {@link opennlp.tools.languagemodel.NgramLMFactory} that provides the default implementation
   * of the resources.
   */
  public NgramLMFactory() {
  }

  public NgramLMFactory(String languageCode,
                        int ngramDepth, String smoothing, Boolean compression) {
    this.init(languageCode, ngramDepth, smoothing, compression);
  }

  public NgramLMFactory(String languageCode,
                        int ngramDepth, String smoothing) {
    this.init(languageCode, ngramDepth, smoothing, false);
  }

  public static opennlp.tools.languagemodel.NgramLMFactory create(String subclassName,
                                                                  String languageCode,
                                                                  Integer ngramSize,
                                                                  String smoothing,
                                                                  Boolean compression)
      throws InvalidFormatException {
    if (subclassName == null) {
      // will create the default factory
      return new opennlp.tools.languagemodel.NgramLMFactory(languageCode, ngramSize,
          smoothing);
    }
    try {
      opennlp.tools.languagemodel.NgramLMFactory theFactory = ExtensionLoader.instantiateExtension(
          opennlp.tools.languagemodel.NgramLMFactory.class, subclassName);
      theFactory.init(languageCode, ngramSize,
          smoothing, compression);
      return theFactory;
    } catch (Exception e) {
      String msg = "Could not instantiate the " + subclassName
          + ". The initialization throw an exception.";
      System.err.println(msg);
      e.printStackTrace();
      throw new InvalidFormatException(msg, e);
    }
  }

  @Override
  public void validateArtifactMap() throws InvalidFormatException {
    if (this.artifactProvider.getManifestProperty(NGRAM_SIZE) == null) {
      throw new InvalidFormatException(NGRAM_SIZE
          + " is a mandatory property!");
    }

  }

  protected void init(String languageCode, Integer ngramDepth, String smoothing,
                      Boolean compression) {
    this.languageCode = languageCode;
    this.ngramSize = ngramDepth;
    if (smoothing != null) {
      //todo: check that the smoothing is a valid one
      this.smoothing = smoothing;
    }
    if (compression != null) {
      this.compression = compression;
    }
  }

  @Override
  public Map<String, String> createManifestEntries() {
    Map<String, String> manifestEntries = super.createManifestEntries();

    manifestEntries.put(NGRAM_SIZE,
        Integer.toString(getNgramSize()));

    manifestEntries.put(SMOOTHING,
        getSmoothing());

    manifestEntries.put(COMPRESSION,
        Boolean.toString(getCompression()));

    return manifestEntries;
  }

  public Boolean getCompression() {
    return compression;
  }

  /**
   * Gets whether to use alphanumeric optimization.
   *
   * @return true if the alpha numeric optimization is enabled, otherwise false
   */
  public Integer getNgramSize() {
    if (artifactProvider != null) {
      this.ngramSize = Integer.valueOf(this.artifactProvider
          .getManifestProperty(NGRAM_SIZE));
    }
    return this.ngramSize;
  }

  /**
   * Gets the abbreviation dictionary
   *
   * @return null or the abbreviation dictionary
   */
  public String getSmoothing() {
    if (this.smoothing == null && artifactProvider != null) {
      this.smoothing = this.artifactProvider.getArtifact(SMOOTHING);
    }
    return this.smoothing;
  }

  /**
   * Retrieves the language code.
   *
   * @return the language code
   */
  public String getLanguageCode() {
    if (this.languageCode == null && this.artifactProvider != null) {
      this.languageCode = this.artifactProvider.getLanguage();
    }
    return this.languageCode;
  }

}
