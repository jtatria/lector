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
package edu.columbia.incite.io.readers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import edu.columbia.incite.io.SerializationData;
import edu.columbia.incite.util.FileUtils;
import edu.columbia.incite.util.CollectionTools;

/**
 * Base class for CollectionReaders that acquire collection data from files on disk.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class AbstractFileReader extends JCasCollectionReader_ImplBase { // TODO: refactor as multiplier

    public static final String PARAM_STOP_AFTER_FIRST = "stopAfter";
    @ConfigurationParameter( name = PARAM_STOP_AFTER_FIRST, mandatory = false, defaultValue = "-1" )
    protected Integer stopAfter;
    
    public static final String PARAM_RUN_ON_SAMPLE = "runOnSample";
    @ConfigurationParameter( name = PARAM_RUN_ON_SAMPLE, mandatory = false, defaultValue = "-1" )
    protected Integer runOnSample;
    
    /**
     * Location of a file system directory containing collection files.
     */
    public final static String PARAM_INPUT_DIR = "inputDir";
    @ConfigurationParameter( name = PARAM_INPUT_DIR, mandatory = false, defaultValue = "data/cas_input"
        , description = "Directory containing input files." )
    protected String inputDir;

    /**
     * Collection name. This value will be written to each document's metadata annotation if the
     * type system supports it.
     */
    public final static String PARAM_COLLECTION_NAME = "collection";
    @ConfigurationParameter( name = PARAM_COLLECTION_NAME, mandatory = false, defaultValue = ""
        , description = "Collection name." )
    protected String collectionName;

    /**
     * Read files from the input directory recursively.
     */
    public final static String PARAM_RECURSIVE = "recursive";
    @ConfigurationParameter( name = PARAM_RECURSIVE, mandatory = false, defaultValue = "false"
        , description = "Read files from input directory recursively." )
    protected Boolean recursive;

    /**
     * Glob pattern for the creation of an input directory file stream.
     */
    public final static String PARAM_FILENAME_GLOB = "fileNameGlob";
    @ConfigurationParameter( name = PARAM_FILENAME_GLOB, mandatory = false
        , description = "Filename glob to filter input files."
    )
    protected String fileGlob;

    /**
     * Attempt to add delta files.
     */
    public static final String PARAM_ADD_DELTAS = "addDeltas";
    @ConfigurationParameter( name = PARAM_ADD_DELTAS, mandatory = false, defaultValue = "false"
        , description = "Add delta files if found in the input directory" )
    protected Boolean addDeltas;

    /**
     * Pattern for delta file locations, relative to the input directory.
     */
    public static final String PARAM_DELTA_PATTERN = "deltaPattern";
    @ConfigurationParameter( name = PARAM_DELTA_PATTERN, mandatory = false, defaultValue = "../%i/%c.%e"
        , description = "Filename pattern for delta files." )
    protected String deltaPattern;

    protected Path inputDirPath;
    protected List<Path> paths;
    protected Iterator<Path> pathsIt;
    protected Path curPath;
    protected SerializationData serData;
    
    protected int totalFiles;
    protected int readFiles;

    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );

        inputDirPath = Paths.get( inputDir );
        
        if( !Files.isDirectory( inputDirPath ) ) {
            throw new ResourceInitializationException(
                ResourceConfigurationException.DIRECTORY_NOT_FOUND,
                new Object[] { PARAM_INPUT_DIR, this.getMetaData().getName(), inputDir }
            );
        }

        if( addDeltas && deltaPattern == null ) {
            throw new ResourceInitializationException(
                ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS,
                new Object[] { PARAM_DELTA_PATTERN }
            );
        }

        try {
            paths = FileUtils.listPaths( inputDirPath, fileGlob, recursive );
            if( runOnSample > 0 ) {
                paths = CollectionTools.sample( paths, runOnSample );
            }
        } catch( IOException ex ) {
            throw new ResourceInitializationException( ex );
        }

        totalFiles = paths.size();
        pathsIt = paths.iterator();
        if( stopAfter > 0 ) {
            pathsIt = paths.subList( 0, stopAfter ).iterator();
        }

        serData = SerializationData.getInstance();

        getLogger().log( Level.INFO, "{0} file reader initialized. Reading {1} files {2}from {3}",
            new Object[] {
                this.getClass().getSimpleName(), Integer.toString( paths.size() ), recursive
                    ? "recursively " : "", inputDir
            }
        );
    }

    @Override
    public void getNext( JCas jcas ) throws CollectionException {
        curPath = pathsIt.next();

        getLogger().log( Level.INFO, "Reading CAS data from {0}", curPath.toString() );
        
        try {
            addDataFromFile( jcas, new FileInputStream( curPath.toFile() ) );
        } catch( FileNotFoundException ex ) {
            throw new CollectionException( ex );
        }

        if( addDeltas ) {
            List<Path> deltas = findDeltas();

            if( deltas != null && deltas.size() > 0 ) getLogger().log(
                Level.INFO, "Adding {0} delta files to CAS.", Integer.toString( deltas.size() )
            );

            for( Path delta : deltas ) {
                try {
                    addDataFromFile( jcas, new FileInputStream( delta.toFile() ) );
                } catch( FileNotFoundException ex ) {
                    throw new CollectionException( ex );
                }
            }
        }

        serData.saveMarker( jcas, jcas.getCas().createMarker() );
        readFiles++;
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return pathsIt.hasNext();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[] {
            new ProgressImpl( readFiles, totalFiles, Progress.ENTITIES )
        };
    }

    protected List<Path> findDeltas() {
        List<Path> deltas = new ArrayList<>();
        int i = 0;
        Path delta = makeDeltaPath( curPath, i );
        while( Files.exists( delta ) ) {
            deltas.add( delta );
            delta = makeDeltaPath( curPath, ++i );
        }
        return deltas;
    }

    private Path makeDeltaPath( Path path, int i ) {
        String dir = path.getParent().toString();
        String baseName = FilenameUtils.getBaseName( path.getFileName().toString() );
        String extension = FilenameUtils.getExtension( path.getFileName().toString() );
        String delta = deltaPattern
            .replaceAll( "%c", baseName )
            .replaceAll( "%e", extension )
            .replaceAll( "%i", Integer.toString( i ) );
        return Paths.get( dir, delta );
    }

    protected abstract void addDataFromFile( JCas jcas, FileInputStream fileInputStream ) throws CollectionException;

}
