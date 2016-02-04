/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res;

import org.apache.uima.analysis_component.AnalysisComponent;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <C>
 * @param <D>
 */
public abstract class AbstractDataResource<C,D> extends AbstractConfResource<C> {
    
    @Override
    public void register( AnalysisComponent user ) {
        if( isRegistered( user ) ) throw new IllegalStateException( "User already registered" );
        sessions.put( user, new UserDataSession<>() );
        initUser( user );
    }
    
    @Override
    protected UserDataSession<C,D> getSession( AnalysisComponent user ) {
        if( isRegistered( user ) ) return (UserDataSession) sessions.get( user );
        throw new IllegalStateException( "User is not registered" );
    }
    
    protected void configureData( AnalysisComponent user, D data ) {
        getSession( user ).data( data );
    }
    
    protected D getData( AnalysisComponent user ) {
        return getSession( user ).data();
    }
}
