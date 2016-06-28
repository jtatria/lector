/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.annotator;


import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.ae.AbstractEngine;
import edu.columbia.incite.uima.api.types.Paragraph;

/**
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public class ParaFixer extends AbstractEngine {

    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        
        int ct = 0;
        AnnotationIndex<Paragraph> ps = jcas.getAnnotationIndex( Paragraph.class );
        for( Paragraph p : ps ) {
            if( p.getBegin() == p.getEnd() ) {
                ct++;
                p.removeFromIndexes();
            }
        }
        
        getLogger().log( Level.INFO, "Parafixer: removed {0} empty paras from {1}",
            new Object[]{ ct, getDocumentId() } );
    }



}
