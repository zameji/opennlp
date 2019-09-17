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

  int getCorpusSize();

  //get number of distinct ngrams at level n (optionally with frequency = freq or
  // minfreq <= freq <= maxfreq, i.e. freq in [minfreq, maxfreq)
  int getNGramCount(int n);

  int getNGramCount(int n, int minfreq, int maxfreq);

  int getNGramCountSum(int n);

  int getSiblingCount(String... gram);

  int getSiblingCount(String[] arrayWithGram, int start, int end);

  int getSiblingCount(String[] arrayWithGram, int start, int end, int minfreq, int maxfreq);

  void add(String... gram);

  void add(String[] gram, Integer start, Integer end);

  int get(String... gram);

  int get(String[] gram, int start, int end);

  String[][] getSiblings(String[] gram);

  String[][] getSiblings(String[] arrayWithGram, int start, int length);
}
