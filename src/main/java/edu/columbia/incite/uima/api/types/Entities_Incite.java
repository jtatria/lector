/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.types;

import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.types.Span;

/**
 *
 * @author gorgonzola
 */
public class Entities_Incite {
    
    public static String[] parse( AnnotationFS ann, boolean dump ) {
        if( !isEntity( ann ) ) throw new IllegalArgumentException();
        Span span = (Span) ann;
        String type  = span.getType().getShortName();
        String id    = span.getId();
        String text  = span.getCoveredText();
        String dumps = dump ? span.toString() : "";
        return new String[]{ type, id, text, dumps };
    }
    
    public static boolean isEntity( AnnotationFS ann ) {
        return ann.getClass().isAssignableFrom( Span.class );
    }
}
