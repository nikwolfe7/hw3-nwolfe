package edu.cmu.lti.f14.hw3.hw3_nwolfe.utils;

public class SimilarityFactory {

  private static String dj = "dice-jaccard";

  private static String dice = "dice";

  private static String jaccard = "jaccard";

  private static String cos = "cosine";

  private static String current = cos;

  public static Similarity getNewSimilarity() {
    if (current.equals(dj))
      return new DiceJaccardSimilarity();
    else if (current.equals(dice))
      return new DiceSimilarityStrategy();
    else if (current.equals(jaccard))
      return new JaccardSimilarityStrategy();
    else if (current.equals(cos))
      return new CosineSimilarityStrategy();
    else
      return new CosineSimilarityStrategy();
  }
}
