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
package edu.columbia.incite.resources.casio;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceConfigurationException;

import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.data.DataField;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class BrokerDecorator extends Resource_ImplBase implements FeatureBroker<Datum> {

    public static final String RES_DELEGATE = "delegate";
    @ExternalResource( key = RES_DELEGATE, api = FeatureBroker.class, mandatory = false )
    public FeatureBroker<Datum> delegate;

    private Map<DataField,Function<AnnotationFS,? extends Serializable>> decorations;

    public BrokerDecorator( FeatureBroker delegate, Map<DataField,Function<AnnotationFS,? extends Serializable>> decorations ) {
        this.delegate = delegate;
        this.decorations = decorations;
    }

    public void addDecoration( DataField f, Function<AnnotationFS,? extends Serializable> decorator ) {
        decorations.put( f, decorator );
    }

    @Override
    public Datum values( AnnotationFS ann ) throws CASException {
        Datum d = delegate.values( ann );
        return decorate( d, ann );
    }

    @Override
    public void values( AnnotationFS ann, Datum tgt ) throws CASException {
        delegate.values( ann, tgt );
        decorate( tgt, ann );
    }

    private Datum decorate( Datum d, AnnotationFS ann ) {
        for( DataField f : decorations.keySet() ) {
            Serializable v = decorations.get( f ).apply( ann );
            d.put( f, v );
        }
        return d;
    }
//
//    @Override
//    public void configure( CAS conf ) throws CASException {
//        try {
//            delegate.configure( conf );
//        } catch( Exception ex ) {
//            throw new CASException( ex );
//        }
//    }

    @Override
    public void configure( CAS conf ) throws ResourceConfigurationException {
        delegate.configure( conf );
    }

}
