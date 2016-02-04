/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.run;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.resource.ResourceInitializationException;

/**
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public class CPERunner implements Runnable {
    private final CollectionProcessingEngine cpe;
    private final CallbackListener listen;
    
    public CPERunner( CallbackListener listen, CollectionProcessingEngine cpe ) {
        this.cpe = cpe;
        this.listen = listen;
    }
    
    @Override
    public void run() {
        this.cpe.addStatusCallbackListener( this.listen );
        try {
            cpe.process();
            synchronized( listen ) {
                while( listen.isRunning() ) {
                    listen.wait();
                }
            }
        } catch( ResourceInitializationException | InterruptedException ex ) {
            Logger.getLogger( CPERunner.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }
    
}
