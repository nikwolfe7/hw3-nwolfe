package edu.cmu.lti.f14.hw3.hw3_nwolfe.utils;

public class FrequencyCounterFactory {
  private static String cleanStem = "cleanStem";

  private static String stemStopWord = "stemStopWord";

  private static String stopWord = "stopWord";

  private static String freq = "freq";

  private static String lemma = "lemma";

  private static String stem = "stem";

  private static String current = freq;

  public static FrequencyCounter getNewFrequencyCounter() {
    if (current.equals(cleanStem))
      return new CleanStemCounter();
    else if (current.equals(stemStopWord))
      return new CleanStemStopWordCounter();
    else if (current.equals(stopWord))
      return new StopWordCounter();
    else if (current.equals(freq))
      return new FrequencyCounter();
    else if (current.equals(lemma))
      return new LemmatizeCounter();
    else if (current.equals(stem))
      return new StemCounter();
    else
      return new FrequencyCounter();
  }
}
