/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.debug;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import edu.columbia.incite.uima.ae.AbstractEngine;

/**
 *
 * @author gorgonzola
 */
public class NullAE extends AbstractEngine {

    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        System.out.printf( "Processing %s\n", getDocumentId() );
    }
    
}
