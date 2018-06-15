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
package edu.columbia.incite.uima;

import java.util.function.Supplier;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.tools.FeatureBroker;
import edu.columbia.incite.uima.types.InciteTypes;
import edu.columbia.incite.uima.tools.FeaturePathBroker;
import edu.columbia.incite.uima.tools.InciteDatumBroker;
import edu.columbia.incite.util.Datum;
import edu.columbia.incite.util.Reflection.CasData;
import edu.columbia.incite.uima.util.Types;
import edu.columbia.incite.util.Reflection;

/**
 * Base class for all UIMA CAS processors in this package.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class AbstractProcessor extends JCasAnnotator_ImplBase {

    public static final String DEFAULT_DOC_MD_TYPE_NAME = InciteTypes.DOCUMENT_TYPE;
    public static final String DEFAULT_DOC_ID_FEAT_NAME = InciteTypes.DOC_ID_FEATURE;
    public static final Supplier<FeatureBroker<Datum>> DEFAULT_FEAT_BROKER = InciteDatumBroker::new;
    
    // CAS metadata parameters
    public static final String PARAM_CUSTOM_DOC_TYPE = "documentMetadataTypeName";
    @ConfigurationParameter( name = PARAM_CUSTOM_DOC_TYPE, mandatory = false,
        description = "Typename for document metadata annotations"
    )
    protected String docMDTypeName;

    public static final String PARAM_CUSTOM_ID_FEATURE = "documentIdFeatureName";
    @ConfigurationParameter( name = PARAM_CUSTOM_ID_FEATURE, mandatory = false,
        description = "Feature name for document id in document metadata annotations" )
    protected String docIdFeatName;

    public static final String PARAM_CUSTOM_METADATA_PATHS = "documentMetadataFeaturePaths";
    @ConfigurationParameter( name = PARAM_CUSTOM_METADATA_PATHS, mandatory = false,
        description = "Feature name patterns for document metadata features" )
    protected String[] docMDFeatPaths;

    public static final String RES_CUSTOM_METADATA_BROKER = "documentMetadataFeatureBroker";
    @ExternalResource( key = RES_CUSTOM_METADATA_BROKER, mandatory = false,
        description = "Feature broker for document metadata annotations" )
    private FeatureBroker<Datum> docMDFeatBroker;

    protected Type dmdType;
    protected Feature dmdIdF;
//    protected boolean customDmd = false;

    @CasData
    private Annotation curCasData;

    private int curCasIndex = 0;
    private TypeSystem ts;

    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
                        
        if( docMDTypeName == null ) {
            getLogger().log( Level.CONFIG, "No type name specified for document metadata. Using Incite Types." );
            docMDTypeName = DEFAULT_DOC_MD_TYPE_NAME;
        }
        
        if( docIdFeatName == null ) {
            getLogger().log( Level.CONFIG, "No feature name specified for document ids. Using Incite features." );
            docIdFeatName = DEFAULT_DOC_ID_FEAT_NAME;
        }
        
        if( docMDFeatBroker == null ) {
            String msg = "No feature broker configured for document metadata";
            if( docMDFeatPaths != null ) {
                getLogger().log( Level.CONFIG, msg + ". Building from given feature paths.");
                docMDFeatBroker = new FeaturePathBroker( docMDFeatPaths, true  );
            } else {
                getLogger().log( Level.CONFIG, msg + " and no feature paths given. Using default feature broker." );
                docMDFeatBroker = DEFAULT_FEAT_BROKER.get();
            }
        }
    }

    /**
     * UIMA's {@link JCasAnnotator_ImplBase#process(org.apache.uima.jcas.JCas)} method 
     * implementation, that splits analysis logic in three distinct phases, executed in order: 
     * {@link #preProcess(org.apache.uima.jcas.JCas)}, which executes all pre-analysis logic, 
     * {@link #realProcess(org.apache.uima.jcas.JCas)}, which executes the actual analysis logic 
     * and should be overriden by implementations (it does nothing by default), and 
     * {@link #postProcess(org.apache.uima.jcas.JCas)}, which carries out all post-analysis actions.
     * 
     * See the documentation for each method for more details.
     * 
     * @param jcas
     * @throws AnalysisEngineProcessException 
     */
    @Override
    public final void process( JCas jcas ) throws AnalysisEngineProcessException {
        preProcess( jcas );
        realProcess( jcas );
        postProcess( jcas );
    }

    /**
     * Obtain a {@link Datum} instance containing all available metadata for the document currently 
     * under analysis, as produced by the configured {@link FeatureBroker}.
     * @return  A {@link Datum} with the current document's metadata.
     * @throws AnalysisEngineProcessException 
     */
    protected Datum getMetadata() throws AnalysisEngineProcessException {
        if( curCasData != null ) {
            try {
                return docMDFeatBroker.values( curCasData );
            } catch( CASException ex ) {
                throw new AnalysisEngineProcessException( ex );
            }
        }
        return null;
    }

    /**
     * Get a collection-wide unique identifier for the document contained in the current CAS.
     * @return A string unambigously identifying the document currently under analysis.
     */
    protected String getDocumentId() {
        if( curCasData != null ) {
            return curCasData.getFeatureValueAsString( dmdIdF );
        } else {
            getLogger().log( Level.WARNING, "No metadata for CAS. Using naive ids." );
            return "doc-" + Integer.toString( curCasIndex );
        }
    }

    protected void initTypes( JCas jcas ) throws AnalysisEngineProcessException {
        this.ts = jcas.getTypeSystem();

        dmdType = Types.checkType( ts, docMDTypeName );
        dmdIdF = Types.checkFeature( dmdType, docIdFeatName );
        if( !dmdIdF.getRange().isPrimitive() ) {
            throw new AnalysisEngineProcessException(
                new Exception( "Non-primitive range for document id feature." )
            );
        }
        
        if( docMDFeatBroker != null ) try {
            docMDFeatBroker.configure( jcas.getCas() );
        } catch( Exception ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }

    /**
     * Perform all pre-analysis actions on the current JCas. These typically include indexing 
     * annotations, gathering metadata, configuring resources, opening I/O channels, etc.
     * 
     * Overriding implementations must call this method if they want CAS metadata to be available 
     * through this class's methods.
     * 
     * @param jcas
     * @throws AnalysisEngineProcessException 
     */
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        curCasIndex++;
        if( ts == null || !ts.equals( jcas.getTypeSystem() ) ) initTypes( jcas );
        curCasData = jcas.getAnnotationIndex( dmdType ).iterator().next();
    }

    /**
     * Execute the actual analysis logic on the current JCas.
     * 
     * @param jcas
     * @throws AnalysisEngineProcessException 
     */
    protected abstract void realProcess( JCas jcas ) throws AnalysisEngineProcessException;

    /**
     * Perform all post-analysis actions for the current JCas. This typically includes deleting 
     * annotation indexes, clearing retained metadata, resetting resource configurations, close 
     * per-document I/O channels, etc.
     * 
     * Overriding implementations must call this method if they are using this class's metadata 
     * access methods, i.e. if they called preProcess() from an overriding implementation.
     * 
     * @param jcas
     * @throws AnalysisEngineProcessException 
     */
    protected void postProcess( JCas jcas ) throws AnalysisEngineProcessException {
        Reflection.destroyFor( this );
    }
    
    
}
