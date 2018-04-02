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
package edu.columbia.incite.run;

import java.util.ArrayList;
import java.util.List;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class CallbackListener implements StatusCallbackListener {

        private final List<Exception> exceptions = new ArrayList<>();
        private boolean isRunning = true;
        
        public boolean isRunning() { return isRunning; }

        @Override
        public void entityProcessComplete( CAS cas, EntityProcessStatus status ) {
            if( status.isException() ) {
                for( Exception e : status.getExceptions() ) {
                    exceptions.add( e );
                }
            }
        }

        @Override
        public void initializationComplete() {
        }

        @Override
        public void batchProcessComplete() {
        }

        @Override
        public void collectionProcessComplete() {
            stop();
        }

        @Override
        public void paused() {
        }

        @Override
        public void resumed() {
        }

        @Override
        public void aborted() {
            stop();
        }
        
        private void stop() {
            synchronized( this ) {
                if( isRunning ) {
                    isRunning = false;
                    notify();
                }
            }
        }
    }
