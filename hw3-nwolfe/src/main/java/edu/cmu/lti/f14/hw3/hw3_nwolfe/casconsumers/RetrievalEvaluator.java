package edu.cmu.lti.f14.hw3.hw3_nwolfe.casconsumers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_nwolfe.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Answer;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.BetterMap;
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
  private ArrayList<Answer> outputBuffer;

  private final String outfile = "report.txt";

  @Override
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    qMap = new BetterMap<Integer, Query>();

    ansMap = new BetterMap<Integer, Answer>();

    outputBuffer = new ArrayList<Answer>();
  }

  /**
   * Loops through the Answer objects in the outputBuffer and writes the results to a file. Ends by
   * computing the MRR and outputs that as the last line of the file.
   */
  private void printReport() {
    PrintStream ps;
    try {
      ps = new PrintStream(new File(outfile));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new UIMA_IllegalStateException();
    }
    for (Answer a : outputBuffer) {
      ps.println(a.getReport());
    }
    // TODO :: compute the metric:: mean reciprocal rank
    Double metric_mrr = compute_mrr();
    ps.println(" (MRR) Mean Reciprocal Rank :: " + metric_mrr);
    ps.close();
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

    FSIterator<Annotation> it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      HashMap<String, Integer> tokenFrequencies = new HashMap<String, Integer>(tokenList.size());
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
    for (Integer qid : qMap.keySet()) {
      Query query = qMap.get(qid).get(0);
      for (Answer ans : ansMap.get(qid)) {
        Double cosSim = computeCosineSimilarity(query.getDocTokenFrequencies(),
                ans.getDocTokenFrequencies());
        ans.setCosineSimilarity(cosSim);
      }
      // TODO :: compute the rank of retrieved sentences
      ArrayList<Answer> alist = ansMap.get(qid);
      Collections.sort(alist);
      ansMap.put(qid, alist);

      // ouput highest ranked sentences
      addReportResults(alist, qid);
    }
    printReport();
  }

  /**
   * Loops through the Answer array, adds the ones which are ranked highest and relevant to the
   * outputBuffer
   * 
   * @param alist
   * @param qid
   */
  private void addReportResults(ArrayList<Answer> alist, Integer qid) {
    for (int i = 0; i < alist.size(); i++) {
      Answer a = alist.get(i);
      a.setRank(i + 1);
      if (a.getRank() == 1) {
        outputBuffer.add(a);
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
   * Calculate MRR from the Answers outputBuffer
   * 
   * @return mrr
   */
  private Double compute_mrr() {
    Double metric_mrr = 0.0;
    for (Answer a : outputBuffer) {
      metric_mrr += 1 / (a.getRank().floatValue());
    }
    metric_mrr = metric_mrr / outputBuffer.size();
    return metric_mrr;
  }

}
