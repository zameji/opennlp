package opennlp.tools.languagemodel;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.javamex.classmexer.MemoryUtil;
import org.junit.Test;

import opennlp.tools.formats.masc.MascDocumentStream;
import opennlp.tools.formats.masc.MascNamedEntitySampleStream;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;

import static org.junit.Assert.fail;

public class SmoothedNgramLanguageModelTest {

  @Test
  public void train_and_calculate_probability() {

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

    try {
      int reps = 50;

      File directory = new File("C:/projects/OpenNLP/MASC/data");
//      File directory = new File(this.getClass().getResource(
//          "/opennlp/tools/formats/masc/").getFile());
      FileFilter fileFilter = pathname -> pathname.getName().contains("");
      ObjectStream<NameSample> trainTokens = new MascNamedEntitySampleStream(
          new MascDocumentStream(directory,
              true, fileFilter));

      System.out.println("Training");
      System.out.println("_____________");

      System.out.println("BASELINE");
      System.out.println("********");
      NameSample next = trainTokens.read();
      NGramLanguageModel baseline = new NGramLanguageModel();
      int wordsRead = 0;

      long start = System.currentTimeMillis();
      while (next != null) {
        String[] nextStrings = next.getSentence();
        wordsRead += nextStrings.length;
        for (int n = 3; n > 0; n--) {
          for (int i = 0; i + n <= nextStrings.length; i++) {
            String[] gram = new String[n];
            System.arraycopy(nextStrings, i, gram, 0, n);
            baseline.add(gram);
          }
        }
        next = trainTokens.read();
      }
      long end = System.currentTimeMillis();
      System.out.println(wordsRead + " words read in " + (end - start) / 1000.0 + "seconds.\n" +
          "Speed: " + wordsRead / ((end - start) / 1000.0) + " wps");

      trainTokens.reset();
      List<String[]> gramList = new ArrayList<>();
      next = trainTokens.read();
      while (next != null) {
        String[] nextStrings = next.getSentence();
        for (int n = 3; n > 0; n--) {
          for (int i = 0; i + n <= nextStrings.length; i++) {
            String[] gram = new String[n];
            System.arraycopy(nextStrings, i, gram, 0, n);
            gramList.add(gram);
          }
        }
        next = trainTokens.read();
      }

      List<String[]> unigramList = new ArrayList<>();
      List<String[]> bigramList = new ArrayList<>();
      List<String[]> trigramList = new ArrayList<>();
      for (String[] gram : gramList) {
        switch (gram.length) {
          case 1:
            unigramList.add(gram);
            break;
          case 2:
            bigramList.add(gram);
            break;
          case 3:
            trigramList.add(gram);
            break;
        }

      }

      start = System.currentTimeMillis();
      for (int j = 0; j < reps; j++) {
        for (int i = 0; i < unigramList.size(); i++) {
          double x = baseline.calculateProbability(unigramList.get(i));
        }
      }
      end = System.currentTimeMillis();
      System.out.println("Unigram performace: " + (end - start) * 1000.0 / (reps * unigramList.size()) +
          "us" + " per unigram");

      start = System.currentTimeMillis();
      for (int j = 0; j < reps; j++) {
        for (int i = 0; i < bigramList.size(); i++) {
          double x = baseline.calculateProbability(bigramList.get(i));
        }
      }
      end = System.currentTimeMillis();
      System.out.println("Bigram performace: " + (end - start) * 1000.0 / (reps * unigramList.size()) + "us " +
          "per bigram");

      start = System.currentTimeMillis();
      for (int j = 0; j < reps; j++) {
        for (int i = 0; i < trigramList.size(); i++) {
          double x = baseline.calculateProbability(trigramList.get(i));
        }
      }
      end = System.currentTimeMillis();

      System.out.println("Trigram performace: " + (end - start) * 1000.0 / (reps * unigramList.size()) + "us" +
          " " +
          "per trigram");

      long es = MemoryUtil.deepMemoryUsageOf(baseline, MemoryUtil.VisibilityFilter.ALL);
      System.out.println("Estimated space: " + es + ", i.e. " + es / gramList.size() + "bytes/gram");

      System.out.println("___________________________________");
      System.out.println("TRIE-COMPRESSED");
      System.out.println("********");

      trainTokens.reset();

      SmoothedNgramLanguageModel model = null;
      start = System.currentTimeMillis();
      model = SmoothedNgramLanguageModel.train(new TokenStream(trainTokens), new NgramLMFactory(
          "en", 3,
          "none", true));
      end = System.currentTimeMillis();
      trainTokens.reset();

      System.out.println(wordsRead + " words read in " + (end - start) / 1000.0 + "seconds.\n" +
          "Speed: " + wordsRead / ((end - start) / 1000.0) + " wps");


      start = System.currentTimeMillis();
      for (int j = 0; j < reps; j++) {
        for (int i = 0; i < unigramList.size(); i++) {
          double x = model.calculateProbability(unigramList.get(i));
        }
      }
      end = System.currentTimeMillis();

      System.out.println("Unigram performace: " + (end - start) * 1000.0 / (reps * unigramList.size()) +
          "us" + " per unigram");

      start = System.currentTimeMillis();
      for (int j = 0; j < reps; j++) {
        for (int i = 0; i < bigramList.size(); i++) {
          double x = model.calculateProbability(bigramList.get(i));
        }
      }
      end = System.currentTimeMillis();
      System.out.println("Bigram performace: " + (end - start) * 1000.0 / (reps * unigramList.size()) + "us " +
          "per bigram");

      start = System.currentTimeMillis();
      for (int j = 0; j < reps; j++) {
        for (int i = 0; i < trigramList.size(); i++) {
          double x = model.calculateProbability(trigramList.get(i));
        }
      }
      end = System.currentTimeMillis();
      System.out.println("Trigram performace: " + (end - start) * 1000.0 / (reps * unigramList.size()) + "us" +
          " per trigram");

      es = MemoryUtil.deepMemoryUsageOf(model, MemoryUtil.VisibilityFilter.ALL);
      System.out.println("Estimated space: " + es + ", i.e. " + es / gramList.size() + "bytes/gram");

      System.out.println("___________________________________");
      System.out.println("TRIE-RAW");
      System.out.println("********");

      start = System.currentTimeMillis();
      model = SmoothedNgramLanguageModel.train(new TokenStream(trainTokens), new NgramLMFactory(
          "en", 3,
          "none", false));
      end = System.currentTimeMillis();
      trainTokens.reset();

      System.out.println(wordsRead + " words read in " + (end - start) / 1000.0 + "seconds.\n" +
          "Speed: " + wordsRead / ((end - start) / 1000.0) + " wps");


      start = System.currentTimeMillis();
      for (int j = 0; j < reps; j++) {
        for (int i = 0; i < unigramList.size(); i++) {
          double x = model.calculateProbability(unigramList.get(i));
        }
      }
      end = System.currentTimeMillis();

      System.out.println("Unigram performace: " + (end - start) * 1000.0 / (reps * unigramList.size()) +
          "us" + " per unigram");

      start = System.currentTimeMillis();
      for (int j = 0; j < reps; j++) {
        for (int i = 0; i < bigramList.size(); i++) {
          double x = model.calculateProbability(bigramList.get(i));
        }
      }
      end = System.currentTimeMillis();
      System.out.println("Bigram performace: " + (end - start) * 1000.0 / (reps * unigramList.size()) + "us " +
          "per bigram");

      start = System.currentTimeMillis();
      for (int j = 0; j < reps; j++) {
        for (int i = 0; i < trigramList.size(); i++) {
          double x = model.calculateProbability(trigramList.get(i));
        }
      }
      end = System.currentTimeMillis();
      System.out.println("Trigram performace: " + (end - start) * 1000.0 / (reps * unigramList.size()) + "us" +
          " per trigram");

      es = MemoryUtil.deepMemoryUsageOf(model, MemoryUtil.VisibilityFilter.ALL);
      System.out.println("Estimated space: " + es + ", i.e. " + es / gramList.size() + "bytes/gram");

    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.err.println(Arrays.toString(e.getStackTrace()));
      fail("Exception raised");
    }
  }

}
