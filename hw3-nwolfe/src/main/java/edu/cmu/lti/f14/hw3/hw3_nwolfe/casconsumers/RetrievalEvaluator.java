package edu.cmu.lti.f14.hw3.hw3_nwolfe.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
  
  /** Output string buffer **/
  private ArrayList<String> outputBuffer;

  @Override
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    qMap = new BetterMap<Integer, Query>();

    ansMap = new BetterMap<Integer, Answer>();
    
    outputBuffer = new ArrayList<String>();
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
    for(Integer qid : qMap.keySet()) {
      Query query = qMap.get(qid).get(0);
      for(Answer ans : ansMap.get(qid)) {
        Double cosSim = computeCosineSimilarity(query.getDocTokenFrequencies(), ans.getDocTokenFrequencies());
        ans.setCosineSimilarity(cosSim);
      }
      // TODO :: compute the rank of retrieved sentences
      ArrayList<Answer> alist = ansMap.get(qid);
      Collections.sort(alist);
      ansMap.put(qid, alist);
      
      // ouput highest ranked sentences
      addReportResults(alist,qid);
    }
    // TODO :: compute the metric:: mean reciprocal rank
    Double metric_mrr = compute_mrr();
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
  }

  private void addReportResults(ArrayList<Answer> alist, Integer qid) {
    for(int i = 0; i < alist.size(); i++) {
      Answer a = alist.get(i);
      if(a.getRelevance() == 1) {
        String report = String.format("cosine=%.4f\trank=%d\tqid=%d\trel=1\t%s", a.getCosineSimilarity(), i+1, qid, a.getDocText());
        outputBuffer.add(report);
        break;
      }
    }
  }

  /**
   * @return cosine_similarity
   */
  private Double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    Double cosine_similarity = 0.0;

    Iterable<Integer> A = queryVector.values();
    Iterable<Integer> B = docVector.values();
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
  private Double calcEuclideanNorm(Iterable<Integer> V) {
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
  private Double compute_mrr() {
    Double metric_mrr = 0.0;
    
    

    // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection

    return metric_mrr;
  }

}
