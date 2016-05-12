/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.corpus;

import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.ConfigurableResource;

/**
 *
 * @author gorgonzola
 */
public interface CorpusReader extends ConfigurableResource<CAS> {
    
    void read( AnnotationFS doc, Collection<AnnotationFS> covers, Collection<AnnotationFS> tokens );
    
}
