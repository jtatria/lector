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
package edu.columbia.incite.uima.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Common type-system tree operations.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class Types {

    /**
     * Find common parent type between the two given types, or null if no such type exists.
     * 
     * This method will traverse the type tree starting from each of the given types and return the 
     * first node it find that is common to the inheritance chain of both types.
     * 
     * Features associated to the returned type are guaranteed to be valid for both of the given 
     * types.
     * 
     * @param ts A type system.
     * @param k A type.
     * @param j A type.
     * @return A type that both j and k inherit from, or null if there is no such type.
     */
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

    /**
     * Get the types corresponding to the given type names in the given type system or die trying.
     * @param ts A type system.
     * @param typeNames A string array of type names.
     * @return A type.
     * @throws AnalysisEngineProcessException If any of the given names do not correspond to a type 
     *                                        in the given type system.
     */
    public static Type[] checkTypes( TypeSystem ts, String... typeNames ) 
    throws AnalysisEngineProcessException {
        Type[] out = new Type[typeNames.length];
        for( int i = 0; i < out.length; i++ ) {
            out[i] = checkType( ts, typeNames[i] );
        }
        return out;
    }
    
    /**
     * Get the types corresponding to the given type codes in the given type system or die trying.
     * @param ts A type system.
     * @param typeCodes An integer array of type codes.
     * @return A type.
     * @throws AnalysisEngineProcessException If any of the given codes do not correspond to a type 
     *                                        in the given type system.
     */
    public static Type[] checkTypes( TypeSystem ts, int... typeCodes ) 
    throws AnalysisEngineProcessException {
        Type[] out = new Type[typeCodes.length];
        for( int i = 0; i < out.length; i++ ) {
            out[i] = checkType( ts, typeCodes[i] );
        }
        return out;
    }

    /**
     * Get the type corresponding to the given type name in the given type system or die trying.
     * @param ts A type system.
     * @param typeName A fully qualified type name.
     * @return A type.
     * @throws AnalysisEngineProcessException If the given name does not correspond to a type in 
     *                                        the given type system.
     */
    public static Type checkType( TypeSystem ts, String typeName ) 
    throws AnalysisEngineProcessException {
        Type type = ts.getType( typeName );
        if( type == null ) {
            throw new AnalysisEngineProcessException(
                AnalysisEngineProcessException.REQUIRED_FEATURE_STRUCTURE_MISSING_FROM_CAS,
                new Object[] { typeName }
            );
        }
        return type;
    }
    
    /**
     * Get the type corresponding to the given type code in the given type system or die trying.
     * @param ts A type system.
     * @param typeCode An integer type code.
     * @return A type.
     * @throws AnalysisEngineProcessException If the given code does not correspond to a type in 
     *                                        the given type system.
     */
    public static Type checkType( TypeSystem ts, int typeCode )
    throws AnalysisEngineProcessException {
        Type type = ts.getLowLevelTypeSystem().ll_getTypeForCode( typeCode );
        if( type == null ) {
            throw new AnalysisEngineProcessException(
                AnalysisEngineProcessException.REQUIRED_FEATURE_STRUCTURE_MISSING_FROM_CAS,
                new Object[] { "index " + typeCode }
            );
        }
        return type;
    }

    /** Get features or die trying.
     * Extract features with the given feature names from the given type or throw an exception if 
     * any of the given names does not correspond to a feature in the given type.
     * @param type A type.
     * @param featureNames An array of feature names.
     * @return An array with the corresponding features.
     * @throws CASRuntimeException If any of the given names does not correspond to a feature in 
     *                             the given type.
     */
    public static Feature[] checkFeatures( Type type, String... featureNames ) 
    throws CASRuntimeException {
        Feature[] out = new Feature[featureNames.length];
        for( int i = 0; i < out.length; i++ ) {
            out[i] = checkFeature( type, featureNames[i] );
        }
        return out;
    }

    /**
     * Get feature or die trying.
     * Extract a feature with the given feature name from the given type or throw an exception if 
     * no such feature is found in the given type.
     * @param type A type.
     * @param featureName A feature name.
     * @return A feature with the given name in the given type.
     * @throws CASRuntimeException If no feature with that name can be found in the given type.
     */
    public static Feature checkFeature( Type type, String featureName ) 
    throws CASRuntimeException {
        Feature feature = type.getFeatureByBaseName( featureName );
        if( feature == null ) {
                throw new CASRuntimeException(
                    CASRuntimeException.INAPPROP_FEAT,
                    new Object[] { featureName, type.getName() }
                );
        }
        return feature;
    }

    /**
     * Get the inheritance chain for the given type in the given type system.
     * @param ts   A type system.
     * @param type A type.
     * @return A list representing the inheritance chain, in ascending order (children first).
     */
    public static List<Type> getTypeHierarchy( TypeSystem ts, Type type ) {
        List<Type> hierarchy = new ArrayList<>();

        for( Type t = type; t != ts.getTopType(); t = ts.getParent( t ) ) {
            hierarchy.add( t );
        }

        hierarchy.add( ts.getTopType() );

        Collections.reverse( hierarchy );
        return hierarchy;
    }
    
//    public static String getShortName( String typeName ) {
//        // WTF this is insane.
//        return Iterables.getLast( Splitter.on( TypeSystem.NAMESPACE_SEPARATOR ).split( typeName ) );
//    }
    
    /**
     * Filter the given collection of annotations to remove all annotations that are not instances
     * of any of the given types.
     * @param input A collection of annotations.
     * @param types A set of types.
     * @return A collection of annotations with all annotations not instantiating one of the given 
     *         types removed.
     */
    public static Collection<AnnotationFS> filterTypes(
        Collection<AnnotationFS> input, Set<Type> types 
    ) {
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
    
    /**
     * Return {@code true} if the given annotation is an instance of the given type.
     * i.e. check if the annotation is the given type or any of its ancestor types.
     * @param ann An annotation
     * @param chk A type
     * @return {@code true} if ann is an instance of chk, {@code false} otherwise.
     */
    public static boolean isType( AnnotationFS ann, Type chk ) {
        if( ann == null || chk == null ) return false;
        Type type = ann.getType();
        while( type != null ) {
            if( type == chk ) return true;
            type = ann.getView().getTypeSystem().getParent( type );
        }
        return false;
    }
}
