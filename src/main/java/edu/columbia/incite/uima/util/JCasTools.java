/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public abstract class JCasTools {

    public static BooleanArray makeArray( JCas jcas, Boolean[] data ) {
        BooleanArray array = new BooleanArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    public static ByteArray makeArray( JCas jcas, Byte[] data ) {
        ByteArray array = new ByteArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    public static ShortArray makeArray( JCas jcas, Short[] data ) {
        ShortArray array = new ShortArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    public static IntegerArray makeArray( JCas jcas, Integer[] data ) {
        IntegerArray array = new IntegerArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    public static LongArray makeArray( JCas jcas, Long[] data ) {
        LongArray array = new LongArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    public static FloatArray makeArray( JCas jcas, Float[] data ) {
        FloatArray array = new FloatArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    public static DoubleArray makeArray( JCas jcas, Double[] data ) {
        DoubleArray array = new DoubleArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    public static StringArray makeArray( JCas jcas, String[] data ) {
        StringArray array = new StringArray( jcas, data.length );
        for( int i = 0; i < data.length; i++ ) {
            array.set( i, data[i] );
        }
        return array;
    }

    public static Map<AnnotationFS,List<AnnotationFS>> multiTypeCoverIndex( JCas jcas, Type kType, Set<Type> eTypes  ) {
        Type base = jcas.getCas().getTypeSystem().getType( CAS.TYPE_NAME_ANNOTATION );
        Map<AnnotationFS,Collection<AnnotationFS>> index = CasUtil.indexCovering( jcas.getCas(), kType, base );
        return filterIndex( index, eTypes );
    }
    
    public static Map<AnnotationFS,List<AnnotationFS>> multiTypeMemberIndex( JCas jcas, Type kType, Set<Type> eTypes  ) {
        Type base = jcas.getCas().getTypeSystem().getType( CAS.TYPE_NAME_ANNOTATION );
        Map<AnnotationFS,Collection<AnnotationFS>> index = CasUtil.indexCovered( jcas.getCas(), kType, base );
        return filterIndex( index, eTypes );
    }

    private static Map<AnnotationFS,List<AnnotationFS>> filterIndex( Map<AnnotationFS,Collection<AnnotationFS>> input, Set<Type> filter ) {
        Map<AnnotationFS,List<AnnotationFS>> out = new HashMap<>();
        for( Map.Entry<AnnotationFS,Collection<AnnotationFS>> e : input.entrySet() ) {
            List<AnnotationFS> entry = new ArrayList<>();
            for( AnnotationFS member : Types.filterTypes( e.getValue(), filter ) ) {
                entry.add( member );
            }
            out.put( e.getKey(), Collections.unmodifiableList( entry ) );
        }
        return Collections.unmodifiableMap( out );
    }
    
}
