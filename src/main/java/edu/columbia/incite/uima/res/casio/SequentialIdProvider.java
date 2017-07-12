/* 
 * Copyright (C) 2017 José Tomás Atria <jtatria at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.columbia.incite.uima.res.casio;

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
 * @author José Tomás Atria <jtatria@gmail.com>
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
