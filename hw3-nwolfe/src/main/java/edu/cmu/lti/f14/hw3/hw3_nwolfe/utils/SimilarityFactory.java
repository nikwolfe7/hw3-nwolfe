package edu.cmu.lti.f14.hw3.hw3_nwolfe.utils;

public class SimilarityFactory {
  
  private static String weighted = "weighted";
  
  private static String avg = "average";
  
  private static String vote = "vote";

  private static String dj = "dice-jaccard";

  private static String dice = "dice";

  private static String jaccard = "jaccard";

  private static String cos = "cosine";

  private static String current = cos;

  public static Similarity getNewSimilarity() {
    if (current.equals(dj))
      return new DiceJaccardStrategy();
    else if (current.equals(dice))
      return new DiceSimilarityStrategy();
    else if (current.equals(jaccard))
      return new JaccardSimilarityStrategy();
    else if (current.equals(cos))
      return new CosineSimilarityStrategy();
    else if (current.equals(vote))
      return new VoteStrategy();
    else if (current.equals(avg))
      return new AverageStrategy();
    else if (current.equals(weighted))
      return new WeightedSumStrategy();
    else
      return new CosineSimilarityStrategy();
  }
}
