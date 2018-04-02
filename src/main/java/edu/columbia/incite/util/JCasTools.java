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
package edu.columbia.incite.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
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
    
    public static Map<AnnotationFS,Map<Type,List<AnnotationFS>>> typedIndexCovering(
        JCas jcas, Type kType, Set<Type> eTypes
    ) {
        Map<AnnotationFS,Collection<AnnotationFS>> index = CasUtil.indexCovering(
            jcas.getCas(), kType, jcas.getCas().getTypeSystem().getType( CAS.TYPE_NAME_ANNOTATION )
        );
        return splitIndex( index, eTypes );
    }
    
    public static Map<AnnotationFS,Map<Type,List<AnnotationFS>>> typedIndexCovered(
        JCas jcas, Type kType, Set<Type> eTypes
    ) {
        Map<AnnotationFS,Collection<AnnotationFS>> index = CasUtil.indexCovered(
            jcas.getCas(), kType, jcas.getCas().getTypeSystem().getType( CAS.TYPE_NAME_ANNOTATION )
        );
        return splitIndex( index, eTypes );
    }    

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