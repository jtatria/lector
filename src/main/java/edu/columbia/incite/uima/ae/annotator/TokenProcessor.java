/* 
 * Copyright (C) 2015 Jose Tomas Atria <jtatria@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.columbia.incite.uima.ae.annotator;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

import edu.columbia.incite.uima.ae.AbstractEngine;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.columbia.incite.util.reflex.annotations.Resource;

/**
 *
 * @author José Tomás Atria
 */
public class TokenProcessor extends AbstractEngine {
    
    @Resource
    List<Token> tokens;

    public static final String DOTS = ".";
    
    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );
        tokens  = new ArrayList( JCasUtil.select( jcas, Token.class ) );
    } 

    @Override
    protected void realProcess(JCas jcas) throws AnalysisEngineProcessException {
        List<TokenTriple> triples = new ArrayList<>();
        
        for( int i = 0; i < tokens.size(); i++ ) {
            Token cur = tokens.get( i );
            if( cur.getCoveredText().equals( DOTS ) ) {
                Token pre = i - 1 < 0 ? null : tokens.get( i - 1 );
                Token pos = i + 1 >= tokens.size() ? null : tokens.get( i + 1 );
                triples.add( new TokenTriple( pre, cur, pos ) );
            }
        }        
    }
    
    private class TokenTriple {
        Token pre;
        Token cur;
        Token pos;
        
        public TokenTriple( Token pre, Token cur, Token pos ) {
            this.pre = pre;
            this.cur = cur;
            this.pos = pos;
        }
    }
    
}
