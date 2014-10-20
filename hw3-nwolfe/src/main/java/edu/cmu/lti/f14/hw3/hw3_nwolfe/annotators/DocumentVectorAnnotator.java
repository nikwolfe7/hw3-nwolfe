package edu.cmu.lti.f14.hw3.hw3_nwolfe.annotators;

import java.util.*;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.f14.hw3.hw3_nwolfe.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  HashMap<String, Integer> tokenCounter;

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    this.tokenCounter = new HashMap<String, Integer>();
  }

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
   * @param doc
   *          input text
   * @return a list of tokens.
   */
  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();
    for (String s : doc.split("\\s+"))
      res.add(s);
    return res;
  }

  /**
   * 
   * @param jCas
   * @param doc
   */
  private void createTermFreqVector(JCas jCas, Document doc) {
    String docText = doc.getText();
    List<String> list = tokenize0(docText);
    /*
     * Count occurrences of words
     */
    for (String token : list) {
      if (tokenCounter.containsKey(token)) {
        Integer update = tokenCounter.get(token) + 1;
        tokenCounter.put(token, update);
      } else {
        tokenCounter.put(token, new Integer(1));
      }
    }
    /**
     * Turn everything into tokens
     */
    List<Token> tokenList = new Stack<Token>();
    for (String s : tokenCounter.keySet()) {
      Token tkn = new Token(jCas);
      tkn.setText(s);
      tkn.setFrequency(tokenCounter.get(s));
      tokenList.add(tkn);
    }
    /*
     * Add list to document
     */
    FSList fsList = Utils.fromCollectionToFSList(jCas, tokenList);
    doc.setTokenList(fsList);
  }
}
