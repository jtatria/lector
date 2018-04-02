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

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.columbia.incite.io.readers.BinaryReader;
import edu.columbia.incite.util.FileUtils;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class CPEConf extends edu.columbia.incite.util.Conf {

    public static final String META_DIR        = "metaDir";
    public static final String ACTION_ON_ERROR = "actionOnError";
    public static final String DUMP_METADATA   = "dumpMetadata";
    public static final String THREADS         = "threads";
    public static final String READER_CLASS    = "readerClass";
    public static final String AE_CLASSES      = "aeClasses";
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
        return getString( META_DIR, System.getProperty( "user.dir" ) + "/data/conf" );
    }
    
    public boolean dumpMetaData() {
        return getBoolean( DUMP_METADATA, true );
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
