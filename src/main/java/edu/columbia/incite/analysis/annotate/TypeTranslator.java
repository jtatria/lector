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
package edu.columbia.incite.analysis.annotate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.CasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.util.Level;

import edu.columbia.incite.util.Types;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class TypeTranslator extends CasAnnotator_ImplBase {

    public static final String PARAM_SOURCE_TYPE = "srcTypeName";
    @ConfigurationParameter( name = PARAM_SOURCE_TYPE, mandatory = true
        , description = "Type of annotations to copy from." )
    private String srcTypeName;
    
    public static final String PARAM_TARGET_TYPE = "tgtTypeName";
    @ConfigurationParameter( name = PARAM_TARGET_TYPE, mandatory = true
        , description = "Type of annotations to copy to." )
    private String tgtTypeName;
    
    public static final String PARAM_KEEP_SOURCE = "keepSource";
    @ConfigurationParameter( name = PARAM_KEEP_SOURCE, mandatory = false, defaultValue = "true"
        , description = "Retain source annotations." )
    private Boolean keepSource;
    
    public static final String PARAM_OVERWRITE = "overwrite";
    @ConfigurationParameter( name = PARAM_OVERWRITE, mandatory = false, defaultValue = "false"
        , description = "Overwrite annotations of the target type. This will remove all existing "
            + "annotations of this type." )
    private Boolean overwrite;
    
    private Map<String,Set<String>> missing = new HashMap<>();
    private Map<String,Set<String>> mismatch = new HashMap<>();
    private FeatureMap fMap = new FeatureMap();
        
    @Override
    public void process( CAS cas ) throws AnalysisEngineProcessException {
        Type srcType = Types.checkType( cas.getTypeSystem(), srcTypeName );
        Type tgtType = Types.checkType( cas.getTypeSystem(), tgtTypeName );
        
        if( overwrite ) {
            List<AnnotationFS> removes = new ArrayList<>();
            for( AnnotationFS oldTgtAnn : cas.getAnnotationIndex( tgtType ) ) {
                removes.add( oldTgtAnn );
            }
            for( AnnotationFS remove : removes ) {
                cas.removeFsFromIndexes( remove );
            }
        }
        
        for( AnnotationFS srcAnn : cas.getAnnotationIndex( srcType ) ) {
            AnnotationFS tgtAnn = cas.createAnnotation( tgtType, srcAnn.getBegin(), srcAnn.getEnd() );
            for( Feature srcFeat : srcType.getFeatures() ) {
                Feature tgtFeat = fMap.getTargetFeature( tgtType, srcFeat );
                
                if( tgtFeat == null ) {
                    cryMissing( tgtType.getShortName(), srcFeat.getShortName() );
                    continue;
                }
                
                if( !srcFeat.getRange().equals( tgtFeat.getRange() )) {
                    cryMismatch( srcFeat.getShortName(), tgtFeat.getShortName() );
                    continue;
                }
                
                if( tgtFeat.getRange().isPrimitive() && srcFeat.getRange().isPrimitive() ) {
                    tgtAnn.setFeatureValueFromString( 
                        tgtFeat, srcAnn.getFeatureValueAsString( srcFeat )
                    );
                } else {
                    tgtAnn.setFeatureValue( tgtFeat, srcAnn.getFeatureValue( srcFeat ) );
                }                
            }
            if( !keepSource ) cas.removeFsFromIndexes( srcAnn );
            cas.addFsToIndexes( tgtAnn );
        }
    }    

    private void cryMissing( String tgtTypeName, String missingFeatName ) {
        if( !alreadyLogged( tgtTypeName, missingFeatName, missing ) ) {
            getLogger().log( Level.WARNING, "Missing feature {0} in target type {1}",
                new Object[]{ missingFeatName, tgtTypeName }
            );
        }
    }

    private void cryMismatch( String srcFeatName, String tgtFeatName ) {
        if( !alreadyLogged( srcFeatName, tgtFeatName, mismatch ) ) {
            getLogger().log( Level.WARNING, "Type mismatch: Source feature {0} and target feature "
                + "{1} have different range.", new Object[]{ srcFeatName, tgtFeatName } );
        }
    }
    
    private boolean alreadyLogged( String key, String value, Map<String,Set<String>> map ) {
        if( map.get( key ) != null ) {
            if( map.get( key ).contains( value ) ) {
                return true;
            } else {
                map.get( key ).add( value );
            }
        } else {
            Set<String> set = new HashSet<>();
            set.add( value );
            map.put( key, set );
        }
        return false;
    }

    public class FeatureMap {
    
        private Map<String,String> map = new HashMap<>();

        public FeatureMap() {
            map.put( "id", "documentId" );
            map.put( "uri", "documentUri" );
            map.put( "collection", "collectionId" );
            map.put( "proc_isLast", "isLastSegment" );
        }

        public Feature getTargetFeature( Type tgtType, Feature srcFeat ) {
            String tgtFeatName = getTargetFeatureName( srcFeat.getShortName() );
            Feature tgt = tgtType.getFeatureByBaseName( tgtFeatName );
            if( tgt != null ) return tgt;
            return null;
        }

        private String getTargetFeatureName( String name ) {
            if( map.containsKey( name ) ) return map.get( name );
            else return name;
        }

    }
    
}
