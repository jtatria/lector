/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.casio;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;

import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.data.DataField;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class DecoratingBroker extends Resource_ImplBase implements FeatureBroker<Datum> {

    public static final String RES_DELEGATE = "delegate";
    @ExternalResource( key = RES_DELEGATE, api = FeatureBroker.class, mandatory = false )
    public FeatureBroker<Datum> delegate;

    private Map<DataField,Function<AnnotationFS,? extends Serializable>> decorations;

    public DecoratingBroker( FeatureBroker delegate, Map<DataField,Function<AnnotationFS,? extends Serializable>> decorations ) {
        this.delegate = delegate;
        this.decorations = decorations;
    }

    public void addDecoration( DataField f, Function<AnnotationFS,? extends Serializable> decorator ) {
        decorations.put( f, decorator );
    }

    @Override
    public Datum values( AnnotationFS ann, boolean merge ) throws CASException {
        Datum d = delegate.values( ann, merge );
        return decorate( d, ann );
    }

    @Override
    public void values( AnnotationFS ann, Datum tgt, boolean merge ) throws CASException {
        delegate.values( ann, tgt, merge );
        decorate( tgt, ann );
    }

    private Datum decorate( Datum d, AnnotationFS ann ) {
        for( DataField f : decorations.keySet() ) {
            Serializable v = decorations.get( f ).apply( ann );
            d.set( f, v );
        }
        return d;
    }

    @Override
    public void configure( CAS conf ) throws CASException {
        try {
            delegate.configure( conf );
        } catch( Exception ex ) {
            throw new CASException( ex );
        }
    }

}
