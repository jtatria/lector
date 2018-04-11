/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.run;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import edu.columbia.incite.Conf;

/**
 *
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class Logs {
    
    public static void initLogs( Conf conf ) throws IOException {
        if( Files.exists( conf.logProps() ) ) {
            LogManager.getLogManager().readConfiguration(
                new FileInputStream( conf.logProps().toFile() )
            );
        }
    }

    public static void infof( String f, Object... args ) {
        logf( Level.INFO, f, args );
    }

    public static void warnf( String f, Object... args ) {
        logf( Level.WARNING, f, args );
    }

    public static void errorf( String f, Object... args ) {
        logf( Level.SEVERE, f, args );
    }

    public static void logf( Level lvl, String f, Object... args ) {
        Logger.getLogger( Conf.DFLT_NS ).log( lvl, String.format( f, args ) );
    }

    public static Logger logger() {
        return Logger.getLogger( Conf.DFLT_NS );
    }
}
