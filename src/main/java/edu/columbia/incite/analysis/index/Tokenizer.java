/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.analysis.index;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.lucene.util.BytesRef;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Interface for an object that translates UIMA annotations to strings and byte arrays for 
 * consumption by indexer objects.
 * This interface also defines standard characters and byte sequences for use as string separators 
 * and null values and standard character sets for string / byte conversions.
 * 
 * NB: Implementations of this interface need to be thread safe, as they will usually be shared by 
 * multiple consumer threads.
 * 
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public interface Tokenizer {
    
    /**
     * String separator used in field names, search keys, etc. Set to "_".
     */
    public static final String SEP = "_";

    /**
     * Empty term. The empty string.
     */
    public static final String NOTERM = "";
    
    /**
     * Empty payload data. The empty byte array.
     */
    public static final byte[] NODATA = BytesRef.EMPTY_BYTES;

    /**
     * Standard charset for data. UTF8 everywhere.
     */
    public static final Charset CS = StandardCharsets.UTF_8;
    
    /**
     * Create string term representation of the given UIMA annotation.
     * Implementations need to override this.
     * @param ann   A UIMA annotation.
     * @return The canonical string representation of the given annotation.
     */
    String charterm( AnnotationFS ann );
    
    /**
     * Create arbitrary data from the given UIMA annotation.
     * Implementations need to override this.
     * @param ann  A UIMA annotation.
     * @return Arbitrary data created from the given annotation.
     */
    byte[] payload( AnnotationFS ann );
    
    /**
     * Create string representation for categorization of UIMA annotations.
     * Defaults to the UIMA annotation type's shortname.
     * @param ann A UIMA annotation.
     * @return A string suitable for token classification.
     */
    default String type( AnnotationFS ann ) {
        return ann.getType().getShortName();
    }
    
    /**
     * @return @code{true} if this @link{Tokenizer} supplies charterms.
     */
    default boolean hasCharterm() {
        return true;
    }
    
    /**
     * @return @code{true} if this @link{Tokenizer} supplies payload data.
     */
    default boolean hasPayload() {
        return true;
    }
    
    /**
     * @return @code{true} if this @link{Tokenizer} supplies types.
     * @return 
     */
    default boolean hasType() {
        return true;
    }
    
}
