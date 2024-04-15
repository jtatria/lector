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

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.columbia.incite.util.SparseMatrix.Record;

/**
 * Naive, single-threaded implementation for a sparse matrix in simple tuple format.
 * 
 * NB: Instances of this class are not thread-safe and should be synchronized externally; the 
 * {@link #merge(edu.columbia.incite.util.SparseMatrix)} method may be used to combine data from 
 * different instances in multithreaded environments.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class SparseMatrix {

    /** Record struct size in bytes. **/
    public static int size_t = Integer.BYTES + Integer.BYTES + Double.BYTES;
    
    private final TIntObjectMap<TIntDoubleMap> data = new TIntObjectHashMap<>();

    private int maxRow = -1;
    private int maxCol = -1;
    
    /** 
     * Increment entry at row i column j by the value d.
     * 
     * @param i Row index
     * @param j Col index
     * @param d Increment
     */
    public void update( int i, int j, double d ) {
        maxRow = i > maxRow ? i : maxRow;
        maxCol = j > maxCol ? j : maxCol;
        data.putIfAbsent( i, new TIntDoubleHashMap() );
        data.get( i ).adjustOrPutValue( j, d, d );
    }
    
    /**
     * Increments values in this SparseMatrix by the values from the given src Sparsematrix.
     * 
     * @param src A SparseMatrix with values to add to this matrix.
     */
    public void merge( SparseMatrix src ) {
        for( int i : src.data.keys() ) {
            TIntDoubleMap row = src.data.get( i );
            for( int j : row.keys() ) { 
                this.update( i, j, row.get( j ) );
            }
        }
    }
    
    /**
     * Row and column index of the last (bottom-right) entry in this matrix.
     * 
     * @return An {@code int[]{row,col}} array with maximum indices.
     */
    public int[] last() {
        return new int[]{ maxRow, maxCol };
    }
    
    /**
     * Number of non-zero rows in this matrix.
     * 
     * NB: This is not the same as the row index of the maximal row! use {@link #last()} to get the 
     * maximum row and column indices.
     * 
     * @return number of rows with non-zero entries in this matrix.
     */
    public int nrows() {
        return data.keys().length;
    }

    /**
     * Drop all data from this SparseMatrix.
     */
    public void clear() {
        for( int i : data.keys() ) {
            data.get( i ).clear();                
        }
        data.clear();
    }

    /**
     * Total number of non-zero entries in this SparseMatrix.
     * 
     * NB: This number times the value of {@link #size_t} will equal the total size of 
     * this SparseMatrix in bytes.
     * 
     * @return The number of non-zero entries in this SparseMatrix
     */
    public long size() {
        long size = 0;
        for( int i : data.keys() ) {
            size += data.get( i ).size();
        }
        return size;
    }
    
    /**
     * Obtain a copy the given src SparseMatrix.
     * @param src A SparseMatrix.
     * @return A new SparseMatrix containing a copy of the data in src.
     */
    public static SparseMatrix clone( SparseMatrix src ) {
        return copy( src, new SparseMatrix() );
    }

    /**
     * Copy the given src SparseMatrix to the given tgt SparseMatrix.
     * 
     * @param src A SparseMatrix to read values from.
     * @param tgt A SparseMatrix to write values to.
     * @return A reference to tgt.
     */
    public static SparseMatrix copy( SparseMatrix src, SparseMatrix tgt ) {
        for( int i : src.data.keys() ) {
            TIntDoubleMap row = src.data.get( i );
            for( int j : row.keys() ) { 
                tgt.update( i, j, row.get( j ) );
            }
        }
        return tgt;
    }
        
    /**
     * Write the given SparseMatrix m to disk at the given location path.
     * 
     * This class uses a simple binary format consisting of (int,int,double) triplets in ascending 
     * order, sorted by row and then column.
     * 
     * NB: Bytes are written in native order (usually little-endian) and NOT in the default Java 
     * big-endian order.
     * 
     * @param m A SparseMatrix instance.
     * @param path A {@link Path} to write data to.
     * @throws IOException
     */
    public static void save( SparseMatrix m, Path path ) throws IOException {
        long entries = m.size();
        try ( FileChannel fc = FileUtils.openChannel( path, true, true, true, true ) ) {
            int[] rows = m.data.keys();
            Arrays.sort( rows );
            MappedByteBuffer buffer = fc.map( FileChannel.MapMode.READ_WRITE, 0, entries * size_t );
            buffer.order( ByteOrder.nativeOrder() );
            for( int i : rows ) {
                TIntDoubleMap row = m.data.get( i );
                int[] cols = row.keys();
                Arrays.sort( cols );
                for( int j : cols ) {
                    double d = row.get( j );
                    buffer.putInt( i );
                    buffer.putInt( j );
                    buffer.putDouble( d );
                }
            }
        }
    }
    
    /**
     * Load SprseMatrix data from the file at the location of the given directory and file names.
     * 
     * This method will attempt to read the file at the given location as if it were a continuous 
     * byte stream of (int,int,double) tuples.
     * 
     * @param dir A directory name.
     * @param file A file name.
     * @return A new SparseMatrix instance with all data found at the given location.
     * 
     * @throws IOException 
     */
    public static SparseMatrix load( String dir, String file ) throws IOException {
        SparseMatrix out = new SparseMatrix();
        try( FileChannel fc = FileUtils.openChannel( dir, file, true, StandardOpenOption.READ ) ) {
            MappedByteBuffer buffer = fc.map( FileChannel.MapMode.READ_ONLY, 0, fc.size() );
            while( buffer.position() != buffer.capacity() ) {
                int i = buffer.getInt();
                int j = buffer.getInt();
                double d = buffer.getDouble();
                out.update( i, j, d );
            }
        }
        return out;
    }

    /**
     * Obtain a copy of the data in this SparseMatrix as a {@link List} of 
     * {@link SparseMatrix.Record}.
     * 
     * @return A {@link List} with records for all non-sero entries in this matrix.
     */
    public List<Record> triplets() {
        List<Record> out = new ArrayList<>();
        for( int i : data.keys() ) {
            TIntDoubleMap row = data.get( i );
            for( int j : row.keys() ) {
                double x = row.get( j );
                out.add( new Record( i, j, x ) );
            }
        }
        out.sort( ( r1, r2 ) -> r1.compareTo( r2 ) );
        return out;
    }
    
    /**
     * Copy the data in this matrix into arrays.
     * 
     * See {@link SpArrays} for details.
     * 
     * @return An {@link SpArrays} instance with all data in this matrix.
     */
    public SpArrays arrays() {
        List<Record> trs = triplets();
        int[] i = new int[ trs.size() ];
        int[] j = new int[ trs.size() ];
        double[] x = new double[ trs.size() ];
        for( int it = 0; it < trs.size(); it++ ) {
            Record r = trs.get( it );
            i[it] = r.i;
            j[it] = r.j;
            x[it] = r.x;
        }
        return new SpArrays( i, j, x );
    }

    /**
     * Array representation of a SparseMatrix.
     */
    public static class SpArrays {
        /** Row indices **/
        public final int[] i;
        /** Col indices **/
        public final int[] j;
        /** Values **/
        public final double[] x;
        
        /**
         * Create a new array representation from the given arrays.
         * @param i
         * @param j
         * @param x 
         */
        SpArrays( int[] i, int[] j, double[] x ) {
            this.i = i;
            this.j = j;
            this.x = x;
        }
    }
    
    /** 
     * A SparseMatirx record.
     * 
     * An (int,int,double) tuple representing a non-zero entry in a SparseMatrix.
     * 
     */
    public static class Record implements Comparable<Record> {
        /** Row index **/
        public int i;
        /** Col index **/
        public int j;
        /** Value **/
        public double x;
        
        /**
         * Create a new record from the given indices and data.
         * @param i A row index
         * @param j A col index
         * @param x A value
         */
        Record( int i, int j, double x ) {
            this.i = i;
            this.j = j;
            this.x = x;
        }

        @Override
        public int compareTo( Record o ) {
            int icomp = Integer.compare( this.i, o.i );
            if( icomp != 0 ) return icomp;
            return Integer.compare( this.j, o.j );
        }
    }
}
