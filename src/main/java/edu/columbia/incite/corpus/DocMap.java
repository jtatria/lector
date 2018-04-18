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
package edu.columbia.incite.corpus;

import java.io.IOException;
import java.util.SortedSet;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FST.INPUT_TYPE;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;

/**
 * Convenience class to construct document maps using Lucene's Finite State Transducers.
 * 
 * Typical corpus sizes make runtime document indexing prohibitive using Java's standard maps. 
 * This class offers a memory-cheap alternative to create caches for document's field values that 
 * are several orders of magnitude faster than trying to access these values from the index, for 
 * any field (not only doc-values fields, i.e. without requiring a prior decision at index-time).
 * 
 * This class is much more general than this use-case, though, as it allows associating arbitrary 
 * output data to integers, mapping each discrete output value to a unique long, internally. It 
 * performs reasonably well when the number of integers is large (millions) but the number of 
 * discrete output values is small (thousands), since a regular map is still required internally 
 * between output values of arbitrary type and the long keys used in the internal FST.
 * 
 * Integer keys (input values) must be added in sorted order according to the natural ordering of 
 * integers values. Adding a smaller input value after a larger input value has already been added 
 * will result in an {@link OutOfOrderException}.
 * 
 * Bidirectional retrieval is possible, if the output values are added sequentially, i.e. all 
 * bindings for one output value must be added before adding a binding for any other output value. 
 * If this condition is not satisfied (i.e. bindings for different values are added in an 
 * interspersed manner), then the long keys used in the underlying FST will not be sorted and 
 * bidirectional retrieval will be disabled.
 * 
 * An alternative to sequential construction for cases in which all values are known in advance is 
 * to use the {@link #DocMap(java.util.SortedSet) } constructor, which ensures sorted construction,
 * ensuring bidirectional retrieval.
 * 
 * Note that the map must be populated in full using the {@link #add(int, java.lang.Object) } 
 * method and then finished via the {@link #finish() } method before it can be queried with the 
 * {@link #get(int) } method. 
 * 
 * Querying a DocMap before finishing it will result in undefined behavior, but this may change in 
 * future releases to add proper error handling.
 * 
 * Instances of this class can also be used as {@link IntFunction} in lambda expressions.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 * @param <T> Type for associated values, i.e. entries.
 */
public class DocMap<T> implements IntFunction<T> {

    private Builder<Long> bldr    = new Builder( INPUT_TYPE.BYTE4, PositiveIntOutputs.getSingleton() );
    private BiMap<Long,T> outputs = HashBiMap.create();
    private FST<Long> fst;
    
    private boolean sorted = true;
    private boolean open   = true;
    private long lastV = -1;
    private int lastK = -1;
    
    private final ThreadLocal<IntsRef> tKey = ThreadLocal.withInitial(
        () -> new IntsRef( new int[1], 0, 1 )
    );

    /** 
     * Create a new DocMap instance with no outputs and no documents.
     */
    public DocMap() {
        this( null );
    }
    
    /** 
     * Create a new DocMap with the given sorted set of T values for output.
     * 
     * Adding values in advance will preserve bidirectional retrieval in the resulting map.
     * 
     * @param ts A sorted set of known T output values.
     */
    public DocMap( SortedSet<T> ts ) {
        if( ts == null ) return;
        for( T t : ts ) makeV( t );
    }
    
    /**
     * Add a new association between the given document number and the given T output value.
     * 
     * Adding output values in an interspersed manner will disable bidirectional retrieval.
     * 
     * New associations can not be added once the {@link #finish()} method has been called.
     * 
     * @param k A document number.
     * @param t An output value.
     * 
     * @throws IOException
     */
    public void add( int k, T t ) throws IOException {
        if( !open ) throw new UnsupportedOperationException(
            String.format( "Can't add new key %d to closed DocMap", k )
        );
        
        if( lastK > k ) throw new OutOfOrderException( k, lastK );
        
        long v = makeV( t );
        if( lastV > v ) this.sorted = false;
        
        bldr.add( makeK( k ), outputs.inverse().get( t ) );
        lastK = k;
        lastV = v;
    }
    
    /**
     * Finish adding associations to this DocMap.
     * 
     * This method must be called once all entries in the map have been added and before querying 
     * the resulting map for results.
     * 
     * Failing to do so will throw a null pointer exception (TODO).
     * 
     * @throws IOException 
     */
    public void finish() throws IOException {
        if( !open ) throw new UnsupportedOperationException( "Attempting to finish finished DocMap" );
        this.open    = false;
        this.outputs = ImmutableBiMap.copyOf( outputs );
        this.fst     = bldr.finish();
        this.bldr    = null;
    }
    
    /**
     * Obtain a map of T values and the long values used internally in the underlying FST.
     * 
     * @return A BiMap containing output values and their internal numeric keys.
     */
    public BiMap<Long,T> outputMap() {
        return ImmutableBiMap.copyOf( outputs.entrySet() );
    }
    
    /** 
     * Get the number of outputs in this DocMap.
     * @return The total number of distinct T values in this DocMap.
     */
    public int numOutputs() {
        return this.outputs.values().size();
    }
    
    /**
     * Obtain the output key associated to the given T value in this DocMap's underlying FST.
     * @param output A T value.
     * @return An internal numeric key for the given T value.
     */
    public long outputKey( T output ) {
        return this.outputs.inverse().get( output );
    }
        
    /**
     * Get whether this DocMap has sorted outputs.
     *
     * Sorted outputs means that the distinct T values used as entries in this DocMap's 
     * associations where inserted in order, with all associations for a given value entered before 
     * any association for a different value.
     * 
     * This is independent of the natural sorting order of values of T and applies only to the 
     * insertion order!
     * 
     * Sorted outputs are needed for bidirectional retrieval.
     * 
     * @return {code true} if the outputs in this DocMap were entered in sorted order.
     */
    public boolean sorted() {
        return this.sorted;
    }
    
    /**
     * Bidirectional retrieval.
     * 
     * Not supported yet.
     * 
     * @param output
     * @return a doc number.
     * 
     * @throws UnsupportedOperationException always.
     */
    public int outputDoc( T output ) {
        if( !sorted ) {
            throw new IllegalStateException(
                "Retrieval of keys by output requires a sorted DocMap"
            );
        }
        throw new UnsupportedOperationException( "Not supported yet." );
    }
    
    private IntsRef makeK( int k ) {
        tKey.get().ints[0] = k;
        return tKey.get();
    }
    
    private long makeV( T t ) {
        long v;
        if( outputs.containsValue( t ) ) {
            v = outputs.inverse().get( t );
        } else {
            v = ++lastV;
            outputs.put( v, t );
        }
        return v;
    }
        
    /**
     * Main query method. Get the T value associated to the given document number.
     * @param doc a document number
     * @return A T value associated to the given document number in this DocMap.
     * @throws IOException 
     */
    public T get( int doc ) throws IOException {
        if( this.fst == null ) throw new IllegalStateException( "Querying unfinished DocMap" );
        long v = Util.get( fst, makeK( doc ) );
        return outputs.get( v );
    }
    
    @Override
    public T apply( int t ) {
        try {
            return get( t );
        } catch( IOException ex ) {
            Logger.getLogger( DocMap.class.getName() ).log( Level.SEVERE, null, ex );
            return null;
        }
    }

    /**
     * Thrown when input values for a DocMap are added out of order.
     */
    public static class OutOfOrderException extends IllegalArgumentException {
        public OutOfOrderException( int cur, int last ) {
            super( String.format( 
                "FST keys must be inserted in sorted order: tried to add %d after %d"
                , cur, last
            ) );
        }
    }
}
