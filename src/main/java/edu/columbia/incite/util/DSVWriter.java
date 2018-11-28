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

import com.google.common.collect.Table;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Class for dumping arbitrary data to DSV format.
 * 
 * This class provides several static convenience methods for dumping typical data containers, 
 * like guava {@link Table tables}, trove {@link TIntObjectMap maps}, etc.
 * 
 * Custom data containers may be dumped by passing a suitable implementation of an {@link Accesor}.
 * 
 * Instances of this class can be created in order to modify its default values via a fluent 
 * interface for parameter setting, like so:
 * {@code 
 * DSVWriter w = new DSVWriter( someAccesor )
 *      .rowSeparator( "\n" )
 *      .colSeparator( "\t" )
 *      .missingValue( "NA" )
 *      .addHeader( false );
 * }
 * 
 * NB: this class <em>does not</em> implement any sophisticated escaping procedure for strings. 
 * This is by design; this class is meant for matrix-type data, if you find yourself needing such 
 * procedures, you should reconsider the use of DSV format.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 * @param <D> Type for internal data representations.
 */
public class DSVWriter<D> {

    /** COlumn separator. Defaults to '@'. **/
    public static final String COL_SEP = "@";
    /** Row separator. Defaults to {@link System#lineSeparator() }. **/
    public static final String ROW_SEP = System.lineSeparator();
    /** String for missing values. Defaults to the empty string. **/
    public static final String MISSING = "";
    /** Default header for the first row. Defaults to "_id_". **/
    public static final String ID_TOP  = "_id_";
    /** Default include header. Defaults to {@code true}. **/
    public static final boolean HEADER = true;
    
    private final Accesor<D,?,?,?> acc;
    
    private String rowSep   = ROW_SEP;
    private String colSep   = COL_SEP;
    private String missing  = MISSING;
    private String idHeader = ID_TOP;
    private boolean header  = HEADER;
    
    /**
     * Create a new DSVWriter with the given accesor object.
     * @param acc An Accesor object for D.
     */
    public DSVWriter( Accesor<D,?,?,?> acc ) {
        this.acc = acc;
    }
    
    /** Set this writer's row separator and return it
     * @param rowSep A string to use as row separator.
     * @return This {@link DSVWriter}.
     */
    public DSVWriter rowSeparator( String rowSep ) {
        this.rowSep = rowSep;
        return this;
    }

    /** Set this writer's row separator and return it
     * @param colSep A string to use as column separator.
     * @return This {@link DSVWriter}.
     */    
    public DSVWriter colSeparator( String colSep ) {
        this.colSep = colSep;
        return this;
    }
    
    /** Set this writer's row separator and return it
     * @param missing A string to use for missing values.
     * @return This {@link DSVWriter}.
     */
    public DSVWriter missingValue( String missing ) {
        this.missing = missing;
        return this;
    }
    
    /** Set this writer's row separator and return it
     * @param header Boolean indicating whether to include a header row or not.
     * @return This {@link DSVWriter}.
     */
    public DSVWriter addHeader( boolean header ) {
        this.header = header;
        return this;
    }

    /** Set this writer's row separator and return it
     * @param idHeader A string to use as row id header.
     * @return This {@link DSVWriter}.
     */    
    public DSVWriter rowIdHead( String idHeader ) {
        this.idHeader = idHeader;
        return this;
    }
    
    /**
     * Write the given internal data representation data to the given stream writer w.
     * @param data  Some data
     * @param w A {@link Writer}.
     * @throws IOException 
     */
    public void write( D data, Writer w ) throws IOException {
        write( w, data, acc, idHeader, rowSep, colSep, missing, header );
    }
    
    /**
     * Convenience method: dump a {@code TIntObjectMap<?>} instance t the given Writer using 
     * suitable defaults.
     * 
     * The type of ? should be one of TIntLongMap for frequencies and counts or TIntDoubleMap for 
     * numeric values.
     * 
     * @param ps    A {@link Writer}
     * @param data  Some data, stored as a Trove map.
     * @throws IllegalArgumentException if the type of row entries is not one of Trove's numeric 
     *      maps.
     * @throws IOException 
     */
    @SuppressWarnings("unchecked") // JAVA SUCKS
    public static void write( Writer ps, TIntObjectMap data ) throws IOException {
        Object probe = null;
        int[] keys = data.keys();
        int i = 0;
        while( probe == null ) {
            probe = data.get( keys[i++] );
        }
        
        if( probe == null ) {
            throw new IllegalArgumentException( "Can't infer row type! Is data empty?" );
        }
        
        if( probe.getClass().isAssignableFrom( TIntLongMap.class ) ) {
            write( ps, (TIntObjectMap<TIntLongMap>) data, new TIntIntLongAccesor() );
        } else 
        if( probe.getClass().isAssignableFrom( TIntDoubleMap.class ) ) {
            write( ps, (TIntObjectMap<TIntDoubleMap>) data, new TIntIntDoubleAccesor() );
        } else {
            throw new IllegalArgumentException( "Unknown data type!" );
        }
    }

    /**
     * Convenience method: dump the given Map<R,Map<C,V>> to the given Writer using suitable 
     * defaults.
     * 
     * This method expects all of R,C and V to have reasonable toString() methods, as it uses the 
     * built-in {@link MapAccesor}.
     * 
     * @param <R> Type for row keys
     * @param <C> Type for col keys
     * @param <V> Type for data entries
     * @param ps A {@link Writer}
     * @param data Some data
     * @throws IOException 
     */
    public static <R extends Comparable<R>,C extends Comparable<C>,V> void write(
        Writer ps, Map<R,Map<C,V>> data 
    ) throws IOException {
        write( ps, data, new MapAccesor<>() );
    }
    
    /**
     * Convenience method: dump the given Table<R,C,V> to the given Writer using suitable 
     * defaults.
     * 
     * This method expects all of R,C and V to have reasonable toString() methods, as it uses the 
     * built-in {@link TableAccesor}.
     * 
     * @param <R> Type for row keys
     * @param <C> Type for col keys
     * @param <V> Type for data entries
     * @param ps A {@link Writer}
     * @param data Some data
     * @throws IOException 
     */
    public static <R extends Comparable<R>,C extends Comparable<C>,V> void write( 
        Writer ps, Table<R,C,V> data 
    ) throws IOException {
        write( ps, data, new TableAccesor<>() );
    }
    
    /**
     * Advanced: Dump the given internal representation of data to the given {@link Writer}, using 
     * the given {@link Accesor} with default printing options.
     * 
     * @param <D> Type for internal data representations.
     * @param <R> Type for row keys.
     * @param <C> Type for column keys.
     * @param <V> Type for data entries.
     * @param ps    A {@link Writer}
     * @param data  Some data
     * @param acc   A suitable {@link Accesor}
     * @throws IOException 
     */
    public static <D,R extends Comparable<R>,C extends Comparable<C>,V> void write(
        Writer ps, D data, Accesor<D,R,C,V> acc 
    ) throws IOException {
        write( ps, data, acc, ID_TOP, ROW_SEP, COL_SEP, MISSING, HEADER );
    }
    
    /**
     * Advanced: Dump the given internal representation of data to the given {@link Writer}, using 
     * the given {@link Accesor} with the given options.
     * 
     * All the other {@code write(...)} methods in this class call this method internally.
     * 
     * @param <D> Type for internal data representations.
     * @param <R> Type for row keys.
     * @param <C> Type for column keys.
     * @param <V> Type for data entries.
     * @param ps    A {@link Writer}
     * @param data  Some data
     * @param acc   A suitable {@link Accesor}
     * @param idTop Value to use as the column head of the row id keys (i.e. the value in cell 'A1').
     * @param rowSep    Row separator, typically "\n".
     * @param colSep    Column separator, typically "\t" or ",", but this is usually naive.
     * @param missing   A filler for missing values.
     * @param header    boolean indicating whether the resulting file should include column headers.
     * @throws IOException 
     */
    public static <D,R extends Comparable<R>,C extends Comparable<C>,V> void write(
        Writer ps, D data, Accesor<D,R,C,V> acc,
        String idTop, String rowSep, String colSep, String missing, boolean header
    ) throws IOException {
        SortedSet<C> cols = acc.cols( data );
        if( header ) {
            String head = makeHead( idTop, cols, acc.colFunc(), rowSep, colSep );
            ps.append( head );
        }
        for( R r : acc.rows( data ) ) {
            String row = makeRow(
                r, cols, acc.values( data, r ), acc.rowFunc(), acc.valFunc(), rowSep, colSep, 
                missing 
            );
            ps.append( row );
        }
        ps.close();
    }
       
    /**
     * Convenience method to write floating point array matrices.
     * 
     * This method will use default values for row separator, column separator, missing values,
     * whether to include a header row and row id header.
     * 
     * The given {@code double[][]} array is assumed to be square, i.e. all second-order arrays 
     * should be of the same length, and the length of the first such array will be assumed to be 
     * equal to the number of columns in the matrix.
     * 
     * @param ps      A print stream to write data to.
     * @param data    A {@code double[][]} instance with matrix data.
     * @param rows    A list of row ids of the same length as entries in the first order array.
     * @param cols    A list of col ids of the same length as the first second-order array.

     * @throws IOException 
     */
    public static void write( Writer ps, double[][] data, List<String> rows, List<String> cols )
    throws IOException {
        write( ps, data, rows, cols, ID_TOP );
    }

    /**
     * Convenience method to write floating point array matrices.
     * 
     * This method will use default values for row separator, column separator, missing values and 
     * whether to include a header row.
     * 
     * The given {@code double[][]} array is assumed to be square, i.e. all second-order arrays 
     * should be of the same length, and the length of the first such array will be assumed to be 
     * equal to the number of columns in the matrix.
     * 
     * @param ps      A print stream to write data to.
     * @param data    A {@code double[][]} instance with matrix data.
     * @param rows    A list of row ids of the same length as entries in the first order array.
     * @param cols    A list of col ids of the same length as the first second-order array.
     * @param idTop   A string to use as row id header (i.e. the 0,0 entry in the resulting file).

     * @throws IOException 
     */
    public static void write(
        Writer ps, double[][] data, List<String> rows, List<String> cols, String idTop
    ) throws IOException {
        write( ps, data, rows, cols, idTop, ROW_SEP, COL_SEP, MISSING, HEADER );
    }
    
    /**
     * Convenience method to write floating point array matrices.
     * 
     * The given {@code double[][]} array is assumed to be square, i.e. all second-order arrays 
     * should be of the same length, and the length of the first such array will be assumed to be 
     * equal to the number of columns in the matrix.
     * 
     * @param ps      A print stream to write data to.
     * @param data    A {@code double[][]} instance with matrix data.
     * @param rows    A list of row ids of the same length as entries in the first order array.
     * @param cols    A list of col ids of the same length as the first second-order array.
     * @param idTop   A string to use as row id header (i.e. the 0,0 entry in the resulting file).
     * @param rowSep  A row separator string. Use newlines.
     * @param colSep  A column separator string. Use something uncommon.
     * @param missing A string for missing values.
     * @param header  Include a header row?
     * 
     * @throws IOException 
     */
    public static void write(
        Writer ps, double[][] data, List<String> rows, List<String> cols, String idTop, 
        String rowSep, String colSep, String missing, boolean header
    ) throws IOException {
        if( rows.size() != data.length || cols.size() != data[0].length ) {
            throw new IllegalArgumentException();
        }
        
        if( header ) {
            List<String> head = new ArrayList<>();
            head.add( idTop );
            head.addAll( cols );
            ps.append( String.join( colSep, head.toArray( new String[head.size()] ) ) );
            ps.append( rowSep );
        }
        
        for( int i = 0; i < data.length; i++ ) {
            ps.append( rows.get( i ) );
            StringJoiner sj = new StringJoiner( colSep );
            for( double x : data[i] ) {
                sj.add( Double.toString( x ) );
            }
            ps.append( sj.toString() );
            ps.append( rowSep );
        }
        ps.close();
    }
    
    /**
     * Convenience method to write long integer array matrices.
     * 
     * This method will use default values for row separator, column separator, missing values,
     * whether to include a header row and row id header.
     * 
     * The given {@code long[][]} array is assumed to be square, i.e. all second-order arrays 
     * should be of the same length, and the length of the first such array will be assumed to be 
     * equal to the number of columns in the matrix.
     * 
     * @param ps      A print stream to write data to.
     * @param data    A {@code long[][]} instance with matrix data.
     * @param rows    A list of row ids of the same length as entries in the first order array.
     * @param cols    A list of col ids of the same length as the first second-order array.
     * 
     * @throws IOException 
     */
    public static void write( Writer ps, long[][] data, List<String> rows, List<String> cols )
    throws IOException {
        write( ps, data, rows, cols, ID_TOP );
    }
    
    /**
     * Convenience method to write long integer array matrices.
     * 
     * This method will use default values for row separator, column separator, missing values and
     * whether to include a header row.
     * 
     * The given {@code long[][]} array is assumed to be square, i.e. all second-order arrays 
     * should be of the same length, and the length of the first such array will be assumed to be 
     * equal to the number of columns in the matrix.
     * 
     * @param ps      A print stream to write data to.
     * @param data    A {@code long[][]} instance with matrix data.
     * @param rows    A list of row ids of the same length as entries in the first order array.
     * @param cols    A list of col ids of the same length as the first second-order array.
     * @param idTop   A string to use as row id header (i.e. the 0,0 entry in the resulting file).
     * 
     * @throws IOException 
     */
    public static void write(
        Writer ps, long[][] data, List<String> rows, List<String> cols, String idTop
    ) throws IOException {
        write( ps, data, rows, cols, idTop, ROW_SEP, COL_SEP, MISSING, HEADER );
    }
    
    /**
     * Convenience method to write long integer array matrices.
     * 
     * The given {@code long[][]} array is assumed to be square, i.e. all second-order arrays 
     * should be of the same length, and the length of the first such array will be assumed to be 
     * equal to the number of columns in the matrix.
     * 
     * @param ps      A print stream to write data to.
     * @param data    A {@code long[][]} instance with matrix data.
     * @param rows    A list of row ids of the same length as entries in the first order array.
     * @param cols    A list of col ids of the same length as the first second-order array.
     * @param idTop   A string to use as row id header (i.e. the 0,0 entry in the resulting file).
     * @param rowSep  A row separator string. Use newlines.
     * @param colSep  A column separator string. Use something uncommon.
     * @param missing A string for missing values.
     * @param header  Include a header row?
     * 
     * @throws IOException 
     */
    public static void write( 
        Writer ps, long[][] data, List<String> rows, List<String> cols, String idTop, 
        String rowSep, String colSep, String missing, boolean header
    ) throws IOException {
        if( rows.size() != data.length || cols.size() != data[0].length ) {
            throw new IllegalArgumentException();
        }
        
        if( header ) {
            List<String> head = new ArrayList<>();
            head.add( idTop );
            head.addAll( cols );
            ps.append( String.join( colSep, head.toArray( new String[head.size()] ) ) );
            ps.append( rowSep );
        }
        
        for( int i = 0; i < data.length; i++ ) {
            ps.append( rows.get( i ) );
            ps.append( colSep );
            StringJoiner sj = new StringJoiner( colSep );
            for( long x : data[i] ) {
                sj.add( Long.toString( x ) );
            }
            ps.append( sj.toString() );
            ps.append( rowSep );
        }
        ps.close();
    }
      
    private static <C> String makeHead( String idCol, SortedSet<C> dataCols,
        Function<C,String> cCodec, String rowSep, String colSep 
    ) {
        StringBuilder sb = new StringBuilder();
        
        sb.append( idCol );
        for( C c : dataCols ) {
            sb.append( colSep );
            sb.append( cCodec.apply( c ) );
        }
        sb.append( rowSep );
        
        return sb.toString();
    }
    
    private static <R,C,V> String makeRow( R row, SortedSet<C> cols, Map<C,V> values,
        Function<R,String> rCodec, Function<V,String> vCodec,
        String rowSep, String colSep, String missing 
    ) {
        StringBuilder sb = new StringBuilder();
        
        sb.append( rCodec.apply( row ) );
        for( C c : cols ) {
            sb.append( colSep );
            V v = values.get( c );
            sb.append( v == null ? missing : vCodec.apply( v ) );
        }
        sb.append( rowSep );
        
        return sb.toString();
    }
    
    /**
     * Interface for an object that is capable of extracting data contained in some internal 
     * representation D in order to produce the tabular structure required by tabular data dumpers 
     * like {@link DSVWriter}.
     * 
     * @param <D> Type for data container
     * @param <R> Type for row keys
     * @param <C> Type for column keys
     * @param <V> Type for data values
     * 
 * @author José Tomás Atria <jtatria@gmail.com>
     */
    public static interface Accesor<D,R extends Comparable<R>,C extends Comparable<C>,V> {
        /**
         * Produce a SortedSet of row keys from the given internal data container.
         * @param data Some data
         * @return A sorted set of row keys.
         */
        public SortedSet<R> rows( D data );
        
        /**
         * Produce a SortedSet of column keys from the given data container.
         * @param data Some data
         * @return A sorted set of column keys.
         */
        public SortedSet<C> cols( D data );
        
        /**
         * Extract data values from the given internal data representation.
         * 
         * The keys in the returned map should correspond to the keys produced by calls to 
         * {@link #cols(java.lang.Object) } on the same data set.
         * 
         * @param data  Some data
         * @param r A row key, if necessary
         * @return A map containing values for the different columns.
         */
        public Map<C,V> values( D data, R r );
        
        /**
         * Advanced: provide a custom function for turning row keys into strings.
         * 
         * If not implemented, keys will be serialized via their {@code toString( r )} method.
         * 
         * @return An R -> String function.
         */
        default public Function<R,String> rowFunc() {
            return ( o ) -> o.toString();
        }
        
        /**
         * Advanced: provide a custom function for turning column keys into strings.
         * 
         * If not implemented, keys will be serialized via their {@code toString( c )} method.
         * 
         * @return A C -> String function.
         */
        default public Function<C,String> colFunc() {
            return ( o ) -> o.toString();
        }
        
        /**
         * Advanced: provide a custom function for turning data values into strings.
         * 
         * If not implemented, keys will be serialized via their {@code toString( v )} method.
         * 
         * @return A V -> String function.
         */
        default public Function<V,String> valFunc() {
            return ( o ) -> o.toString();
        }
    }

    /**
     * Acesor implementation for long[][] matrices.
     * @param <R> Type for row headers.
     * @param <C> Type for column headers.
     */ 
    public static class LongMatrixAccesor<R extends Comparable<R>,C extends Comparable<C>>
    implements Accesor<long[][],R,C,Long> {
        private final IntFunction<R> rowMap;
        private final IntFunction<C> colMap;
        private final Map<R,Integer> index = new HashMap<>();
        
        public LongMatrixAccesor( IntFunction<R> rowMap, IntFunction<C> colMap ) {
            this.rowMap = rowMap;
            this.colMap = colMap;
        }
        
        @Override
        public SortedSet<R> rows( long[][] data ) {
            SortedSet<R> rows = new TreeSet<>();
            for( int i = 0; i < data.length; i++ ) {
                R row = rowMap.apply( i );
                rows.add( row );
                index.put( row, i );
            }
            return rows;
        }

        @Override
        public SortedSet<C> cols( long[][] data ) {
            SortedSet<C> cols = new TreeSet<>();
            for( int i = 0; i < data[0].length; i++ ) {
                cols.add( colMap.apply( i ) );
            }
            return cols;
        }

        @Override
        public Map<C,Long> values( long[][] data, R r ) {
            Map<C,Long> out = new HashMap<>();
            for( int j = 0; j < data[index.get( r )].length; j++ ) {
                out.put( colMap.apply( j ), data[index.get( r )][j]);
            }
            return out;
        }
    }
    
    public static class DoubleMatrixAccesor<R extends Comparable<R>,C extends Comparable<C>>
    implements Accesor<double[][],R,C,Double> {
        private final IntFunction<R> rowMap;
        private final IntFunction<C> colMap;
        private final Map<R,Integer> index = new HashMap<>();

        public DoubleMatrixAccesor( IntFunction<R> rowMap, IntFunction<C> colMap ) {
            this.rowMap = rowMap;
            this.colMap = colMap;
        }

        @Override
        public SortedSet<R> rows( double[][] data ) {
            SortedSet<R> rows = new TreeSet<>();
            for( int i = 0; i < data.length; i++ ) {
                rows.add( rowMap.apply( i ) );
            }
            return rows;
        }

        @Override
        public SortedSet<C> cols( double[][] data ) {
            SortedSet<C> cols = new TreeSet<>();
            for( int i = 0; i < data[0].length; i++ ) {
                cols.add( colMap.apply( i ) );
            }
            return cols;
        }

        @Override
        public Map<C,Double> values( double[][] data, R r ) {
            Map<C,Double> out = new HashMap<>();
            for( int j = 0; j < data[ index.get( r ) ].length; j++ ) {
                out.put( colMap.apply( j ), data[index.get( r )][j]);
            }
            return out;
        }
    }
    
    /**
     * Accesor implementation for Maps of Maps.
     * 
     * All string conversions are carried out with each type's @{code toString()} methods.
     * 
     * @param <R>   Type for row keys.
     * @param <C>   Type for col keys.
     * @param <V>   Type for values.
     */
    public static class MapAccesor<R extends Comparable<R>,C extends Comparable<C>,V>
    implements Accesor<Map<R,Map<C,V>>,R,C,V> {
        
        @Override
        public SortedSet<R> rows( Map<R,Map<C,V>> data ) {
            return new TreeSet<>( data.keySet() );
        }

        @Override
        public SortedSet<C> cols( Map<R,Map<C,V>> data ) {
            SortedSet<C> tmp = new TreeSet<>();
            for( R r : data.keySet() ) {
                tmp.addAll( data.get( r ).keySet() );
            }
            return tmp;
        }

        @Override
        public Map<C,V> values( Map<R, Map<C, V>> data, R r ) {
            return data.get( r );
        }
    }
    
    /** 
     * Accesor implementation for Guava tables.
     * 
     * @param <R> Row keys.
     * @param <C> Column keys.
     * @param <V> Data entries.
     */
    public static class TableAccesor<R extends Comparable<R>,C extends Comparable<C>,V>
    implements Accesor<Table<R,C,V>,R,C,V> {
        @Override
        public SortedSet<R> rows( Table<R,C,V> data ) {
            return new TreeSet<>( data.rowKeySet() );
        }

        @Override
        public SortedSet<C> cols( Table<R, C, V> data ) {
            return new TreeSet<>( data.columnKeySet() );
        }

        @Override
        public Map<C,V> values( Table<R,C,V> data, R r ) {
            return data.row( r );
        }
    } 
    
    /**
     * Accesor implementation for Trove {@link TIntObjectMap }
     */
    public static class TIntIntLongAccesor
    implements Accesor<TIntObjectMap<TIntLongMap>,Integer,Integer,Long> {
        @Override
        public SortedSet<Integer> rows( TIntObjectMap<TIntLongMap> data ) {
            SortedSet<Integer> tmp = new TreeSet<>();
            for( int i : data.keys() ) {
                tmp.add( i );
            }
            return tmp;
        }

        @Override
        public SortedSet<Integer> cols( TIntObjectMap<TIntLongMap> data ) {
            SortedSet<Integer> tmp = new TreeSet<>();
            data.forEachValue( ( TIntLongMap row ) -> {
                row.forEachKey( ( int a ) -> tmp.add( a ) );
                return true;
            });
            return tmp;
        }

        @Override
        public Map<Integer,Long> values( TIntObjectMap<TIntLongMap> data, Integer r ) {
            Map<Integer,Long> tmp = new HashMap<>();
            TIntLongMap row = data.get( r );
            row.forEachEntry( ( int a, long b ) -> {
                tmp.put( a, b );
                return true;
            } );
            return tmp;
        }
    }
    
    /**
     * Accesor implementation for Trove {@link TIntObjectMap}
     */
    public static class TIntIntDoubleAccesor
    implements Accesor<TIntObjectMap<TIntDoubleMap>,Integer,Integer,Double> {
        @Override
        public SortedSet<Integer> rows( TIntObjectMap<TIntDoubleMap> data ) {
            SortedSet<Integer> tmp = new TreeSet<>();
            for( int i : data.keys() ) {
                tmp.add( i );
            }
            return tmp;
        }

        @Override
        public SortedSet<Integer> cols( TIntObjectMap<TIntDoubleMap> data ) {
            SortedSet<Integer> tmp = new TreeSet<>();
            data.forEachValue( ( TIntDoubleMap row ) -> {
                row.forEachKey( ( int a ) -> tmp.add( a ) );
                return true;
            });
            return tmp;
        }

        @Override
        public Map<Integer,Double> values( TIntObjectMap<TIntDoubleMap> data, Integer r ) {
            Map<Integer,Double> tmp = new HashMap<>();
            TIntDoubleMap row = data.get( r );
            row.forEachEntry( ( int a, double b ) -> {
                tmp.put( a, b );
                return true;
            } );
            return tmp;
        }
    }
}
