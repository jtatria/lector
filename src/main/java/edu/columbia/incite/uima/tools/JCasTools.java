/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.tools;

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
public class JCasTools {

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
    
}
