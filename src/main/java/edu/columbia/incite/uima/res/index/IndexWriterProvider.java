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
package edu.columbia.incite.uima.res.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.SessionResource;


/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class IndexWriterProvider extends Resource_ImplBase implements SessionResource<Long> {

    public static final String DFLT_OUTPUT_DIR = "data/index";
    
    public static final String PARAM_OUTPUT_DIR   = "outDir";
    @ConfigurationParameter( name = PARAM_OUTPUT_DIR, mandatory = false,
        description = "Index directory path.", defaultValue = DFLT_OUTPUT_DIR
    )
    private String outDir;
    
    public static final String PARAM_UPDATE_INDEX = "update";
    @ConfigurationParameter( name = PARAM_UPDATE_INDEX, mandatory = false, defaultValue = "false",
        description = "Update an existing index instead of creating a new one."
    )
    private Boolean update;

    public static final String PARAM_RAMBUFFER_SIZE = "rbSize";
    @ConfigurationParameter( name = PARAM_RAMBUFFER_SIZE, mandatory = false,
        description = "RAM buffer size."
    )
    private Double rbSize;

//    public static final String PARAM_MAX_THREADS = "maxThreads";
//    @ConfigurationParameter( name = PARAM_MAX_THREADS, mandatory = false,
//        description = "Maximum number of threads for indexing process."
//    )
//    private Integer maxThreads;

    public static final String PARAM_USE_COMPOUND_FILE = "compound";
    @ConfigurationParameter( name = PARAM_USE_COMPOUND_FILE, mandatory = false,
        description = "Write index using Lucene's compound file format."
    )
    private Boolean compound;

    public static final String PARAM_OPTIMIZE = "optimize";
    @ConfigurationParameter( name = PARAM_OPTIMIZE, mandatory = false, defaultValue = "true",
        description = "Optimize index on close"
    )
    private Boolean optimize;

    public static final String PARAM_MERGE_SEGMENTS = "segments";
    @ConfigurationParameter( name = PARAM_MERGE_SEGMENTS, mandatory = false, defaultValue = "1",
        description = "Merge index segments on close"
    )
    private Integer segments;

    public static final String PARAM_WIPE_EXISTING = "wipeExisting";
    @ConfigurationParameter( name = PARAM_WIPE_EXISTING, mandatory = false, defaultValue = "true",
        description = "Delete files in target directory before indexing"
    )
    private Boolean wipeExisting;

    private IndexWriter writer;

    private final AtomicLong sssnProvider = new AtomicLong();
    private final Set<Long> sessions = Sets.newConcurrentHashSet();

    @Override
    public Long openSession() {
        long sssn = this.sssnProvider.incrementAndGet();
        sessions.add( sssn );
        return sssn;
    }

    @Override
    public void closeSession( Long session ) {
        this.sessions.remove( session );
        if( this.sessions.isEmpty() ) {
            try {
                close();
            } catch ( IOException ex ) {
                getLogger().log( Level.SEVERE, "I/O error when trying to close index writer!", ex );
            }
        }
    }
    
    @Override
    public boolean initialize( ResourceSpecifier spec, Map<String,Object> params )
    throws ResourceInitializationException {
        boolean ret = super.initialize( spec, params );
        
        // Deferred to allow auto-initialization.
        initialize();
        
        return ret;
    }
    
    void initialize() throws ResourceInitializationException {
        
        try {
            Path path = Paths.get( outDir );
            if( !Files.isDirectory( path ) ) {
                Files.createDirectories( path );
            }

            if( wipeExisting ) {
                for( Path p : Files.newDirectoryStream( path ) ) {
                    Files.delete( p );
                }
            }

            FSDirectory dir = FSDirectory.open( path );

            IndexWriterConfig conf = new IndexWriterConfig( null );
            conf.setOpenMode( update ? OpenMode.APPEND : OpenMode.CREATE );
            conf.setRAMBufferSizeMB( rbSize != null ? rbSize : conf.getRAMBufferSizeMB() );
            conf.setUseCompoundFile( compound != null ? compound : conf.getUseCompoundFile() );
            conf.setCommitOnClose( true );

            writer = new IndexWriter( dir, conf );
            getLogger().log( Level.INFO,
                "WriterProvider: Writing index to directory {0} with codec {1}."
                , new Object[]{ dir.getDirectory().toString(), conf.getCodec().getName() }
            );
        } catch( IOException ex ) {
            throw new ResourceInitializationException( ex );
        }

    }

    public void index( Document doc ) throws IOException {
        writer.addDocument( doc );
    }

    public void close() throws IOException {
        if( optimize ) {
            
            getLogger().log( Level.INFO,
                "Index optimization requested. Merging index into {0} segments. This may take a while..."
                , new Object[]{ segments }
            );
            
            Stopwatch sw = Stopwatch.createStarted();
            writer.forceMerge( segments );
            sw.stop();
            
            getLogger().log(
                Level.INFO, "Index optimization completed in {0} seconds."
                , sw.elapsed( TimeUnit.SECONDS )
            );
        }
        
        getLogger().log( Level.INFO, "Indexing complete. Committing changes to disk." );
        
        Stopwatch sw = Stopwatch.createStarted();
        writer.commit();
        sw.stop();
        
        getLogger().log( 
            Level.INFO, "Commit finished in {0} seconds."
            , sw.elapsed( TimeUnit.SECONDS )
        );
        
        writer.close();
    }
}
