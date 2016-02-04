/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus.token;


import edu.columbia.incite.uima.api.corpus.TokenFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.BytesRef;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;


/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class LuceneTSFactory extends Resource_ImplBase implements TokenFactory<TokenStream> {
    
    public static final String PARAM_TALLY = "tallying";
    @ConfigurationParameter( name = PARAM_TALLY, mandatory = false )
    protected Boolean tallying = false;
    
    protected Multiset<String> ctTally = ConcurrentHashMultiset.<String>create();
    protected Multiset<String> plTally = ConcurrentHashMultiset.<String>create();

    private ThreadLocal<Map<String,UIMATokenStream>> streams = ThreadLocal.withInitial( () -> ( new HashMap<>() ) );
    
    @Override
    public boolean initialize( ResourceSpecifier spec, Map<String,Object> paras )
    throws ResourceInitializationException {
        boolean ret = super.initialize( spec, paras );
        
        streams = ThreadLocal.withInitial( () -> ( new HashMap<>() ) );
        
        return ret;
    }
    
    @Override
    public void configure( CAS cas ) {
    }
    
    @Override
    public TokenStream makeTokens( String field, Collection<AnnotationFS> tokens, int offset ) throws CASException {
        if( !streams.get().containsKey( field ) ) {
            streams.get().put( field, createTokenStreamInstace() );
        }
        UIMATokenStream uts = streams.get().get( field );
//        System.out.println( String.format( "thread: %s, field: %s, ts: %d", Thread.currentThread().getName(), field, uts.hashCode() ) );
        return uts.setInput( tokens, offset );
    }
    
    protected UIMATokenStream createTokenStreamInstace() {
        return new NaiveTokenStream();
    }
    
    public class NaiveTokenStream extends UIMATokenStream {
            @Override
            protected String getCharTerm( AnnotationFS cur ) {
                String ct = cur.getCoveredText();
                if( tallying ) ctTally.add( ct );
                return ct;
            }

            @Override
            protected BytesRef getPayload( AnnotationFS cur ) {
                return new BytesRef();
            }        
    }
}
