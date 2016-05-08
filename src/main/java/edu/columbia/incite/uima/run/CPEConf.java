/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.run;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.columbia.incite.uima.io.readers.BinaryReader;
import edu.columbia.incite.util.io.FileUtils;

/**
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public class CPEConf extends edu.columbia.incite.util.run.Conf {

    public static final String META_DIR        = "metaDir";
    public static final String ACTION_ON_ERROR = "actionOnError";
    public static final String DUMP_METADATA   = "dumpMetadata";
    public static final String THREADS         = "threads";
    public static final String READER_CLASS    = "readerClass";
    public static final String AE_CLASSES      = "aeCLasses";
    public static final String CONSUMER        = "consumer";
    
    public static final String NS = CPEConf.class.getPackage().getName();

    public CPEConf( Properties props ) {
        this( NS, props );
    }
    
    public CPEConf( String ns, Properties props ) {
        super( ns, props );
    }

    public String actionOnError() {
        return getString( ACTION_ON_ERROR, "continue" );
    }

    public int maxThreads() {
        return getInteger( THREADS, Runtime.getRuntime().availableProcessors() );
    }

    public Class readerClass() {
        return getClass( READER_CLASS, BinaryReader.class );
    }

    public List<Class> aeClasses() {
        String[] clzs = getStringArray( AE_CLASSES, new String[]{} );
        List<Class> out = new ArrayList<>();
        for( String clz : clzs ) {
            try {
                out.add( Class.forName( clz ) );
            } catch ( ClassNotFoundException ex ) {
                throw new RuntimeException( "I hate java." );
            }
        }
        return out;
    }

    public Class consumer() {
        return getClass( CONSUMER, null );
    }

    public String metaDir() {
        return getString( META_DIR, System.getProperty( "user.dir" ) + "/meta" );
    }
    
    public boolean dumpMetaData() {
        return getBoolean( DUMP_METADATA, false );
    }
    
    public void dump( String dir ) {
        String now = LocalDate.now().format( DateTimeFormatter.ofPattern( "yyyy-MM-dd" ) );
        try( Writer wrt = FileUtils.getWriter( dir, now + ".conf", true, false ) ) { // TODO hardcoded extension
            wrt.write( this.toString() );
        } catch ( IOException ex ) {
            Logger.getLogger( CPEConf.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

}
