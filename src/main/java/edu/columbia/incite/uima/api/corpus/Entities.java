/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.corpus;

import java.util.Locale;
import java.util.function.Function;

import edu.columbia.incite.uima.api.types.Span;

/**
 *
 * @author gorgonzola
 */
public class Entities {
    
    public static final String BASE = "ENT";
    public static final String SEP = Tokens.SEP;
    
    public static final int TYPE = 0;
    public static final int ID   = 1;
    public static final int TEXT = 2;
    public static final int DUMP = 3;
    
    public static String[] parse( Span span ) {
        return parse( span, false );
    }
    
    public static String[] parse( Span span, boolean dump ) {
        String type = span.getType().getShortName();
        String id = span.getId();
        String text = span.getCoveredText();
        String dumps = dump ? span.toString() : "";
        return new String[]{ type, id, text, dumps };
    }
        
    public enum EAction implements Function<Span,String> {
        NONE,
        ADD_TYPE,
        ADD_ID,
        ADD_TEXT,
        ADD_DUMP,
        ;
        
        @Override
        public String apply( Span t ) {
            String[] parse = parse( t );
            String[] parts = new String[this.ordinal()];
            System.arraycopy( parse, 0, parts, 0, this.ordinal() );
            return String.join( SEP, parts ).toUpperCase( Locale.ROOT );
        }
    }
}
