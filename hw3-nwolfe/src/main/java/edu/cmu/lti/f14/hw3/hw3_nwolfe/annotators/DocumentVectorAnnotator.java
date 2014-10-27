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
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Counter;
import edu.cmu.lti.f14.hw3.hw3_nwolfe.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  Counter tokenCounter;
  
  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    this.tokenCounter = new Counter();
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
   * 
   * @param jCas
   * @param doc
   */
  private void createTermFreqVector(JCas jCas, Document doc) {
    tokenCounter.empty();
    String docText = doc.getText();
    tokenCounter.tokenizeAndPutAll(docText, " ");
    /**
     * Turn everything into tokens
     */
    List<Token> tokenList = new ArrayList<Token>();
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
