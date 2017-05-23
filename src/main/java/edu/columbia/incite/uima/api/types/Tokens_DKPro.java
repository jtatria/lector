/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.types;

import java.util.Locale;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.cas.text.AnnotationFS;

/**
 *
 * @author gorgonzola
 */
public class Tokens_DKPro {

    public static String[] parse( AnnotationFS token ) {
        if( !isToken( token ) ) throw new IllegalArgumentException();
        Token t = (Token) token;
        String posg  = t.getPos().getType().getShortName();
        String port  = t.getPos().getPosValue();
        String lemma = t.getLemma().getValue().toLowerCase( Locale.ROOT );
        String raw   = t.getCoveredText().toLowerCase( Locale.ROOT );
        return new String[]{ posg, port, lemma, raw };
    }
    
    public static boolean isToken( AnnotationFS token ) {
        return token.getClass().isAssignableFrom( Token.class );
    }
    
}
