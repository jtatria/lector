/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.tools.corpus;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;

import static org.apache.lucene.index.IndexWriterConfig.DEFAULT_RAM_BUFFER_SIZE_MB;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class WriterProvider extends Resource_ImplBase {
    
    public static final String PARAM_BUILD_IN_RAM = "useRam";
    @ConfigurationParameter( name = PARAM_BUILD_IN_RAM, mandatory = false, defaultValue = "false",
        description = "Use a memory mapped file for index construction."
    )
    private Boolean useRam = false;

    public static final String PARAM_UPDATE_INDEX = "update";
    @ConfigurationParameter( name = PARAM_UPDATE_INDEX, mandatory = false, defaultValue = "false",
        description = "Update an existing index instead of creating a new one."
    )
    private Boolean update = false;

    public static final String PARAM_RAMBUFFER_SIZE = "rbSize";
    @ConfigurationParameter( name = PARAM_RAMBUFFER_SIZE, mandatory = false,
        description = "RAM buffer size."
    )
    private Double rbSize;

    public static final String PARAM_MAX_THREADS = "maxThreads";
    @ConfigurationParameter( name = PARAM_MAX_THREADS, mandatory = false,
        description = "Maximum number of threads for indexing process."
    )
    private Integer maxThreads;
    
    public static final String PARAM_USE_COMPOUND_FILE = "compound";
    @ConfigurationParameter( name = PARAM_USE_COMPOUND_FILE, mandatory = false,
        description = "Write index using Lucene's compound file format."
    )
    private Boolean compound;
    
    public static final String PARAM_OUTPUT_DIR   = "outDir";
    @ConfigurationParameter( name = PARAM_OUTPUT_DIR, mandatory = false,
        description = "Index directory path."
    )
    private String outDir = System.getProperty( "user.dir" ) + "/indexDir";
        
    public static final String PARAM_OPTIMIZE = "optimize";
    private Boolean optimize = true;
    
    public static final String PARAM_MERGE_SEGMENTS = "segments";
    private Integer segments = 1;
    
    public static final String PARAM_FORCE_OPEN = "forceOpen";
    private Boolean forceOpen = true;
    
    private IndexWriter writer;
    
    @Override
    public boolean initialize( ResourceSpecifier spec, Map<String,Object> params )
    throws ResourceInitializationException {
        boolean ret = super.initialize( spec, params );
        initialize();
        return ret;
    }
    
    public void index( Document doc ) throws IOException {
        writer.addDocument( doc );
    }
    
    public void close() throws IOException {
        if( optimize ) {
            getLogger().log( Level.INFO,
                "Index optimization requested. Merging index into {0} segments. This may take a while...",
                new Object[]{ segments }
            );
            writer.forceMerge( segments );
            getLogger().log( Level.INFO, "Index optimization done.");
        }
        writer.close();
    }

    void initialize() throws ResourceInitializationException {
        try {
            
            Path path = Paths.get( outDir );
            if( !Files.isDirectory( path ) ) {
                Files.createDirectories( path );
            }
            
            if( forceOpen ) {
                for( Path p : Files.newDirectoryStream( path ) ) {
                    Files.delete( p );
                }
            }
            
            FSDirectory dir = 
                useRam ? MMapDirectory.open( path ) : NIOFSDirectory.open( path );
            
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
}
