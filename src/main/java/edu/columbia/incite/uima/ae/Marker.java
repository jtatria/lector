/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;

/**
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public class Marker extends AbstractEngine {

    private static int global = 0;
    private int local;

    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        String id = getDocumentId();
        getLogger().log( Level.INFO, "Marker engine running on document {0}. {1} documents seen, {2} total documents.",
            new Object[]{ id, Integer.toString( ++local ), Integer.toString( ++global ) }
        );
    }

}
