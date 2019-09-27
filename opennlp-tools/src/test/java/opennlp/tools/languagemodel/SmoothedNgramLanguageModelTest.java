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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.javamex.classmexer.MemoryUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import opennlp.tools.formats.masc.MascDocumentStream;
import opennlp.tools.formats.masc.MascNamedEntitySampleStream;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;

import static org.junit.Assert.fail;

public class SmoothedNgramLanguageModelTest {

  class TokenStream extends FilterObjectStream<NameSample, String[]> {
    public TokenStream(ObjectStream<NameSample> samples) {
      super(samples);
    }

    public String[] read() {
      try {
        NameSample next = samples.read();
        if (next == null) {
          return null;
        }
        return next.getSentence();

      } catch (IOException e) {
        return null;
      }
    }

    public void reset() {
      try {
        samples.reset();
      } catch (Exception e) {
        System.err.println("Failed");
      }
    }
  }

  final static int DEPTH = 5;
  static ObjectStream<NameSample> trainTokens;
  static int REPS = 1;
  static int SAMPLE_LIMIT = 10;
  static boolean DEBUG = true;
  static List<String[]> gramList;
  static List<List> testLists;
  static long gramCount;
  static int wordCount = 0;
  static String[] SMOOTHING = {"maximum likelihood", "chen"};

  @BeforeClass
  public static void doBeforeClass() {

    System.out.println("Running @BeforeClass");
    try {
      File directory = new File("C:/projects/OpenNLP/MASC/test");
//      File directory = new File(this.getClass().getResource(
//          "/opennlp/tools/formats/masc/").getFile());
      FileFilter fileFilter = pathname -> pathname.getName().contains("");
      trainTokens = new MascNamedEntitySampleStream(
          new MascDocumentStream(directory,
              true, fileFilter));

      trainTokens.reset();
      gramList = new ArrayList<>();
      NameSample next = trainTokens.read();
      next = trainTokens.read();
      while (next != null) {
        String[] nextStrings = next.getSentence();
        wordCount += nextStrings.length;
        for (int n = DEPTH; n > 0; n--) {
          for (int i = 0; i + n <= nextStrings.length; i++) {
            String[] gram = new String[n];
            System.arraycopy(nextStrings, i, gram, 0, n);
            gramList.add(gram);
          }
        }

        next = trainTokens.read();
      }

      testLists = new ArrayList<>();
      for (int n = 0; n <= DEPTH; n++) {
        List<String[]> l = new ArrayList<>();
        testLists.add(l);
      }

      for (String[] gram : gramList) {
        if (gram.length <= DEPTH) {
          testLists.get(gram.length - 1).add(gram);
        }
      }

      gramCount = 0;
      for (int gram = 1; gram <= DEPTH; gram++) {
        gramCount += testLists.get(gram - 1).size();
      }

    } catch (IOException e) {
      fail("Could not preprocess the corpus");
    }
  }

  @Test
  public void baseline_predict_next() {

    try {
      trainTokens.reset();

      NameSample next = trainTokens.read();
      NGramLanguageModel model = new NGramLanguageModel();

      long start = System.nanoTime();
      while (next != null) {
        String[] nextStrings = next.getSentence();

        for (int n = DEPTH; n > 0; n--) {
          for (int i = 0; i + n <= nextStrings.length; i++) {
            String[] gram = new String[n];
            System.arraycopy(nextStrings, i, gram, 0, n);
            model.add(gram);
          }
        }
        next = trainTokens.read();
      }
      long end = System.nanoTime();
      System.out.println(wordCount + " words read in " + (end - start) / 1000000000.0 + "seconds" +
          ".\nSpeed: " + wordCount / ((end - start) / 1000000000.0) + " w/s");

      for (int gram = 2; gram < DEPTH; gram++) {
        int correct = 0;
        int processed = 0;
        String[] blackHole = new String[1];
        start = System.nanoTime();
        for (int j = 0; j < REPS; j++) {
          int limit;
          if (DEBUG) {
            limit = SAMPLE_LIMIT;
          } else {
            limit = testLists.get(gram - 1).size();
          }
          for (int i = 0; i < limit; i++) {
            String[] test = (String[]) testLists.get(gram - 1).get(i);
            blackHole = model.predictNextTokens(Arrays.copyOf(test, test.length - 1));
            if (blackHole[blackHole.length - 1].equals(test[test.length - 1])) {
              correct++;
            }
            processed++;
          }
        }
        end = System.nanoTime();
        System.out.println(gram + "-gram performace: " + (end - start) /
            (1000.0 * REPS * testLists.get(gram - 1).size()) +
            "us" + " per " + gram + "-gram;\n Accuracy: " + (((double) correct) / processed) +
            "\n sample prediction" + Arrays.toString(blackHole));
      }

      System.out.println("I am very " + model.predictNextTokens("I", "am", "very")[0]);

    } catch (IOException e) {
      fail("Failed baseline");
    }
  }

  @Test
  public void baseline_calculate_probability() {

    try {
      trainTokens.reset();

      NameSample next = trainTokens.read();
      NGramLanguageModel model = new NGramLanguageModel();

      long start = System.nanoTime();
      while (next != null) {
        String[] nextStrings = next.getSentence();

        for (int n = DEPTH; n > 0; n--) {
          for (int i = 0; i + n <= nextStrings.length; i++) {
            String[] gram = new String[n];
            System.arraycopy(nextStrings, i, gram, 0, n);
            model.add(gram);
          }
        }
        next = trainTokens.read();
      }
      long end = System.nanoTime();
      System.out.println(wordCount + " words read in " + (end - start) / 1000000000.0 + "seconds" +
          ".\nSpeed: " + wordCount / ((end - start) / 1000000000.0) + " w/s");

      for (int gram = 1; gram <= DEPTH; gram++) {
        double blackHole = 0;
        start = System.nanoTime();
        for (int j = 0; j < REPS; j++) {
          int limit;
          if (DEBUG) {
            limit = SAMPLE_LIMIT;
          } else {
            limit = testLists.get(gram - 1).size();
          }
          for (int i = 0; i < limit; i++) {
            blackHole = model.calculateProbability((String[]) testLists.get(gram - 1).get(i));
          }
        }
        end = System.nanoTime();
        System.out.println(gram + "-gram performace: " + (end - start) /
            (1000.0 * REPS * testLists.get(gram - 1).size()) +
            "us" + " per " + gram + "-gram;\n sample probability" + blackHole);
      }

      long es = MemoryUtil.deepMemoryUsageOf(model, MemoryUtil.VisibilityFilter.ALL);
      System.out.println("Estimated space: " + es + ", i.e. " + es / gramCount + "bytes/gram");

    } catch (IOException e) {
      fail("Failed baseline");
    }
  }

  @Test
  public void compressed_probability() {
    for (String smoothing : SMOOTHING) {
      try {

        trainTokens.reset();

        SmoothedNgramLanguageModel model = null;
        long start = System.nanoTime();
        model = SmoothedNgramLanguageModel.train(new TokenStream(trainTokens), new NgramLMFactory(
            "en", DEPTH,
            smoothing, true, 1));
        long end = System.nanoTime();
        trainTokens.reset();

        System.out.println(wordCount + " words read in " + (end - start) / 1000000000.0 +
            "seconds" +
            ".\n" +
            "Speed: " + wordCount / ((end - start) / 1000000000.0) + " w/s");

        for (int gram = 1; gram <= DEPTH; gram++) {

          start = System.nanoTime();
          double blackHole = 0;
          for (int j = 0; j < REPS; j++) {
            int limit;
            if (DEBUG) {
              limit = SAMPLE_LIMIT;
            } else {
              limit = testLists.get(gram - 1).size();
            }
            for (int i = 0; i < limit; i++) {
              blackHole = model.calculateProbability((String[]) testLists.get(gram - 1).get(i));
            }
          }
          end = System.nanoTime();
          System.out.println(gram + "-gram performace: " + (end - start) /
              (1000.0 * REPS * testLists.get(gram - 1).size()) +
              "us" + " per " + gram + "-gram;\n sample probability" + blackHole);

        }

        long es = MemoryUtil.deepMemoryUsageOf(model, MemoryUtil.VisibilityFilter.ALL);
        System.out.println("Estimated space: " + es + ", i.e. " + es / gramCount + "bytes/gram");

      } catch (IOException e) {
        fail("Failed compressed");
      }
    }
  }

  @Test
  public void compressed_next_tokens() {
    for (String smoothing : SMOOTHING) {
      try {

        trainTokens.reset();

        SmoothedNgramLanguageModel model = null;
        long start = System.nanoTime();
        model = SmoothedNgramLanguageModel.train(new TokenStream(trainTokens), new NgramLMFactory(
            "en", DEPTH,
            smoothing, true, 1));
        long end = System.nanoTime();
        trainTokens.reset();

        for (int gram = 2; gram < DEPTH; gram++) {
          int correct = 0;
          int processed = 0;
          String[] blackHole = new String[1];
          start = System.nanoTime();
          for (int j = 0; j < REPS; j++) {
            int limit;
            if (DEBUG) {
              limit = SAMPLE_LIMIT;
            } else {
              limit = testLists.get(gram - 1).size();
            }
            for (int i = 0; i < limit; i++) {
              String[] test = (String[]) testLists.get(gram - 1).get(i);
              blackHole = model.predictNextTokens(Arrays.copyOf(test, test.length - 1));
              if (blackHole[blackHole.length - 1].equals(test[test.length - 1])) {
                correct++;
              }
              processed++;
            }
          }
          end = System.nanoTime();
          System.out.println(gram + "-gram performace: " + (end - start) /
              (1000.0 * REPS * testLists.get(gram - 1).size()) +
              "us" + " per " + gram + "-gram;\n Accuracy: " + (((double) correct) / processed) +
              "\n sample prediction" + Arrays.toString(blackHole));
        }

        System.out.println("I am very " + model.predictNextTokens("I", "am", "very")[0]);

      } catch (IOException e) {
        fail("Failed compressed");
      }
    }
  }

  @Test
  public void raw_probability() {
    for (String smoothing : SMOOTHING) {
      try {

        trainTokens.reset();
//      long wait = System.currentTimeMillis();
//      while (System.currentTimeMillis() - wait < 15000){
//        int s = 0;
//      }
        long start = System.nanoTime();
        LanguageModel model = SmoothedNgramLanguageModel.train(new TokenStream(trainTokens),
            new NgramLMFactory(
                "en", DEPTH,
                smoothing, false, 1));
        long end = System.nanoTime();

        System.out.println(wordCount + " words read in " + (end - start) / 1000000000.0 + "seconds.\n" +
            "Speed: " + wordCount / ((end - start) / 1000000000.0) + " wps");

        for (int gram = 1; gram <= DEPTH; gram++) {

          start = System.nanoTime();
          double blackHole = 0;
          for (int j = 0; j < REPS; j++) {
            int limit;
            if (DEBUG) {
              limit = SAMPLE_LIMIT;
            } else {
              limit = testLists.get(gram - 1).size();
            }
            for (int i = 0; i < limit; i++) {
              blackHole = model.calculateProbability((String[]) testLists.get(gram - 1).get(i));
            }
          }
          end = System.nanoTime();
          System.out.println(gram + "-gram performace: " + (end - start) /
              (1000.0 * REPS * testLists.get(gram - 1).size()) +
              "us" + " per " + gram + "-gram;\n sample probability" + blackHole);
        }

        long es = MemoryUtil.deepMemoryUsageOf(model, MemoryUtil.VisibilityFilter.ALL);
        System.out.println("Estimated space: " + es + ", i.e. " + es / gramCount + "bytes/gram");

      } catch (Exception e) {
        System.out.println(e.getMessage());
        System.out.println(Arrays.toString(e.getStackTrace()));
        fail("Failed raw");

      }
    }
  }

  @Test
  public void raw_next_tokens() {
    for (String smoothing : SMOOTHING) {
      try {

        trainTokens.reset();
//      long wait = System.currentTimeMillis();
//      while (System.currentTimeMillis() - wait < 15000){
//        int s = 0;
//      }
        long start = System.nanoTime();
        LanguageModel model = SmoothedNgramLanguageModel.train(new TokenStream(trainTokens),
            new NgramLMFactory(
                "en", DEPTH,
                smoothing, false, 1));
        long end = System.nanoTime();

        System.out.println(wordCount + " words read in " + (end - start) / 1000000000.0 + "seconds.\n" +
            "Speed: " + wordCount / ((end - start) / 1000000000.0) + " wps");

        for (int gram = 2; gram < DEPTH; gram++) {
          int correct = 0;
          int processed = 0;
          String[] blackHole = new String[1];
          start = System.nanoTime();
          for (int j = 0; j < REPS; j++) {
            int limit;
            if (DEBUG) {
              limit = SAMPLE_LIMIT;
            } else {
              limit = testLists.get(gram - 1).size();
            }
            for (int i = 0; i < limit; i++) {
              String[] test = (String[]) testLists.get(gram - 1).get(i);
              blackHole = model.predictNextTokens(Arrays.copyOf(test, test.length - 1));
              if (blackHole[blackHole.length - 1].equals(test[test.length - 1])) {
                correct++;
              }
              processed++;
            }
          }
          end = System.nanoTime();
          System.out.println(gram + "-gram performace: " + (end - start) /
              (1000.0 * REPS * testLists.get(gram - 1).size()) +
              "us" + " per " + gram + "-gram;\n Accuracy: " + (((double) correct) / processed) +
              "\n sample prediction" + Arrays.toString(blackHole));
        }

        System.out.println("I am very " + model.predictNextTokens("I", "am", "very")[0]);

      } catch (Exception e) {
        System.out.println(e.getMessage());
        System.out.println(Arrays.toString(e.getStackTrace()));
        fail("Failed raw");
      }
    }
  }

  @Test
  public void raw_compressed_equality() {
    for (String smoothing : SMOOTHING) {
      try {

        trainTokens.reset();
        SmoothedNgramLanguageModel model = null;
        long start = System.nanoTime();
        LanguageModel modelCompressed =
            SmoothedNgramLanguageModel.train(new TokenStream(trainTokens), new NgramLMFactory(
                "en", DEPTH,
                smoothing, true, 1));

        trainTokens.reset();

        LanguageModel modelRaw = SmoothedNgramLanguageModel.train(new TokenStream(trainTokens),
            new NgramLMFactory(
                "en", DEPTH,
                smoothing, false, 1));
        long end = System.nanoTime();

        for (int gram = 1; gram <= DEPTH; gram++) {

          start = System.nanoTime();
          double raw = 0;
          double compressed = 0;
          int match = 0;
          int mismatch = 0;

          for (int i = 0; i < testLists.get(gram - 1).size(); i++) {
            raw = modelRaw.calculateProbability((String[]) testLists.get(gram - 1).get(i));
            compressed = modelCompressed.calculateProbability((String[]) testLists.get(gram - 1).get(i));

            if (raw == compressed) {
              match++;
            } else {
              mismatch++;
            }
          }

          end = System.nanoTime();
          System.out.println(gram + "-gram performace: " + match + "/" + (match + mismatch) + ", i.e." +
              (double) match / ((double) match + mismatch) + "%");
        }

      } catch (IOException e) {
        fail("Failed compressed");
      }
    }
  }


}
