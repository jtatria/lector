/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.index;

import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.types.Span;

/**
 * Thread safe implementation of a @link{Tokenizer} for Incite's @link{Span}s.
 * 
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class InciteTokenizer implements Tokenizer {
// TODO: This class uses Span as base entity type. This is because it is easier to create entity 
// types by extending Span rather than the simple Entity type, because Entity is a fallback type.
// This should probabaly be revised in the TypeSystem to rename Span to Entity and Entity to 
// GenericEntity or some such.

    public static final int TYPE = 0;
    public static final int ID   = 1;
    public static final int TEXT = 2;
    
    private final ThreadLocal<Span>     curSpan  = new ThreadLocal<>();
    private final ThreadLocal<String[]> curParts = ThreadLocal.withInitial( () -> new String[3] );
    private final ThreadLocal<StringBuilder> tlsb = ThreadLocal.withInitial( () -> new StringBuilder() );

    public String[] parts( Span ann ) {
        if( curSpan.get() == null || curSpan.get() != ann ) {
            Span span = (Span) ann;
            curParts.get()[TYPE] = span.getType().getShortName();
            curParts.get()[ID]   = span.getId();
            curParts.get()[TEXT] = span.getCoveredText();
            curSpan.set( span );
        }
        return curParts.get();
    }
    
    @Override
    public String charterm( AnnotationFS ann ) {
        if( !isSpan( ann ) ) return NOTERM;
        Span span = (Span) ann;
        String[] parts = this.parts( span );
        if( !preFilter( parts ) ) return NOTERM;
        StringBuilder sb = tlsb.get();
        sb.delete( 0, sb.length() );
        sb.append( parts[ID] );
        return posFilter( sb.toString() );
    }

    @Override
    public byte[] payload( AnnotationFS ann ) {
        if( !isSpan( ann ) ) return NODATA;
        return parts( (Span) ann )[TYPE].getBytes( CS );
    }

    private boolean isSpan( AnnotationFS ann ) {
        return Span.class.isAssignableFrom( ann.getClass() );
    }    
}
