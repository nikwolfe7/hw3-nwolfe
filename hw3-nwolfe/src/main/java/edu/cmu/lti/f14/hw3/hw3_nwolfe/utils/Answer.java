package edu.cmu.lti.f14.hw3.hw3_nwolfe.utils;

import java.util.HashMap;
import java.util.Map;

public class Answer implements Comparable<Integer> {

  private final Integer queryId;

  private final Integer relevance;

  private final String docText;

  private final HashMap<String, Integer> docTokenFrequencies;

  public Answer(Integer queryId, Integer relevance, String docText,
          HashMap<String, Integer> docTokenFreqs) {
    super();
    this.queryId = queryId;
    this.relevance = relevance;
    this.docText = docText;
    this.docTokenFrequencies = docTokenFreqs;
  }

  public Integer getQueryId() {
    return queryId;
  }

  public Integer getRelevance() {
    return relevance;
  }

  public String getDocText() {
    return docText;
  }

  public Map<String, Integer> getDocTokenFrequencies() {
    return docTokenFrequencies;
  }

  @Override
  public int compareTo(Integer o) {
    if (this.relevance > o) {
      return 1;
    } else if (this.relevance < o) {
      return -1;
    } else {
      return 0;
    }
  }

}
