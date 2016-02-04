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
package edu.columbia.incite.uima.ae.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.corpus.Indexer;
import edu.columbia.incite.uima.ae.AbstractEngine;
import edu.columbia.incite.uima.util.TypeSystems;
import edu.columbia.incite.util.flags.FlagSet;
import edu.columbia.incite.util.reflex.annotations.NullOnRelease;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class CASIndexer<D> extends AbstractEngine {

    public static final String PARAM_CTX_TYPENAME = "docTypeName";
    @ConfigurationParameter( name = PARAM_CTX_TYPENAME, mandatory = true,
        description = "Typename for indexing context annotations" )
    private String docTypeName;

    public static final String PARAM_TOKEN_TYPENAMES = "tokenTypeNames";
    @ConfigurationParameter( name = PARAM_TOKEN_TYPENAMES, mandatory = false )
    private String[] tokenTypeNames;
    
    public static final String PARAM_TYPEFIELD_MAP = "typeFieldMap";
    @ConfigurationParameter( name = PARAM_TYPEFIELD_MAP, mandatory = false )
    private String[] typeFieldMap;

    public static final String PARAM_USE_COVERS = "useCovers";
    @ConfigurationParameter( 
        name = PARAM_USE_COVERS, mandatory = false, defaultValue = "false",
        description = "Include features from covering annotations as document metadata"
    )
    private Boolean useCovers;

    public static final String PARAM_COVER_TYPENAME = "coverTypeNames";
    @ConfigurationParameter( name = PARAM_COVER_TYPENAME, mandatory = false,
        description = "Typename for cover annotation to include as metadata" )
    private String[] coverTypeNames;
    
    public static final String RES_INDEXER = "indexer";
    @ExternalResource( key = RES_INDEXER, mandatory = true,
        description = "Index writing intrerface object" )
    private Indexer<D> indexer;

    @NullOnRelease
    private Map<AnnotationFS,Collection<AnnotationFS>> tokenIndex;

    @NullOnRelease
    private Map<AnnotationFS,Collection<AnnotationFS>> coverIndex;
    
    @NullOnRelease
    private Set<Type> tokenTypes;
    
    @NullOnRelease
    private Set<Type> coverTypes;

    private Map<String,String> typeMap = new HashMap<>();
    
    private Long token;

    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        // TODO: Add option to default to filed-per-type whe no typemap defined.
        super.initialize( ctx );
        
        token = indexer.openSession();
        
        FlagSet flags = new FlagSet( tokenTypeNames != null, typeFieldMap != null );
        // TODO: replace by flags.toInt()
        switch( (int) flags.toLong() ) {
            case 0: { // no paramter.
                throw new ResourceInitializationException( new IllegalArgumentException(
                    String.format( "At least one of %s or %s parameters must be set.",
                        PARAM_TOKEN_TYPENAMES, PARAM_TYPEFIELD_MAP
                    ) )
                );
            }
            case 1: { // list only.
                for( String type : tokenTypeNames ) {
                    typeMap.put( type, Indexer.DEFAULT_FIELD );
                }
                break;
            }
            case 2: { 
                if( typeFieldMap.length % 2 != 0 ) {
                    throw new ResourceInitializationException( new IllegalArgumentException(
                        String.format( "Odd number of elements found in %s value.",
                            PARAM_TYPEFIELD_MAP
                        ) )
                    );
                }
                List<String> types = new ArrayList<>();
                for( int i = 0, j = i + 1; j < typeFieldMap.length; i += 2, j = i + 1 ) {
                    typeMap.put( typeFieldMap[i], typeFieldMap[j] );
                    types.add( typeFieldMap[i] );
                }
                tokenTypeNames = types.toArray( new String[types.size()] );
            }
            case 3: {
                if( typeFieldMap.length % 2 != 0 ) {
                    throw new ResourceInitializationException( new IllegalArgumentException(
                        String.format( "Odd number of elements found in %s value.",
                            PARAM_TYPEFIELD_MAP
                        ) )
                    );
                }
                for( int i = 0, j = i + 1; j < typeFieldMap.length; i += 2, j = i + 1 ) {
                    typeMap.put( typeFieldMap[i], typeFieldMap[j] );
                }
                for( String type : tokenTypeNames ) {
                    if( !typeMap.containsKey( type ) ) {
                        typeMap.put( type, Indexer.DEFAULT_FIELD );
                    }
                }
            }
        }
        
        getLogger().log( Level.INFO, 
            "CASIndexer initialized. Using {0} as context annotations and {1} as tokens."
            , new Object[]{ docTypeName, typeMap.keySet().toString() }
        );
    }
    
    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );

        // check for types.
        Type docType = TypeSystems.checkType( jcas.getTypeSystem(), docTypeName );
        Type annType = TypeSystems.checkType( jcas.getTypeSystem(), CAS.TYPE_NAME_ANNOTATION );
        this.tokenTypes = checkTypes( jcas, typeMap.keySet() );
        
        tokenIndex = CasUtil.indexCovered( jcas.getCas(), docType, annType );
        
        if( useCovers ) {
            coverIndex = CasUtil.indexCovering( jcas.getCas(), docType, annType );
            this.coverTypes = checkTypes( jcas, Arrays.asList( coverTypeNames ) );
        }
        
        getLogger().log( Level.INFO, "Indexing {0}. Found {1} context annotations."
            , new Object[]{ getDocumentId(), Integer.toString( tokenIndex.keySet().size() ) } 
        );
    }

    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        try {
            indexer.configure( jcas.getCas() );
        } catch( Exception ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
        
        int ctxs = 0;
        for( AnnotationFS ctx : tokenIndex.keySet() ) {
            ctxs++;
            try {
                D doc = indexer.createDocument( ctx );
                indexer.addMetadata( doc, getCovers( ctx ) );
                indexer.makeTokens( doc, getTokens( ctx ), ctx.getBegin() );
                indexer.index( doc );
            } catch( Indexer.DocumentCreationException ex ) {
                String msg = String.format( "Document creation failed for context %s in CAS %s: %s",
                    ctxs, getDocumentId(), ex.getMessage()
                );
                getLogger().log( Level.WARNING, msg );
            } catch( Indexer.DocumentMetadataException ex ) {
                String msg = String.format( "Metadata extraction failed for context %s in CAS %s: %s",
                    ctxs, getDocumentId(), ex.getMessage()
                );
                getLogger().log( Level.WARNING, msg );
            } catch( Indexer.TokenStreamException ex ) {
                String msg = String.format( "Token creation failed for context %s in CAS %s: %s",
                    ctxs, getDocumentId(), ex.getMessage()
                );
                getLogger().log( Level.WARNING, msg );
            } catch( Indexer.IndexingException ex ) {
                String msg = String.format( "Indexing failed for context %s in CAS %s: %s",
                    ctxs, getDocumentId(), ex.getMessage()
                );
                getLogger().log( Level.WARNING, msg );
            } catch( IOException ex ) {
                throw new AnalysisEngineProcessException( ex );
            }
        }
    }

    @Override
    public void collectionProcessComplete() {
        indexer.closeSession( token );
    }

    private Collection<AnnotationFS> getCovers( AnnotationFS docAnn ) {
        if( coverIndex == null ) return null;
        List<AnnotationFS> covers = new ArrayList<>( filterTypes( coverIndex.get( docAnn ), coverTypes ) );
        return covers;
    }

    private Map<String,List<AnnotationFS>> getTokens( AnnotationFS docAnn ) {
        if( tokenIndex == null ) return null;
        return sortTypes( filterTypes( tokenIndex.get( docAnn ), tokenTypes ), typeMap );
    }
    
    private Collection<AnnotationFS> filterTypes( Collection<AnnotationFS> input, Set<Type> types ) {
        if( types.isEmpty() ) return input;
        List<AnnotationFS> ret = new ArrayList<>();
        for( AnnotationFS ann : input ) {
            Type type = ann.getType();
            TypeSystem ts = ann.getView().getTypeSystem();
            while( type != null ) {
                if( types.contains( type ) ) {
                    ret.add( ann );
                    break;
                }
                type = ts.getParent( type );
            }
        }
        return ret;
    }

    private Map<String,List<AnnotationFS>> sortTypes( Collection<AnnotationFS> anns, Map<String,String> typeMap ) {
        Map<String,List<AnnotationFS>> ret = new HashMap<>();
        for( AnnotationFS ann : anns ) {
            TypeSystem ts = ann.getView().getTypeSystem();
            
            Type type = ann.getType();
            while( ts.getParent( type ) != null && !typeMap.containsKey( type.getName() ) ) {
                type = ts.getParent( type );
            }
            
            if( !ret.containsKey( typeMap.get( type.getName() ) ) ) {
                ret.put( typeMap.get( type.getName() ), new ArrayList<>() );
            }
            ret.get( typeMap.get( type.getName() ) ).add( ann );
        }
        return ret;
    }

    private Set<Type> checkTypes( JCas jcas, Collection<String> typeNames ) throws AnalysisEngineProcessException {
        Set<Type> ret = new HashSet<>();
        for( String typeName : typeNames ) {
            ret.add( TypeSystems.checkType( jcas.getTypeSystem(), typeName ) );
        }
        return ret;
    }

}
