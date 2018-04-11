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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;

import edu.columbia.incite.uima.SegmentedProcessor;

/**
 * Utility methods for manipulating UIMA CAS data structures.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class JCasTools {

    /** Create a UIMA {@link BooleanArray} in the given {@link JCas} instance with the given data.
     * 
     * @param jcas A {@link JCas}
     * @param data A boolean array.
     * @return An instance of a {@link BooleanArray} over the given {@link JCas} containing the 
     * given data.
     */
    public static BooleanArray makeArray( JCas jcas, Boolean[] data ) {
        BooleanArray array = new BooleanArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    /** Create a UIMA {@link ByteArray} in the given {@link JCas} instance with the given data.
     * 
     * @param jcas A {@link JCas}
     * @param data A byte array.
     * @return An instance of a {@link ByteArray} over the given {@link JCas} containing the 
     * given data.
     */
    public static ByteArray makeArray( JCas jcas, Byte[] data ) {
        ByteArray array = new ByteArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    /** Create a UIMA {@link ShortArray} in the given {@link JCas} instance with the given data.
     * 
     * @param jcas A {@link JCas}
     * @param data A short array.
     * @return An instance of a {@link ShortArray} over the given {@link JCas} containing the 
     * given data.
     */
    public static ShortArray makeArray( JCas jcas, Short[] data ) {
        ShortArray array = new ShortArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    /** Create a UIMA {@link IntegerArray} in the given {@link JCas} instance with the given data.
     * 
     * @param jcas A {@link JCas}
     * @param data An integer array.
     * @return An instance of a {@link IntegerArray} over the given {@link JCas} containing the 
     * given data.
     */
    public static IntegerArray makeArray( JCas jcas, Integer[] data ) {
        IntegerArray array = new IntegerArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    /** Create a UIMA {@link LongArray} in the given {@link JCas} instance with the given data.
     * 
     * @param jcas A {@link JCas}
     * @param data A long array.
     * @return An instance of a {@link LongArray} over the given {@link JCas} containing the 
     * given data.
     */
    public static LongArray makeArray( JCas jcas, Long[] data ) {
        LongArray array = new LongArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    /** Create a UIMA {@link FloatArray} in the given {@link JCas} instance with the given data.
     * 
     * @param jcas A {@link JCas}
     * @param data A float array.
     * @return An instance of a {@link FloatArray} over the given {@link JCas} containing the 
     * given data.
     */
    public static FloatArray makeArray( JCas jcas, Float[] data ) {
        FloatArray array = new FloatArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    /** Create a UIMA {@link DoubleArray} in the given {@link JCas} instance with the given data.
     * 
     * @param jcas A {@link JCas}
     * @param data A double array.
     * @return An instance of a {@link DoubleArray} over the given {@link JCas} containing the 
     * given data.
     */
    public static DoubleArray makeArray( JCas jcas, Double[] data ) {
        DoubleArray array = new DoubleArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    /** Create a UIMA {@link StringArray} in the given {@link JCas} instance with the given data.
     * 
     * @param jcas A {@link JCas}
     * @param data A {@link String} array.
     * @return An instance of a {@link StringArray} over the given {@link JCas} containing the 
     * given data.
     */
    public static StringArray makeArray( JCas jcas, String[] data ) {
        StringArray array = new StringArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }
    
    /**
     * Wrapper for {@link org.apache.uima.fit.util.CasUtil#indexCovering(org.apache.uima.cas.CAS, 
     * org.apache.uima.cas.Type, org.apache.uima.cas.Type) } that allows filtering the covering 
     * annotations.
     * 
     * This method will produce an index that contains all found annotations as keys, and a 
     * collection of all annotations of the requested type that cover each key annotation.
     * Typical usage would be to create an index for all sections covering all found paragraphs, 
     * etc.
     * 
     * Note that this operation includes construction of the index and then filtering the results.
     * This is a rather slow operation; consumers are encouraged to cache results once per CAS, 
     * typically at the beginning of a component's main analysis logic.
     * See {@link SegmentedProcessor#preProcess(org.apache.uima.jcas.JCas) } for usage examples.
     * 
     * @param jcas A {@link JCas} instance.
     * @param kType A type for key annotations.
     * @param eTypes A set of of types for covering annotations to include in the index.
     * @return A map containing the requested index.
     */
    public static Map<AnnotationFS,Map<Type,List<AnnotationFS>>> typedIndexCovering(
        JCas jcas, Type kType, Set<Type> eTypes
    ) {
        Map<AnnotationFS,Collection<AnnotationFS>> index = CasUtil.indexCovering(
            jcas.getCas(), kType, jcas.getCas().getTypeSystem().getType( CAS.TYPE_NAME_ANNOTATION )
        );
        return splitIndex( index, eTypes );
    }

    /**
     * Wrapper for {@link org.apache.uima.fit.util.CasUtil#indexCovered(org.apache.uima.cas.CAS,
     * org.apache.uima.cas.Type, org.apache.uima.cas.Type) } that allows filtering the covered 
     * annotations.
     * 
     * This method will produce an index that contains all found annotations as keys, and a 
     * collection of all annotations of the requested type that are covered by each key annotation.
     * Typical usage would be to create an index for all paragraphs contained in all found sections, 
     * etc.
     * 
     * Note that this operation includes construction of the index and then filtering the results.
     * This is a rather slow operation; consumers are encouraged to cache results once per CAS, 
     * typically at the beginning of a component's main analysis logic.
     * See {@link SegmentedProcessor#preProcess(org.apache.uima.jcas.JCas) } for usage examples.
     * 
     * @param jcas A {@link JCas} instance.
     * @param kType A type for key annotations.
     * @param eTypes A set of of types for covering annotations to include in the index.
     * @return A map containing the requested index.
     */    
    public static Map<AnnotationFS,Map<Type,List<AnnotationFS>>> typedIndexCovered(
        JCas jcas, Type kType, Set<Type> eTypes
    ) {
        Map<AnnotationFS,Collection<AnnotationFS>> index = CasUtil.indexCovered(
            jcas.getCas(), kType, jcas.getCas().getTypeSystem().getType( CAS.TYPE_NAME_ANNOTATION )
        );
        return splitIndex( index, eTypes );
    }    

    /**
     * Filter the given annotation index to exclude all annotations of types other than those 
     * included in the given filtering set.
     * 
     * @param index An annotation index.
     * @param eTypes A set of Types to retain in the given index.
     * @return An annotation index including only annotations of the given types.
     */
    private static Map<AnnotationFS,Map<Type,List<AnnotationFS>>> splitIndex(
        Map<AnnotationFS,Collection<AnnotationFS>> index, Set<Type> eTypes
    ) {
        Map<AnnotationFS,Map<Type,List<AnnotationFS>>> out = new HashMap<>();
        for( AnnotationFS kAnn : index.keySet() ) {
            Map<Type, List<AnnotationFS>> kMap = out.computeIfAbsent( kAnn, a -> new HashMap<>() );
            for( AnnotationFS eAnn : index.get( kAnn ) ) {
                for( Type type : eTypes ) {
                    List<AnnotationFS> list = kMap.computeIfAbsent( type, t -> new ArrayList<>() );
                    if( Types.isType( eAnn, type ) ) {
                        list.add( eAnn );
                    }
                }
            }
        }
        return out;
    }
    
}