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
package edu.columbia.incite.uima.ae;

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

import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.uima.api.types.Document;
import edu.columbia.incite.uima.api.types.InciteTypes;
import edu.columbia.incite.uima.res.casio.FeaturePathBroker;
import edu.columbia.incite.uima.res.casio.InciteFeatureBroker;
import edu.columbia.incite.uima.util.Types;
import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.reflex.Resources;
import edu.columbia.incite.util.reflex.annotations.Resource;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public abstract class AbstractEngine extends JCasAnnotator_ImplBase {

    // CAS metadata parameters
    public static final String PARAM_DOCUMENT_TYPE = "dmdTypeName";
    @ConfigurationParameter( name = PARAM_DOCUMENT_TYPE, mandatory = false,
        description = "Typename for document metadata annotations"
        // Java sucks: this is impossible to achieve because java lacks macros, and a reference to 
        // a final static field is not constant enough for f*cking javac.
        // , defaultValue = PARAM_DOCUMENT_TYPE_DFLT
    )
    protected String dmdTypeName;

    public static final String PARAM_DOC_ID_FEATURE = "dmdIdFeatName";
    @ConfigurationParameter( name = PARAM_DOC_ID_FEATURE, mandatory = false,
        description = "Feature name for document id in document metadata annotations" )
    protected String dmdIdFeatName;

    public static final String PARAM_DOCUMENT_FEATURE_PATTERNS = "dmdFeatPatterns";
    @ConfigurationParameter( name = PARAM_DOCUMENT_FEATURE_PATTERNS, mandatory = false,
        description = "Feature name patterns for document metadata features" )
    protected String[] dmdFeatPatterns;

    public static final String RES_DMD_BROKER = "dmdFeatureBroker";
    @ExternalResource( key = RES_DMD_BROKER, mandatory = false,
        description = "Feature broker for document metadata annotations" )
    private FeatureBroker<Datum> dmdBroker;

    protected Type dmdType;
    protected Feature dmdIdF;
    protected boolean customDmd = false;

    @Resource
    private Annotation curCasData;

    private int curCasIndex = 0;
    private TypeSystem ts;

    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        if( dmdBroker == null ) {
            if( dmdTypeName != null ) { // Custom metadata types. Use broker.
                if( dmdIdFeatName == null ) {
                    throw new ResourceInitializationException(
                        ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS,
                        new Object[] { PARAM_DOC_ID_FEATURE }
                    );
                }
                customDmd = true;
                if( dmdFeatPatterns != null ) {
                    dmdBroker = new FeaturePathBroker( dmdFeatPatterns, true );
                }
            } else {
                dmdTypeName = InciteTypes.DOCUMENT_TYPE;
                dmdBroker = new InciteFeatureBroker();
            }
        }
    }

    @Override
    public final void process( JCas jcas ) throws AnalysisEngineProcessException {
        preProcess( jcas );
        realProcess( jcas );
        postProcess( jcas );
    }

    
    protected Datum getMetadata() throws AnalysisEngineProcessException {
        if( curCasData != null ) {
            try {
                return dmdBroker.values( curCasData );
            } catch( CASException ex ) {
                throw new AnalysisEngineProcessException( ex );
            }
        }
        return null;
    }

    protected String getDocumentId() {
        if( curCasData != null ) {
            if( customDmd ) {
                return curCasData.getFeatureValueAsString( dmdIdF );
            } else {
                return ( (Document) curCasData ).getId();
            }
        } else {
            getLogger().log( Level.WARNING, "No metadata for CAS. Using naive ids." );
            return "doc-" + Integer.toString( curCasIndex );
        }
    }

    protected void initTypes( JCas jcas ) throws AnalysisEngineProcessException {
        this.ts = jcas.getTypeSystem();

        if( customDmd ) {
            dmdType = Types.checkType( ts, dmdTypeName );
            dmdIdF = Types.checkFeature( dmdType, dmdIdFeatName );
            if( !dmdIdF.getRange().isPrimitive() ) {
                throw new AnalysisEngineProcessException(
                    new Exception(
                        "The requested feature for document ids has a non-primitive range."
                    )
                );
            }
        } else {
            dmdType = jcas.getCasType( Document.type );
        }

        if( dmdBroker != null ) try {
            dmdBroker.configure( jcas.getCas() );
        } catch( Exception ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }

    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        curCasIndex++;
        if( ts == null || !ts.equals( jcas.getTypeSystem() ) ) initTypes( jcas );
        curCasData = jcas.getAnnotationIndex( dmdType ).iterator().next();
    }

    protected abstract void realProcess( JCas jcas ) throws AnalysisEngineProcessException;

    protected void postProcess( JCas jcas ) {
        Resources.destroyFor( this );
    }
}
