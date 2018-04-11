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
package edu.columbia.incite.uima.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.AbstractProcessor;
import edu.columbia.incite.util.FileUtils;

/**
 * Base class for a CAS Annotator that writes CASes to disk.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class AbstractFileWriter extends AbstractProcessor {

    /**
     * Directory where CAS files will be saved.
     */
    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter( name = PARAM_OUTPUT_DIR, mandatory = false, defaultValue = "data/cas_output" )
    protected String outputDir;

    /**
     * Create directories if they don't exist.
     */
    public static final String PARAM_MKDIRS = "mkdirs";
    @ConfigurationParameter( name = PARAM_MKDIRS, mandatory = false, defaultValue = "true" )
    protected Boolean mkdirs;

    /**
     * Overwrite existing files.
     */
    public static final String PARAM_OVERWRITE = "overwrite";
    @ConfigurationParameter( name = PARAM_OVERWRITE, mandatory = false, defaultValue = "true" )
    protected Boolean overwrite;

    /**
     * File name pattern.
     */
    public static final String PARAM_FILE_PATTERN = "fileNamePattern";
    @ConfigurationParameter( name = PARAM_FILE_PATTERN, mandatory = false, defaultValue = "%c.%e" )
    protected String fileNamePattern;

    /**
     * Filename extension to use. Default is ".data"
     */
    public static final String PARAM_FILE_EXTENSION = "fileNameExtension";
    // TODO: File name separator hardcoded.
    @ConfigurationParameter( name = PARAM_FILE_EXTENSION, mandatory = false, defaultValue = ".data" )
    protected String ext;
  
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );

        getLogger().log( Level.INFO, "{0} file writer initialized. {1}riting data to files in {2}",
            new Object[] { this.getClass().getSimpleName(), overwrite ? "Overw" : "W", outputDir }
        );
    }

    /**
     * Produce output stream to write CAS data to. The output stream will be created following the
     * configured options (overwrite, create directories, file name pattern, etc.).
     *
     * @param cas CAS to get an output stream for.
     *
     * @return A valid output stream, pointing to an existing file named as per the given CAS's
     *         metadata information.
     *
     * @throws AnalysisEngineProcessException If reading CAS data failed, or an output stream could
     *                                        not be obtained.
     */
    public OutputStream getOutputStreamForCas( CAS cas ) throws AnalysisEngineProcessException {
        String file = getFileNameForCas( cas );
        try {
            return FileUtils.getOutputStream( outputDir, file, mkdirs, overwrite );
        } catch( IOException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }
    
    public OutputStream getOutputStreamForCas( JCas jcas ) throws AnalysisEngineProcessException {
        return getOutputStreamForCas( jcas.getCas() );
    }

    /**
     * Produce writer to write CAS data to. The writer will be created following the configured
     * options (overwrite, create directories, file name pattern, etc.).
     *
     * @param cas CAS to get a writer for.
     *
     * @return A valid writer, pointing to an existing file named as per the given CAS's metadata
     *         information.
     *
     * @throws AnalysisEngineProcessException If reading CAS data failed, or writer could
     *                                        not be obtained.
     */
    public Writer getWriterForCas( CAS cas ) throws AnalysisEngineProcessException {
        String file = getFileNameForCas( cas );
        try {
            return FileUtils.getWriter( outputDir, file, mkdirs, overwrite );
        } catch( IOException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }
    
    public Writer getWriterForCas( JCas jcas ) throws AnalysisEngineProcessException {
        return getWriterForCas( jcas.getCas() );
    }

    private final static AtomicInteger COUNTER = new AtomicInteger();

    /**
     * Get a valid file name for the given CAS. This method will attempt to produce the most
     * informative file name possible from the CAS Document Metadata annotation. If no valid data is
     * found, it will produce a filename that is guaranteed to be unique within the lifetime of this
     * component (i.e. unique within all calls to this method from the currently executing
     * pipeline).
     *
     * @param cas CAS to get a file name for.
     *
     * @return A valid file name for the given CAS.
     *
     * @throws AnalysisEngineProcessException If it was impossible to read CAS data to determine the
     *                                        correct name.
     */
    public String getFileNameForCas( CAS cas ) throws AnalysisEngineProcessException {
        String fileName = getDocumentId();
        fileName = fileNamePattern.replaceAll( "%c", fileName );
        fileName = fileName.replaceAll( "\\.%e", ext );
        return fileName;
    }
    
    public String getFileNameForCas( JCas jcas ) throws AnalysisEngineProcessException {
        return getFileNameForCas( jcas.getCas() );
    }
    
    @Override
    public void collectionProcessComplete() {
        COUNTER.set( 0 );
    }

}
