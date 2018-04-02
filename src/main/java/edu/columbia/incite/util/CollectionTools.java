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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Utility methods for manipulating java collections.
 *
 * This class should not be instantiated.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class CollectionTools {

    public static <T> T[] window( T[] src, int w, int at ) {
        return window( src, w, at, true );
    }

    public static <T> List<T> window( List<T> src, int w, int at ) {
        return window( src, w, at, true );
    }

    public static <T> T[] window( T[] src, int w, int at, boolean fill ) {
        List<T> window = window( Arrays.asList( src ), w, at, fill );
        T[] out = Arrays.copyOf( src, window.size() );
        return window.toArray( out );
    }

    public static <T> List<T> window( List<T> src, int w, int at, boolean fill ) {
        int lo = at - w;
        int hi = at + w + 1;
        if( lo < 0 || hi >= src.size() ) {
            if( fill ) { // fill array with nulls
                List<T> pre = new ArrayList<>();
                List<T> pos = new ArrayList<>();
                if( lo < 0 ) { // fill left.
                    int pad = Math.abs( lo );
                    pre = nullList( pad );
                    hi += pad;
                    lo = 0;
                }
                if( hi > src.size() ) { // fill right
                    int pad = hi - src.size();
                    pos = nullList( pad );
                }
                pre.addAll( src );
                pre.addAll( pos );
                src = pre;
            } else { // don't fill, restrict window size
                lo = lo < 0 ? 0 : lo;
                hi = hi > src.size() ? src.size() : hi;
            }
        }
        return src.subList( lo, hi );
    }

    public static <T> List<T> nullList( int n ) {
        List<T> out = new ArrayList<>( n );
        for( int i = 0; i < n; i++ ) {
            out.add( null );
        }
        return out;
    }
    
    public static <K extends Comparable<K>> LinkedHashMap<K,Integer> sortedIndexMap( K... ks ) {
        return indexMap( CollectionTools.toSortedList( ks ) );
    }
    
    public static <K extends Comparable<K>> LinkedHashMap<K,Integer> sortedIndexMap( Collection<K> ks ) {
        return indexMap( CollectionTools.toSortedList( ks ) );
    }
    
    public static <K> LinkedHashMap<K,Integer> indexMap( List<? extends K> ks ) {
        LinkedHashMap<K,Integer> out = new LinkedHashMap<>();
        int i = 0;
        for( K k : ks ) {
            out.put( k, i++ );
        }
        return out;
    }

    public static <K,V> HashMap<K,V> hashMap( Collection<K> k, Collection<V> v, boolean fail ) {
        return mapEm( new HashMap<>(), k, v, fail );
    }

    public static <K extends Comparable,V> TreeMap<K,V> treeMap( Collection<K> k, Collection<V> v, boolean fail ) {
        return mapEm( new TreeMap<>(), k, v, fail );
    }

    public static <M extends Map<K,V>,K,V> M mapEm( M tgt, Collection<K> k, Collection<V> v, boolean fail ) {
        Iterator<K> kit = k.iterator();
        Iterator<V> vit = v.iterator();
        while( kit.hasNext() && vit.hasNext() ) {
            tgt.put( kit.next(), vit.next() );
        }
        if( kit.hasNext() || vit.hasNext() && fail ) {
            throw new IllegalArgumentException(
                String.format( "Size mismatch: %s", vit.hasNext() ? "too many keys" : "too many values" )
            );
        }
        return tgt;
    }

    public static Double[] asRefs( double[] data ) {
        Double[] out = new Double[data.length];
        for( int i = 0; i < data.length; i++ ) {
            out[i] = data[i];
        }
        return out;
    }

    public static Collection<Double> asCol( double[] data ) {
        return Arrays.asList( asRefs( data ) );
    }
    
    public static <T extends Comparable<T>> List<T> toSortedList( T... t ) {
        return toSortedList( Arrays.asList( t ) );
    }
    
//    public static <T extends Comparable<T>> List<T> toSortedList( Collection<T> t ) {
    public static <T extends Comparable<T>> List<T> toSortedList( Collection ts ) {
        List<T> list = new ArrayList<>();
        for( Object t : ts ) {
            if( !Comparable.class.isAssignableFrom( t.getClass() ) ) throw new IllegalArgumentException();
            list.add( (T) t );
        }
        Collections.sort( list );
        return list;
    }
        
    public static double[] doubles( Number[] d ) {
        double[] r = new double[d.length];
        for( int i = 0; i < d.length; i++ ) {
            r[i] = d[i].doubleValue();
        }
        return r;
    }
    
    public static int[] ints( Number[] d ) {
        int[] r = new int[d.length];
        for( int i = 0; i < d.length; i++ ) {
            r[i] = d[i].intValue();
        }
        return r;
    }
    
    public static long[] longs( Number[] d ) {
        long[] r = new long[d.length];
        for( int i = 0; i < d.length; i++ ) {
            r[i] = d[i].longValue();
        }
        return r;
    }
    
    public static boolean isArray( Object obj ) {
        return obj instanceof Object[] ||
            obj instanceof byte[] ||
            obj instanceof short[] ||
            obj instanceof int[] ||
            obj instanceof long[] ||
            obj instanceof float[] ||
            obj instanceof double[] ||
            obj instanceof boolean[] ||
            obj instanceof char[];
    }
    
    public static <T> Map<String,T> labelMap( Collection<T> ts ) {
        Map<String,T> out = new HashMap<>();
        for( T t : ts ) {
            out.put( t.toString(), t );
        }
        return out;
    }
    
    public static <T,V extends Comparable<V>> int closestTo( V ref, List<T> list, Function<T,V> comp ) {
        int lo = 0;
        int hi = list.size();
        int mid = -1;
        while( lo <= hi ) {
            mid = lo + ( ( hi - lo ) / 2 );
            if( comp.apply( list.get( mid ) ).compareTo( ref ) < 0 ) {
                hi = mid - 1;
            } else if( comp.apply( list.get( mid ) ).compareTo( ref ) > 0 ) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        return mid;
    }
    
    public static <T> List<T> sample( List<T> pop, final int n ) {
        return sample( pop, n, false );
    }
    
    public static <T> List<T> sample( List<T> pop, final int n, boolean replace ) {
        return sample( pop, n, replace, new Random( System.nanoTime() ) );
    }
    
    public static <T> List<T> sample( List<T> pop, final int n, boolean replace, Random r ) {
        int N = pop.size();
        if( N == n ) return new ArrayList<>( pop );
        
        if( N < n ) throw new IllegalArgumentException( 
            String.format( "Sample size %d exceeds population size %d", n, N )
        );

        boolean[] mask = new boolean[ pop.size() ];
        
        List<T> sample = new ArrayList<>();
        while( sample.size() < n ) {
            int i = r.nextInt( N );
            if( !replace && mask[i] ) continue;
            sample.add( pop.get( i ) );
            mask[i] = true;            
        }
        
        return sample;
    }
}