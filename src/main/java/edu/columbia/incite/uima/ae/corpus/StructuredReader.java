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
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import edu.columbia.incite.uima.ae.AbstractEngine;
import edu.columbia.incite.uima.util.Types;

/**
 *
 * @author gorgonzola
 */
public abstract class StructuredReader extends AbstractEngine {
    
    public static final String PARAM_COVER_TYPES    = "cTypeNames";
    public static final String PARAM_TOKEN_TYPES    = "tTypeNames";
    public static final String PARAM_DOCUMENT_TYPES = "dTypeName";
        
    private String[] cTypeNames;
    private String   dTypeName;
    private String[] tTypeNames;
    
    private Set<Type> cTypes;
    private Set<Type> tTypes;
    private Type dType;
    
    private Map<AnnotationFS,Collection<AnnotationFS>> cMap;
    private Map<AnnotationFS,Collection<AnnotationFS>> tMap;
    
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
        }
        if( tTypeNames == null || tTypeNames.length == 0 ) {
            getLogger().info( "No token types defined: Including all covered annotations." );
        }
    }
    
    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        this.dType  = Types.checkType( jcas.getTypeSystem(), dTypeName );
        this.cTypes = new HashSet<>( Arrays.asList( Types.checkTypes( jcas.getTypeSystem(), cTypeNames ) ) );
        this.tTypes = new HashSet<>( Arrays.asList( Types.checkTypes( jcas.getTypeSystem(), tTypeNames ) ) );
        Type base = Types.checkType( jcas.getTypeSystem(), CAS.TYPE_NAME_ANNOTATION );
        this.cMap = CasUtil.indexCovering( jcas.getCas(), dType, base );
        this.tMap = CasUtil.indexCovered( jcas.getCas(), dType, base );
    }
    
    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        FSIterator<Annotation> dIt = jcas.getAnnotationIndex( dType ).iterator();
        while( dIt.hasNext() ) {
            Annotation doc = dIt.next();
            Collection<AnnotationFS> covers = Types.filterTypes( cMap.get( doc ), cTypes );
            Collection<AnnotationFS> tokens = Types.filterTypes( tMap.get( doc ), tTypes );
            read( doc, covers, tokens );
        }
    }

    protected abstract void read( Annotation doc, Collection<AnnotationFS> covers, Collection<AnnotationFS> tokens );

}
