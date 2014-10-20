package edu.cmu.lti.f14.hw3.hw3_nwolfe.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_nwolfe.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Answer;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Query;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Utils;

public class RetrievalEvaluator<V> extends CasConsumer_ImplBase {

  /** query id number **/
  private ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  private ArrayList<Integer> relList;

  /** Map of queries **/
  private BetterMap<Integer, Query> qMap;

  /** Map of answers **/
  private BetterMap<Integer, Answer> ansMap;

  @Override
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    qMap = new BetterMap<Integer, Query>();

    ansMap = new BetterMap<Integer, Answer>();
  }

  @SuppressWarnings("serial")
  private class BetterMap<K, E> extends HashMap<K, ArrayList<E>> {
    public void addItem(K k, E q) {
      if (super.containsKey(k)) {
        super.get(k).add(q);
      } else {
        ArrayList<E> arr = new ArrayList<E>();
        arr.add(q);
        super.put(k, arr);
      }
    }
  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    @SuppressWarnings("rawtypes")
    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      HashMap<String, Integer> tokenFrequencies = new HashMap<String, Integer>();
      for (Token t : tokenList) {
        String key = t.getText();
        Integer value = t.getFrequency();
        tokenFrequencies.put(key, value);
      }
      // query id
      Integer queryId = doc.getQueryID();
      Integer relevance = doc.getRelevanceValue();
      if (relevance == 99) {
        Query q = new Query(queryId, doc.getText(), tokenFrequencies);
        qMap.addItem(queryId, q);
      } else {
        Answer a = new Answer(queryId, relevance, doc.getText(), tokenFrequencies);
        ansMap.addItem(queryId, a);
      }
      qIdList.add(queryId);
      relList.add(relevance);
    }
  }

  /**
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {
    super.collectionProcessComplete(arg0);

    // TODO :: compute the cosine similarity measure

    // TODO :: compute the rank of retrieved sentences

    // TODO :: compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr();
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
  }

  /**
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    Double cosine_similarity = 0.0;

    Vector<Integer> A = (Vector<Integer>) queryVector.values();
    Vector<Integer> B = (Vector<Integer>) docVector.values();
    Double normA = calcEuclideanNorm(A);
    Double normB = calcEuclideanNorm(B);
    Double normAB = normA * normB;

    // Scalar product of A / B
    for (String s : queryVector.keySet()) {
      if (docVector.containsKey(s)) {
        Integer a = queryVector.get(s);
        Integer b = docVector.get(s);
        Double axb = (double) (a * b);
        cosine_similarity += axb;
      }
    }
    // cosine_sim / normAB
    cosine_similarity = cosine_similarity / normAB;
    return cosine_similarity;
  }

  /**
   * Caclulate Euclidean norm of a vector E = SUM(v^2) for v in V
   * 
   * @param V
   * @return Double, euclidean norm
   */
  private Double calcEuclideanNorm(Vector<Integer> V) {
    Double eucNorm = 0.0;
    for (Integer v : V) {
      v = v * v; // v squared
      eucNorm += v;
    }
    return Math.sqrt(eucNorm);
  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr() {
    double metric_mrr = 0.0;

    // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection

    return metric_mrr;
  }

}
