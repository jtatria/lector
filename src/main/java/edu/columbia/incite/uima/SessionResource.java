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
package edu.columbia.incite.uima;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.resource.Resource;

/**
 * A SessionResource provides means to control access to shared resources and maintain per-component
 * processing data.
 * 
 * This facilitates multithreaded deployment of shared resources, allowing resource instances to 
 * keep track of and synchronize requests from several callers, typically several instances of an 
 * analysis component in multithreaded pipelines.
 * 
 * Typical usage would be to implement a consumer in parallel to a running UIMA pipeline.
 * 
 * Components would usually obtain a session token via {@link #openSession() }, pass this token to 
 * subclass implementations, and then signal that they are done using the resource via the 
 * {@link #closeSession(java.lang.Object) } method. Implementations of this class should keep track
 * of all outstanding tokens.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 * 
 * @param <S> Type for an object that provides a component with access to its session data.
 */
public interface SessionResource<S> extends Resource {

    /**
     * Open a new session and return an instance of S that provides access to session data.
     * 
     * This interface's contract requires this method to be called before a component can access
     * session resources.
     * 
     * @return An instance of S that provides a component with access to session data.
     */
    S openSession();

    /**
     * Close the given session and release all held resources.
     * 
     * This interface's contract expects no further uses of any resources associated to the given
     * session.
     * 
     * @param session An instance of S created by {@link #openSession() }.
     */
    void closeSession( S session );
    
    /**
     * Simple base class for thread-safe session resources that track open sessions as unique longs.
     * Subclasses need to provide an implementation of closeResource to handle finalization logic 
     * after all sessions have been closed.
     * 
     * This class performs no additional checks.
     */
    public static abstract class Base extends Resource_ImplBase implements SessionResource<Long> {
        
        private final AtomicLong sssnProvider = new AtomicLong();
        private final Set<Long> sssns = Collections.newSetFromMap( new ConcurrentHashMap<>() );
        
        @Override
        public final Long openSession() {
            long sssn = sssnProvider.incrementAndGet();
            sssns.add( sssn );
            return sssn;
        }
        
        @Override
        public final void closeSession( Long sssn ) {
            sssns.remove( sssn );
            if( sssns.isEmpty() ) closeResource();
        }

        /**
         * Perform any finalization logic for this resource after all sessions have been closed.
         * 
         * This method is called automatically by {@link #closeSession(java.lang.Long) }
         * if the token passed to that 
         * method is the last outstanding session token.
         */
        protected abstract void closeResource();
    }
}
