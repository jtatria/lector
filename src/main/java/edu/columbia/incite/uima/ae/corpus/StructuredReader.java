/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.corpus;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.ae.AbstractEngine;
import edu.columbia.incite.uima.util.Types;

/**
 *
 * @author gorgonzola
 */
public abstract class StructuredReader extends AbstractEngine {
    
    public static final String PARAM_DOCUMENT_TYPES = "dTypeName";
    @ConfigurationParameter(
        name = PARAM_DOCUMENT_TYPES, mandatory = true, description = "Document type name",
        defaultValue = "edu.columbia.incite.uima.api.types.Paragraph"
    )
    private String   dTypeName;
    
    public static final String PARAM_TOKEN_TYPES    = "tTypeNames";
    @ConfigurationParameter(
        name = PARAM_TOKEN_TYPES, mandatory = false, description = "Token type names",
//        defaultValue = { CAS.TYPE_NAME_ANNOTATION }
        defaultValue = { 
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
            "edu.columbia.incite.uima.api.types.Span",
        }
    )
    private String[] tTypeNames;

    public static final String PARAM_COVER_TYPES    = "cTypeNames";
    @ConfigurationParameter(
        name = PARAM_COVER_TYPES, mandatory = false, description = "Cover type names",
        defaultValue = { "edu.columbia.incite.uima.api.types.Span" }
    )
    private String[] cTypeNames;
    
    private Set<Type> cTypes;
    private Set<Type> tTypes;
    private Type dType;
    
    protected Map<AnnotationFS,Collection<AnnotationFS>> cIndex;
    protected Map<AnnotationFS,Collection<AnnotationFS>> tIndex;
    
    @Override
    public void initialize( UimaContext uCtx ) throws ResourceInitializationException {
        super.initialize( uCtx );

        if( dTypeName == null || dTypeName.length() == 0 ) {
            throw new ResourceInitializationException(
                ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS,
                new Object[]{ PARAM_DOCUMENT_TYPES } 
            );
        }
        
        getLogger().info( String.format(
            "%s initialized. Processing %s annotations as corpus documents.",
            this.getClass().getSimpleName(), dTypeName
        ) );
        
        if( cTypeNames == null || cTypeNames.length == 0 ) {
            getLogger().info( "No cover types defined: Including all covering annotations." );
            cTypeNames = new String[]{ CAS.TYPE_NAME_ANNOTATION };
        }
        if( tTypeNames == null || tTypeNames.length == 0 ) {
            getLogger().info( "No token types defined: Including all covered annotations." );
            tTypeNames = new String[]{ CAS.TYPE_NAME_ANNOTATION };
        }
    }
    
    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );
        this.dType  = Types.checkType( jcas.getTypeSystem(), dTypeName );
        this.cTypes = new HashSet<>( Arrays.asList( Types.checkTypes( jcas.getTypeSystem(), cTypeNames ) ) );
        this.tTypes = new HashSet<>( Arrays.asList( Types.checkTypes( jcas.getTypeSystem(), tTypeNames ) ) );
        Type base = Types.checkType( jcas.getTypeSystem(), CAS.TYPE_NAME_ANNOTATION );
        this.cIndex = CasUtil.indexCovering( jcas.getCas(), dType, base );
        this.tIndex = CasUtil.indexCovered( jcas.getCas(), dType, base );
    }
    
    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        FSIterator<Annotation> dIt = jcas.getAnnotationIndex( dType ).iterator();
        int ct = 0;
        while( dIt.hasNext() ) {
            ct++;
            Annotation doc = dIt.next();
            Collection<AnnotationFS> covers = Types.filterTypes( cIndex.get( doc ), cTypes );
            Collection<AnnotationFS> tokens = Types.filterTypes( tIndex.get( doc ), tTypes );
            read( doc, covers, tokens );
        }
        getLogger().log( Level.INFO, String.format( "%s read %d documents from CAS %s",
            this.getClass().getSimpleName(), ct, getDocumentId() )
        );
    }
    
    @Override
    protected void postProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.postProcess( jcas );
        cTypes = null;
        tTypes = null;
        cIndex = null;
        tIndex = null;
    }

    protected abstract void read( Annotation doc, Collection<AnnotationFS> covers, Collection<AnnotationFS> tokens );

}
