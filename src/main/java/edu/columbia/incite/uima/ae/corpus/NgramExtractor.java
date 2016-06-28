/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.corpus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.columbia.incite.uima.ae.AbstractEngine;
import edu.columbia.incite.uima.api.types.Mark;
import edu.columbia.incite.uima.api.types.Paragraph;

/**
 *
 * @author gorgonzola
 */
public class NgramExtractor extends AbstractEngine {

    private int maxN = 2;
    
    private Map<String,AtomicLong> freqs = new HashMap<>();
    
    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        Map<Paragraph,Collection<Mark>> paras = JCasUtil.indexCovered( jcas, Paragraph.class, Mark.class );
        
        for( Paragraph para : paras.keySet() ) {
            String txt = para.getCoveredText();
            String[] words = txt.split( "\\s" );
            
            WORD: for( int i = 0; i < words.length; i++ ) {
                List<CharSequence> ngram = new ArrayList<>();
                for( int pos = 0; ( pos < maxN && pos + i < words.length ); pos++ ) {
                    ngram.add( words[ pos + i ] );
                    count( ngram );
                }
            }
        }
        
        int i = 0;
    }

    private void count( List<CharSequence> ngram ) {
        CharSequence[] parts = ngram.toArray( new CharSequence[ngram.size()] );
        String txt = String.join( " ", parts );
        AtomicLong f = freqs.computeIfAbsent( txt, ( s ) -> new AtomicLong() );
        f.incrementAndGet();
    }
    
}
