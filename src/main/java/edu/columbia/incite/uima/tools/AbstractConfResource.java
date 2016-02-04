/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.OLDSessionResource;

/**
 * Base implementation for a ReactiveResource capable of holding per-user configuration and data.
 * 
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <C>   Type of configuration objects for this resource.
 * 
 */
public abstract class AbstractConfResource<C> 
extends Resource_ImplBase implements OLDSessionResource<AnalysisComponent,C> {

    protected final Map<AnalysisComponent,UserSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void register( AnalysisComponent user ) {
        if( isRegistered( user ) ) throw new IllegalStateException( "User already registered" );
        sessions.put( user, new UserSession<>() );
        initUser( user );
    }
    
    protected boolean isRegistered( AnalysisComponent user ) {
        if( user != null ) return sessions.containsKey( user );
        throw new IllegalArgumentException( "User can not be null" );
    }

    @Override
    public void configure( AnalysisComponent user, C conf ) throws UIMAException {
        if( conf == null ) {
            getLogger().log( Level.WARNING, "null configuration passed to {0} instance."
                , new Object[]{ this.getClass().getSimpleName() }
            );
        }
        getSession( user ).conf( conf );
    }
    
    @Override
    public void finish( AnalysisComponent user ) {
        // nothing by default;
    }
    
    @Override
    public void reset( AnalysisComponent user ) {
        getSession( user ).clearConf();
        initUser( user );
    }
    

    @Override
    public void unregister( AnalysisComponent user ) {
        if( isRegistered( user ) ) {
            getSession( user ).clearConf();
            sessions.remove( user );
        }
    }
    
    protected UserSession<C> getSession( AnalysisComponent user ) {
        if( isRegistered( user ) ) return sessions.get( user );
        throw new IllegalStateException( "User is not registered" );
    }
    
    protected C getConf( AnalysisComponent user ) {
        return getSession( user ).conf();
    }

    protected abstract void initUser( AnalysisComponent user );

}
