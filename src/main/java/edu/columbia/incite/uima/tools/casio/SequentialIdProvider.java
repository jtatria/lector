/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.tools.casio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

import edu.columbia.incite.uima.api.casio.IdProvider;

/**
 * An {@link IdProvider} implementation that provides sequential integer-valued id's for annotations.
 * Uniqueness may be defined with respect to all annotations, or with respect to each annotation type.
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class SequentialIdProvider extends Resource_ImplBase implements IdProvider<Integer> {
    
    /**
     * Provide id's per-type (i.e. the same id may be used for annotations of different type).
     */
    public static final String PARAM_SPLIT_BY_TYPE = "Consider type for id";
    @ConfigurationParameter( name = PARAM_SPLIT_BY_TYPE, mandatory = false, defaultValue = "true" )
    private Boolean useTypes;
    
    private final Map<Type,AtomicInteger> cur = new ConcurrentHashMap<>();
    
    @Override
    public boolean initialize( final ResourceSpecifier spec, final Map<String,Object> moarParas ) 
    throws ResourceInitializationException {
        super.initialize( spec, moarParas);
        
        cur.put( null, new AtomicInteger() );
        
        return true;
    }

    /**
     * Obtain an integer-valued id that is guaranteed to be unique within this UIMA session.
     * @param ann   Annotation to obtain an id for.
     * @return  An Integer, unique within this session, optionally for this annotation's type.s
     */
    @Override
    public Integer getId( AnnotationFS ann ) {
        Type key = useTypes ? ann.getType() : null;
        
        if( !cur.containsKey( key ) ) {
            cur.put( key, new AtomicInteger() );
        }
        
        return cur.get( key ).getAndIncrement();
    }

}
