/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.analysis.index;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.types.Span;

/**
 * Scaffolding for a Tokenizer decorator that wraps other tokenizers to be selected at runtime.
 * Not used for now.
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class MergedTokenizer implements Tokenizer {
    
    private final Tokenizer tokens;
    private final Tokenizer entities;
    private final Tokenizer fallback;
    
    public MergedTokenizer( Tokenizer tokens, Tokenizer entities, Tokenizer fallback ) {
        this.tokens   = tokens;
        this.entities = entities;
        this.fallback = fallback;
    }

    @Override
    public String charterm( AnnotationFS ann ) {
        if( Token.class.isAssignableFrom( ann.getClass() ) ) return tokens.charterm( ann );
        if( Span.class.isAssignableFrom( ann.getClass() ) ) return entities.charterm( ann );
        return fallback.charterm( ann );
    }

    @Override
    public byte[] payload( AnnotationFS ann ) {
        if( Token.class.isAssignableFrom( ann.getClass() ) ) return tokens.payload( ann );
        if( Span.class.isAssignableFrom( ann.getClass() ) ) return entities.payload( ann );
        return fallback.payload( ann );
    }
}
