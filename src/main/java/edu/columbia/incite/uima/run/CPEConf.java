/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import edu.columbia.incite.uima.io.readers.BinaryReader;
import edu.columbia.incite.uima.io.writers.BinaryWriter;

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

    public CPEConf( String ns, Properties props ) {
        super( ns, props );
    }

    public String getActionOnError() {
        return getString( ACTION_ON_ERROR, "continue" );
    }

    public int getNumberOfThreads() {
        return getInteger( THREADS, Runtime.getRuntime().availableProcessors() + 2 );
    }

    public Class getReaderClass() {
        return getClass( READER_CLASS, BinaryReader.class );
    }

    public List<Class> getAEClasses() {
        return getList( AE_CLASSES, Class.class, Collections.EMPTY_LIST );
    }

    public Class consumer() {
        return getClass( CONSUMER, null );
    }

    public String getMetaOutputDir() {
        return getString( META_DIR, System.getProperty( "user.dir" ) + "/meta" );
    }
}
