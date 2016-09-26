/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus.lucene;


import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.res.corpus.TermNormal;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
@Deprecated
public class LuceneTSFactory {

    protected Multiset<String> ctTally = ConcurrentHashMultiset.<String>create();
    protected Multiset<String> plTally = ConcurrentHashMultiset.<String>create();

    private final ThreadLocal<Map<String,UIMATokenStream>> streams = ThreadLocal.withInitial( () -> ( new HashMap<>() ) );
    
    private final Map<String,TermNormal> fieldMap;
    
    public LuceneTSFactory( Map<String,TermNormal> fields ) {
        this.fieldMap = ImmutableMap.copyOf( fields );
    }

    public TokenStream makeTokens( String field, Collection<AnnotationFS> tokens, int offset ) {
        UIMATokenStream uts = streams.get().computeIfAbsent( field, s -> initStream( field ) );
        return uts.setInput( tokens, offset );
    }

    protected UIMATokenStream initStream( String field ) {
        return new UIMATokenStream( fieldMap.get( field ) );
    }

    protected class UIMATokenStream extends TokenStream {
        // Input data
        private Collection<AnnotationFS> src;
        private Integer offset;

        // State data
        private Iterator<AnnotationFS> annIt;
        private AnnotationFS cur;
        private Integer last = 0;

        private final TermNormal termNormal;
        
        public UIMATokenStream( TermNormal tn ) {
            this.termNormal = tn;
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
//                getAttribute( CharTermAttribute.class ).append( dump( cur ) );
                getAttribute( CharTermAttribute.class ).append( termNormal.term( cur ) );
//                getAttribute( PayloadAttribute.class ).setPayload( new BytesRef( getPayload( cur ) ) );
                getAttribute( PayloadAttribute.class ).setPayload( new BytesRef( termNormal.data( cur ) ) );
//                getAttribute( TypeAttribute.class ).setType( cur.getType().getName() );
                getAttribute( TypeAttribute.class ).setType( termNormal.type( cur ) );
                return true;
            } else return false;
        }
    }
}
