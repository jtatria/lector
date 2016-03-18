/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus.token;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.uima.cas.text.AnnotationFS;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public abstract class UIMATokenStream extends TokenStream {
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
            getAttribute( CharTermAttribute.class ).append( getCharTerm( cur ) );
            getAttribute( PayloadAttribute.class ).setPayload( new BytesRef( getPayload( cur ) ) );
            getAttribute( TypeAttribute.class ).setType( cur.getType().getName() );
            return true;
        } else return false;
    }

    protected abstract String getCharTerm( AnnotationFS cur );

    protected abstract byte[] getPayload( AnnotationFS cur );

}
