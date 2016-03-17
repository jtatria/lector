/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.annotator;

import java.util.Collection;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.columbia.incite.uima.ae.AbstractEngine;
import edu.columbia.incite.uima.api.types.Paragraph;
import edu.columbia.incite.uima.api.types.Segment;

/**
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public class ParaFixer extends AbstractEngine {

    Map<Paragraph,Collection<Segment>> idx;

    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        idx = JCasUtil.indexCovering( jcas, Paragraph.class, Segment.class );

        int i = 0;
    }

}
