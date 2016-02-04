/* 
 * Copyright (C) 2015 Jose Tomas Atria <jtatria@gmail.com>
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
package edu.columbia.incite.uima.io.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.casio.TypeProvider;
import edu.columbia.incite.uima.res.casio.InciteTypeProvider;
import edu.columbia.incite.uima.util.TypeSystems;
import edu.columbia.incite.util.io.FileUtils;

/**
 * Base class for a CAS Annotator that writes CASes to disk.
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public abstract class AbstractFileWriter extends JCasAnnotator_ImplBase {

    /**
     * Directory where CAS files will be saved.
     */
    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter( name = PARAM_OUTPUT_DIR, mandatory = true )
    protected String outputDir;

    /**
     * File name pattern.
     */
    public static final String PARAM_FILE_PATTERN = "fileNamePattern";
    @ConfigurationParameter( name = PARAM_FILE_PATTERN, mandatory = false, defaultValue = "%c.%e" )
    protected String fileNamePattern;

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
     * Filename extension to use. Default is ".data"
     */
    public static final String PARAM_FILE_EXTENSION = "fileNameExtension";
    @ConfigurationParameter( name = PARAM_FILE_EXTENSION, mandatory = false, defaultValue = ".data" )
    protected String ext;

    /**
     * Type name for CAS Document Metadata. If provided, this will be used to determine file names,
     * as per the configured file name pattern.
     */
    public final static String PARAM_DMD_TYPE = "dmdTypeName";
    @ConfigurationParameter( name = PARAM_DMD_TYPE, mandatory = false )
    protected String dmdTypeName;

    /**
     * Feature name for a feature in the Document Metadata annotation that contains the file name.
     */
    public final static String PARAM_FILENAME_FEAT = "fileNameFeature";
    @ConfigurationParameter( name = PARAM_FILENAME_FEAT, mandatory = false )
    protected String fileNameFeat;

    /**
     * Resource implementing TypeProvider to access Document Metadata if no type or feature is
     * specified.
     */
    public final static String RES_NAME_PROVIDER = "nameProvider";
    @ExternalResource(
         key = RES_NAME_PROVIDER, api = TypeProvider.class, mandatory = false,
         description = "Name provider for metadata type and features. Mandatory if no metadata "
         + "type and feature names are provided."
    )
    private TypeProvider nameProvider;

    private boolean useNameProvider = true;

    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );

        getLogger().log( Level.INFO, "{0} file CAS writer initialized. {1}riting CAS files to {2}",
            new Object[] { this.getClass().getSimpleName(), overwrite ? "Overw" : "W", outputDir }
        );

        if( dmdTypeName == null || fileNameFeat == null ) {
            String msg = "No features provided for filenaming; ";
            if( nameProvider == null ) {
                getLogger().log( Level.INFO, msg + "Using Incite name provider." );
                nameProvider = new InciteTypeProvider();
            } else {
                getLogger().log( Level.INFO, msg + "Using configured name provider {0}",
                    new Object[] { nameProvider.getMetaData().getName() }
                );
            }
        } else {
            useNameProvider = false;
        }

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

    /**
     * Produce writer to write CAS data to. The writer will be created following the configured
     * options (overwrite, create directories, file name pattern, etc.).
     *
     * @param cas CAS to get a writer for.
     *
     * @return A valid writer, pointing to an existing file named as per the given CAS's metadata
     *         information.
     *
     * @throws AnalysisEngineProcessException If reading CAS data failed, or an output stream could
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

    private final static AtomicInteger counter = new AtomicInteger();

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
        String fileName = null;
        if( useNameProvider ) {
            try {
                nameProvider.configure( cas );
            } catch( Exception ex ) {
                throw new AnalysisEngineProcessException( ex );
            }
            fileName = nameProvider.getCasId( cas );
        } else {
            Type dmdType = TypeSystems.checkType( cas.getTypeSystem(), dmdTypeName );
            Feature docIdF = TypeSystems.checkFeature( dmdType, fileNameFeat );
            AnnotationFS dmd = cas.getAnnotationIndex( dmdType ).iterator().next();
            if( dmd != null ) {
                fileName = dmd.getFeatureValueAsString( docIdF );
            }
        }
        if( fileName == null ) {
            getLogger().log( Level.INFO, "No annotation found for document metadata. Using naive file names." );
            fileName = "cas" + counter.getAndIncrement();
        }
        fileName = fileNamePattern.replaceAll( "%c", fileName );
        fileName = fileName.replaceAll( "\\.%e", ext );
        return fileName;
    }

    protected String getPattern() {
        return this.fileNamePattern;
    }

    protected void setPattern( String pattern ) {
        this.fileNamePattern = pattern;
    }

    protected String getExtension() {
        return this.ext;
    }

    protected void setExtension( String ext ) {
        this.ext = ext;
    }

    protected String getDir() {
        return this.outputDir;
    }

    protected boolean getMakeDirs() {
        return this.mkdirs;
    }

    protected boolean getOverwrite() {
        return this.overwrite;
    }

}
