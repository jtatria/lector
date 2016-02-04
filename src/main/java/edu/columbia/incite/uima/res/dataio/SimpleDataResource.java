/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.dataio;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.uima.analysis_component.AnalysisComponent;

import edu.columbia.incite.uima.res.AbstractDataResource;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class SimpleDataResource<D> extends AbstractDataResource<D,D> {
    
    private BiFunction<D,D,D> merger;
    private Consumer<D> finisher;  
    private Consumer<D> consumer;
    
    public D out;

    public void register( AnalysisComponent user, D data ) {
        super.register( user );
        configureData( user, data );
    }
    
    public D getUserData( AnalysisComponent user ) {
        return getData( user );
    }
    
    @Override
    protected void initUser( AnalysisComponent user ) {
        // nothing by default.
    }

    @Override
    public void finish( AnalysisComponent user ) {
        if( finisher != null ) finisher.accept( getData( user ) );
    }
    
    @Override
    public void unregister( AnalysisComponent user ) {
        if( merger == null || consumer == null ) throw new IllegalStateException(
            "Merger or Consumer not set."
        );
        
        if( out == null ) out = getData( user );
        else {
            out = merger.apply( out, getData( user ) );
        }
        super.unregister( user );
        if( sessions.isEmpty() ) {
            consumer.accept( out );
        }
    }    
    
    public boolean setMerger( BiFunction<D,D,D> merger ) {
        if( this.merger != null ) return false;
        this.merger = merger;
        return true;
    }
    
    public boolean setFinisher( Consumer<D> finisher ) {
        if( this.finisher != null ) return false;
        this.finisher = finisher;
        return true;
    }
    
    public boolean setConsumer( Consumer<D> consumer ) {
        if( this.consumer != null ) return false;
        this.consumer = consumer;
        return true;
    }
    
}
