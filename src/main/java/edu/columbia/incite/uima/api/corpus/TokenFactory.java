/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.corpus;

import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.ConfigurableResource;

/**
 * A shared resource that produces token streams from a collection of UIMA annotations.
 * 
 * @author Jose Tomas Atria <jtatria@gmail.com>
 * @param <T>   A type to contain created tokens.
 */
public interface TokenFactory<T> extends ConfigurableResource<CAS> {
    /**
     * Produce a valid instance of T from the given UIMA annotations.
     * 
     * The given offset value will be substracted from the begin and end offsets of the given
     * annotations for cases in which the index offsets do not correspond to CAS SOFA offsets (e.g.
     * if CAS's do not correspond to indexed documents).
     *
     * @param field
     * @param tokens A collection of UIMA annotations.
     * @param offset An integer indicating an offset to be substracted from the begin and end 
     *               values of the given UIMA annotations.
     *
     * @return A valid instance of T.
     *
     * @throws CASException If an error occurs when accessing CAS data for any of the given 
     *         annotations.
     */
    T makeTokens( String field, Collection<AnnotationFS> tokens, int offset ) throws CASException;
}
