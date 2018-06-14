/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.index;

import org.apache.uima.cas.text.AnnotationFS;

/**
 * {@link Tokenizer} implementation for Annotations of unknown type.
 * Returns covered text as charterm, and short type name as payload and type.
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class NaiveTokenizer implements Tokenizer {

    @Override
    public String charterm( AnnotationFS ann ) {
        return ann.getCoveredText();
    }

    @Override
    public byte[] payload( AnnotationFS ann ) {
        return ann.getType().getShortName().getBytes( CS );
    }
    
}
