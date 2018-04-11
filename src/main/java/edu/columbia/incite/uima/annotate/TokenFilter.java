/* 
 * Copyright (C) 2017 José Tomás Atria <jtatria at gmail.com>
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
package edu.columbia.incite.uima.annotate;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.columbia.incite.uima.AbstractProcessor;

import java.util.regex.Pattern;

import edu.columbia.incite.util.Reflection.CasData;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class TokenFilter extends AbstractProcessor {
    
    public static final String PARAM_PERIOD_REGEX = "periodRegex";
    @ConfigurationParameter( name = PARAM_PERIOD_REGEX, mandatory = false )
    private String periodRegex;
    
    public static final String PARAM_EXTRA_ABBR = "extraAbbr";
    @ConfigurationParameter( name = PARAM_EXTRA_ABBR, mandatory = false )
    private String[] extraAbbr;
    
    public static final String PARAM_CHAR_WINDOW = "windowSize";
    @ConfigurationParameter( name = PARAM_CHAR_WINDOW, mandatory = false )
    private int windowSize = 20;
    
    private Pattern period;
    private Pattern abbrev;
    
    @CasData
    private List<Token> tokens;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        period = Pattern.compile( "^" + periodRegex + "$" );
        
        if( extraAbbr == null ) {
            extraAbbr = new String[]{
                "Knt",
                "Serj",
                "Esq",
                "viz",
                "£",
                "[0-9]+[lsd]"
            };
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append( "^" );
        for( String abbr : extraAbbr ) {
            if( sb.length() > 1 ) sb.append( "|" );
            sb.append( abbr );
        }
        sb.append( "$" );
        
        abbrev = Pattern.compile( sb.toString() );
    }
    
    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );
        tokens = new ArrayList( JCasUtil.select( jcas, Token.class ) );
    }
    
    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        Token pre = null;
        Token cur = null;
        Token pos = null;
        int l = tokens.size();
        for( int i = 0; i < tokens.size(); i++ ) {
            pre = cur != null ? cur : pre;
            cur = tokens.get( i );
            pos = i + 1 < l ? tokens.get( i + 1 ) : null;
            
            if( period.matcher( cur.getCoveredText() ).find() ) {
                if( pos != null && pos.getCoveredText().matches( "^[a-z]" ) ) {
                    int preI = i - windowSize < 0 ? 0 : i - windowSize;
                    int posI = i + windowSize > l ? l : i + windowSize;
                    List<Token> preList = i > 0 ? tokens.subList( preI, i ) : null;
                    List<Token> posList = i + 1 < l ? tokens.subList( i + 1, posI ) : null;
                    
                    boolean match = pos != null &&
                            pos.getCoveredText().matches( "^[a-z]" ) && 
                            pre != null &&
                            abbrev.matcher( pre.getCoveredText() ).find();
                    
                    dumpTokens( match, preList, cur, posList );
//                    
//
//                    if( pre != null && Arrays.asList( extraAbbr ).contains( pre.getCoveredText() ) ) {
//                        mergeWithPre( jcas, pre, cur );
//                    } else if( pre.getCoveredText().matches( "\\d+[lsd]" ) ) {
//                        mergeWithPre( jcas, pre, cur );
//                    }
//                    else {
//                        System.out.println( String.format( "%s %s %s\t\t\t:%s",
//                            pre != null ? pre.getCoveredText() : "NULL",
//                            cur.getCoveredText(),
//                            pos.getCoveredText(),
//                            getSurroundingText( jcas, pre, cur, pos )
//                        ) );
//                    }
                } 
            }
        }
        
        
    }
    
    private String getSurroundingText( JCas jcas, Token pre, Token cur, Token pos ) {
        int lo = ( pre != null ? pre.getBegin() : cur.getBegin() ) - windowSize;
        int hi = ( pos != null ? pos.getEnd() : cur.getEnd() ) + windowSize;
        int l = jcas.getDocumentText().length();
        
        return jcas.getDocumentText().substring( lo > 0 ? lo : 0, hi <= l ? hi : l );
    }

    private void mergeWithPre( JCas jcas, Token pre, Token cur ) {
        Token neu = new Token( jcas );
        neu.setBegin( pre.getBegin() );
        neu.setEnd( cur.getEnd() );
        jcas.removeFsFromIndexes( pre );
        jcas.removeFsFromIndexes( cur );
        jcas.addFsToIndexes( neu );
    }

    private void dumpTokens( boolean match, List<Token> pre, Token cur, List<Token> pos ) {
        StringBuilder sb = new StringBuilder();
        String sep = "|";
        
        sb.append( match ? "YES" : "NO" );
        sb.append( "\t" );
        if( pre != null ) {
            for( Token token : pre ) {
                if( token == null ) continue;
                sb.append( sb.length() == 0 ? "" : sep );
                sb.append( token.getCoveredText() );
            }
        }
        sb.append( ">>>" ).append( cur.getCoveredText() ).append( "<<<" );
        if( pos != null ) {
            for( Token token : pos ) {
                if( token == null ) continue;
                if( sb.charAt( sb.length() - 1 ) != '\t' ) sb.append( sep );
                sb.append( token.getCoveredText() );
            }
        }
        
        System.out.println( sb.toString() );
    }
    
    
}
