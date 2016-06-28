/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus.lucene;


import java.io.IOException;

import edu.columbia.incite.uima.api.corpus.TokenFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;


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

    private final ThreadLocal<Map<String,UIMATokenStream>> streams = ThreadLocal.withInitial( () -> ( new HashMap<>() ) );

    @Override
    public TokenStream makeTokens( String field, Collection<AnnotationFS> tokens, int offset ) throws CASException {
        UIMATokenStream uts = streams.get().computeIfAbsent( field, s -> initStream() );
        return uts.setInput( tokens, offset );
    }

    protected UIMATokenStream initStream() {
        return new UIMATokenStream(){
            @Override
            protected String dump( AnnotationFS cur ) {
                String ct = cur.getCoveredText();
                if( tallying ) ctTally.add( ct );
                return ct;
            }

            @Override
            protected byte[] getPayload( AnnotationFS cur ) {
                return BytesRef.EMPTY_BYTES;
            }
        };
    }

    @Override
    public void configure( CAS conf ) {
    }
    
    protected abstract class UIMATokenStream extends TokenStream {
        // Input data
        private Collection<AnnotationFS> src;
        private Integer offset;

        // State data
        private Iterator<AnnotationFS> annIt;
        private AnnotationFS cur;
        private Integer last = 0;

        public UIMATokenStream() {
            addAttribute( OffsetAttribute.class );
            addAttribute( CharTermAttribute.class );
            addAttribute( PayloadAttribute.class );
            addAttribute( TypeAttribute.class );
        }

        public UIMATokenStream setInput( Collection<AnnotationFS> tokens, int offset ) {
            this.src = tokens;
            this.offset = offset;
            return this;
        }

        @Override
        public void close() throws IOException {
            super.close();
            // Clear input.
            this.src = null;
            this.offset = null;
        }

        @Override
        public void end() throws IOException {
            super.end();
            addAttribute( OffsetAttribute.class ).setOffset( last, last );
            // Clear state.
            this.annIt = null;
            this.cur = null;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            if( src == null ) {
                throw new IllegalStateException( "Input not set" );
            }
            clearAttributes();
            annIt = src.iterator();
            last = 0;
        }

        @Override
        public boolean incrementToken() throws IOException {
            clearAttributes();
            if( annIt.hasNext() ) {
                cur = annIt.next();
                last = last > cur.getEnd() ? last : cur.getEnd();
                if( last > cur.getEnd() ) { // TODO This is naive and needs refactoring
                    addAttribute( PositionIncrementAttribute.class ).setPositionIncrement( 0 );
                }
                int b = cur.getBegin() - offset;
                int e = cur.getEnd() - offset;
                getAttribute( OffsetAttribute.class ).setOffset( b, e );
                getAttribute( CharTermAttribute.class ).append( dump( cur ) );
                getAttribute( PayloadAttribute.class ).setPayload( new BytesRef( getPayload( cur ) ) );
                getAttribute( TypeAttribute.class ).setType( cur.getType().getName() );
                return true;
            } else return false;
        }

        protected abstract String dump( AnnotationFS cur );

        protected abstract byte[] getPayload( AnnotationFS cur );

    }
}
