package edu.cmu.lti.f14.hw3.hw3_nwolfe.casconsumers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.CosineSimilarityStrategy;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.DiceSimilarityStrategy;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.JaccardSimilarityStrategy;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Question;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Similarity;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.SimilarityFactory;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Utils;

public class RetrievalEvaluator<V> extends CasConsumer_ImplBase {

  /** query id number **/
  private ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  private ArrayList<Integer> relList;

  /** Map of queries **/
  private BetterMap<Integer, Question> qMap;

  /** Map of answers **/
  private BetterMap<Integer, Answer> ansMap;

  /** Output string buffer **/
  private ArrayList<Answer> outputBuffer;

  private final String outfile = "report.txt";

  private final String datafile = "all-data.txt";
  
  private Similarity sim;

  @Override
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    qMap = new BetterMap<Integer, Question>();

    ansMap = new BetterMap<Integer, Answer>();

    outputBuffer = new ArrayList<Answer>();
    
    sim = SimilarityFactory.getNewSimilarity();
  }

  /**
   * Loops through the Answer objects in the outputBuffer and writes the results to a file. Ends by
   * computing the MRR and outputs that as the last line of the file.
   */
  private void printReport() {
    PrintStream ps, ds;
    try {
      ps = new PrintStream(new File(outfile));
      ds = new PrintStream(new File(datafile));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new UIMA_IllegalStateException();
    }
    for (Answer a : outputBuffer) {
      String s = a.getReport();
      ps.println(s);
      System.out.println(s);
    }
    for (Integer qid : qMap.keySet()) {
      Question query = qMap.get(qid).get(0);
      ds.println("QUESTION " + qid + "\n");
      ds.println(query.getDocText());
      ds.println("\nANSWERS\n");
      for (Answer ans : ansMap.get(qid)) {
        ds.println(ans.getReport());
      }
      ds.println("\n");
    }

    // TODO :: compute the metric:: mean reciprocal rank
    Double metric_mrr = compute_mrr();
    ps.println(" (MRR) Mean Reciprocal Rank :: " + metric_mrr);
    ps.close();
    ds.close();
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
        Question q = new Question(queryId, doc.getText(), tokenFrequencies);
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
      Question query = qMap.get(qid).get(0);
      for (Answer ans : ansMap.get(qid)) {
        Double cosSim = sim.computeSimilarity(query, ans);
        ans.setSimilarity(cosSim);
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
   * outputBuffer -- assumes list is already sorted
   * 
   * @param alist
   * @param qid
   */
  private void addReportResults(ArrayList<Answer> alist, Integer qid) {
    for (int i = 0; i < alist.size(); i++) {
      Answer a = alist.get(i);
      a.setRank(i + 1);
      if (a.getRelevance() == 1) {
        outputBuffer.add(a);
      }
    }
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
