/* 
 * Copyright (C) 2015 José Tomás Atria <ja2612@columbia.edu>
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
package edu.columbia.incite.uima.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public abstract class TypeSystems {

    public static Type findCommonParentType( TypeSystem ts, Type k, Type j ) {
        List<Type> kp = getTypeHierarchy( ts, k );
        List<Type> jp = getTypeHierarchy( ts, j );

        int iter = jp.size() < kp.size() ? jp.size() : kp.size();
        for( int i = 0; i < iter; i++ ) {
            if( kp.get( i ) == jp.get( i ) ) continue;
            return kp.get(  i - 1 );
        }

        return null;
    }

    public static Type[] checkTypes( TypeSystem ts, String... typeNames ) throws AnalysisEngineProcessException {
        List<Type> types = new ArrayList<>();
        for( String typeName : typeNames ) {
            types.add( checkType( ts, typeName ) );
        }
        return (Type[]) types.toArray();
    }
    
    public static Type[] checkTypes( TypeSystem ts, int... typeCodes ) throws AnalysisEngineProcessException {
        List<Type> types = new ArrayList<>();
        for( int typeCode : typeCodes ) {
            types.add( checkType( ts, typeCode ) );
        }
        return (Type[]) types.toArray();
    }

    public static Type checkType( TypeSystem ts, String typeName ) throws AnalysisEngineProcessException {
        Type type = ts.getType( typeName );
        if( type == null ) {
            throw new AnalysisEngineProcessException(
                AnalysisEngineProcessException.REQUIRED_FEATURE_STRUCTURE_MISSING_FROM_CAS,
                new Object[] { typeName }
            );
        }
        return type;
    }
    
    public static Type checkType( TypeSystem ts, int typeCode ) throws AnalysisEngineProcessException {
        Type type = ts.getLowLevelTypeSystem().ll_getTypeForCode( typeCode );
        if( type == null ) {
            throw new AnalysisEngineProcessException(
                AnalysisEngineProcessException.REQUIRED_FEATURE_STRUCTURE_MISSING_FROM_CAS,
                new Object[] { "index " + typeCode }
            );
        }
        return type;
    }

    public static Feature[] checkFeatures( Type type, String... featureNames ) throws CASRuntimeException {
        List<Feature> features = new ArrayList<>();
        for( String featureName : featureNames ) {
            features.add( checkFeature( type, featureName) );
        }
        return (Feature[]) features.toArray();
    }

    public static Feature checkFeature( Type type, String featureName ) throws CASRuntimeException {
        Feature feature = type.getFeatureByBaseName( featureName );
        if( feature == null ) {
                throw new CASRuntimeException(
                    CASRuntimeException.INAPPROP_FEAT,
                    new Object[] { featureName, type.getName() }
                );
        }
        return feature;
    }

    public static List<Type> getTypeHierarchy( TypeSystem ts, Type type ) {
        List<Type> hierarchy = new ArrayList<>();

        for( Type t = type; t != ts.getTopType(); t = ts.getParent( t ) ) {
            hierarchy.add( t );
        }

        hierarchy.add( ts.getTopType() );

        Collections.reverse( hierarchy );
        return hierarchy;
    }
    
    public static String getShortName( String typeName ) {
        return Pattern.compile( "(.*)\\.([A-Za-z]+)$" ).matcher( typeName ).replaceAll( "$2" );
    }
}
