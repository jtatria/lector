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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import com.google.common.collect.ImmutableSortedMap;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CompiledAutomaton;

//import edu.columbia.incite.obo.OBOConf;

import edu.columbia.incite.corpus.Lexicon.Word;
import edu.columbia.incite.util.FileUtils;
import edu.columbia.incite.util.DSVWriter;
import edu.columbia.incite.util.DSVWriter.Accesor;

/**
 * Canonical lexical set for a corpus.
 * 
 * Lexical sets define the term population for a corpus. This implementation assumes that 
 * term-level transformations have been carried out at index time (via different {@link Tokenizer} 
 * implementations).
 * 
 * Lexicon instances are thus constructed over a given field that is assumed to contain a 
 * normalized token stream, using a single parameter for minimum term frequency.
 * 
 * Instances of this class retain corpus-wide statistics for terms (mainly term and document 
 * frequency), and associate each term's internal string representation to their canonical 
 * representation as UTF8 byte sequences.
 * 
 * Terms in the Lexicon are sorted in descending term frequency and <em>not</em> lexicographically. 
 * 
 * THIS CANONICAL ORDER IS USED THROUGHOUT ALL CORPUS DATASETS IN THIS PACKAGE. Most notably, a 
 * term's index number in this canonical ordering determines its index in all 
 * cooccurrence matrices; these are generally sparse, so inconsistencies in order/indexes will make 
 * it impossible to read them.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Lexicon implements Iterable<Word> {
    
    /** Default field name for term id in all datasets **/
    public static final String TERM_ID = "_term_";
    
    /** Index field over which this lexicon is constructed **/
    public final String field;    
    /** Minimum term frequency **/
    public final int minFreq;
    
    /** Total field term frequency (total number of tokens in the underlying field) **/
    public final long   uFreq;
    /** Total lexicon term frequency (the number of tokens pointing to terms in the lexicon) **/
    public final long   nFreq;
    /** Lexicon coverage (nFreq / uFreq) **/
    public final double cover;    
    /** Lexicon cardinality **/
    public final int    size;

    private final LeafReader ir;
    
    private final BiMap<Integer,BytesRef>  ind2trms;
    private final SortedMap<Integer,Word>  ind2wrds;
    private final SortedMap<BytesRef,Word> trm2wrds;
    
    private final CompiledAutomaton cau;

    /**
     * Construct a new lexicon over the given field in the given index with the given frequency 
     * threshold.
     * 
     * @param ir An atomic {@link LeafReader}.
     * @param field Index field name.
     * @param minFreq Minimum term frequency
     * @throws IOException 
     */
    public Lexicon( LeafReader ir, String field, int minFreq ) throws IOException {
        this.ir      = ir;
        this.field   = field;
        this.minFreq = minFreq;
        
        Terms terms = ir.terms( field );
        this.uFreq = terms.getSumTotalTermFreq();
        
        // Collect terms with frequency above threshold.
        TermsEnum tEnum = terms.iterator();
        SortedSet<Word> tmp = new TreeSet<>();
        long ttf = 0;
        while( tEnum.next() != null ) {
            long tf = tEnum.totalTermFreq();
            long df = tEnum.docFreq();
            if( tf >= minFreq ) {
                ttf += tf;
                tmp.add( new Word( tEnum.term(), tf, df ) );
            } else {
                // TODO: do something with OOL terms?
            }
        }
        // Save coverage statistics
        this.nFreq = ttf;
        this.cover = (double) nFreq / (double) uFreq;
        
        // Build internal maps
        BiMap<Integer,BytesRef>  ind_tmp = HashBiMap.create();
        SortedMap<Integer,Word>  iwd_tmp = new TreeMap<>();
        SortedMap<BytesRef,Word> kwd_tmp = new TreeMap<>();
        
        int key = 0;
        for( Word w : tmp ) {
            ind_tmp.put( key, w.term );
            iwd_tmp.put( key, w );
            kwd_tmp.put( w.term, w );
            key++;
        }
        this.size = key;
        
        this.ind2trms = ImmutableBiMap.copyOf( ind_tmp );
        this.ind2wrds = ImmutableSortedMap.copyOf( iwd_tmp );
        this.trm2wrds = ImmutableSortedMap.copyOf( kwd_tmp );
        
        // Compile membership lexicon
        Automaton au = Automata.makeStringUnion( this.trm2wrds.keySet() );
        this.cau = new CompiledAutomaton( au );
    }
    
    /**
     * This lexicon's cardinality.
     * @return The number of terms in the lexicon.
     */
    public int size() {
        return this.size;
    }
    
    /**
     * Minimum term frequency in this lexicon.
     * @return The term frequency threshold for this lexicon.
     */
    public int minFreq() {
        return this.minFreq;
    }
    
    /** Lexicon coverage.
     * @return Proportion of tokens covered by the terms in this lexicon.
     */
    public double cover() {
        return this.cover;
    }
    
    /** 
     * Lexicon field name
     * @return The name for the field over which this lexicon was constructed.
     */
    public String field() {
        return this.field;
    }
    
    /**
     * All terms in the lexicon.
     * 
     * The canonical array.
     * 
     * @return An array of term strings, sorted in descending term frequency order.
     */
    public String[] terms() {
        String[] out = new String[ this.size ];
        for( Integer i : this.ind2trms.keySet() ) {
            out[i] = this.ind2trms.get( i ).utf8ToString();
        }
        return out;
    }
    
    /**
     * {@code true} if this Lexicon contains the given term
     * @param term A term's {@link BytesRef} representation.
     * @return {@code true} iff the given term is contained in this lexicon.
     */
    public boolean contains( BytesRef term ) {
        return cau.runAutomaton.run( term.bytes, term.offset, term.length );
    }
    
    /**
     * Term index.
     * @param term A term's {@link BytesRef} representation.
     * @return The index of the given term in this lexicon.
     */
    public int getIndex( BytesRef term ) {
        return ind2trms.inverse().get( term );
    }
    
    /**
     * Term at index.
     * @param idx An integer index.
     * @return The term at the position of the given index in this lexicon.
     */
    public String getTerm( int idx ) {
        return ind2wrds.get( idx ).toString();
    }
    
    /**
     * Word at index.
     * @param idx An integer index.
     * @return The {@link Word} for the term at the position of the given index in this lexicon.
     */
    public Word getWord( int idx ) {
        return ind2wrds.get( idx );
    }
    
    /**
     * Filter term enumeration.
     * Filter the given {@link Terms} instance to exclude terms not contained in this lexicon.
     * @param src A field's {@link Terms}.
     * @return A {@link TermsEnum} instance containing only terms in the given terms object that 
     * are also contained in this lexicon.
     * @throws IOException 
     */
    public TermsEnum filter( Terms src ) throws IOException {
        return src.intersect( cau, null );
    }

    /**
     * Generalized positional weights
     * This function produces a suitable weighting factor between two token positions taking into 
     * account both the distance between the tokens and the weights associated to the term's 
     * at both positions.
     * 
     * TODO: implement. The idea is to remove positional weighting from the cooccurrence worker to 
     * further encapsulate parameters.
     * 
     * NB: NOT IMPLEMENTED YET.
     * 
     * @param pre_i Term index for the leading token.
     * @param pre_p Position for the leading token.
     * @param pos_i Term index for the trailing token.
     * @param pos_p Position of the trailing token.
     * @return A weighting factor between the two token positions.
     * @throws UnsupportedOperationException, always
     */
    public double weight( int pre_i, int pre_p, int pos_i, int pos_p ) {
        throw new UnsupportedOperationException( "Generalized weights not supported yet." );
    }
        
    /**
     * Obtain a copy of this lexicon as a set of three same-length arrays.
     * 
     * This is mostly a convenience method for passing data to external libraries consistently 
     * without disk serialization.
     * 
     * See {@link LxcnArrays} for details of the returned type.
     * 
     * @return A copy of this lexicon as a {@link LxcnArrays} instance.
     */
    public LxcnArrays arrays() {
        int n = size();
        String[] k = new String[n];
        long[] tf  = new long[n];
        long[] df  = new long[n];
        int i = 0;
        for( Word w : this ) {
            k[i]  = w.term.utf8ToString();
            tf[i] = w.tf;
            df[i] = w.df;
            i++;
        }
        return new LxcnArrays( k, tf, df );
    }
    
    @Override
    public Iterator<Word> iterator() {
        return new Iterator<Word>() {
            private Iterator<Map.Entry<Integer,Word>> it = ind2wrds.entrySet().iterator();
            
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Word next() {
                return it.next().getValue();
            }
        };
    }
    
    /**
     * Write a lexicon to disk in DSV format.
     * 
     * Lexicon instances are serialized in three columns, the first one containing the utf8-encoded 
     * term string, the second the total term frequency for each term and the third one the total 
     * document frequency for each term.
     * 
     * See {@link DSVWriter} for file format details.
     * 
     * @param lxcn  A lexicon
     * @param file  A file path.
     * @throws IOException 
     */
    public static void write( Lexicon lxcn, Path file ) throws IOException {        
        DSVWriter csv = new DSVWriter( new LxcnAccesor() ).rowIdHead( Lexicon.TERM_ID );
        try ( Writer w = FileUtils.getWriter( file ) ) {
            csv.write( lxcn.ind2wrds, w );
        }
    }
    
    /**
     * Word objects associate each term with their corpus-wide statistics as compiled in a given 
     * lexicon at construction time.
     */
    public class Word implements Comparable<Word> {
        private final BytesRef term;
        /** This word's term frequency **/
        public final long tf;
        /** This word's document frequncy **/
        public final long df;

        Word( BytesRef term, long tf, long df ) {
            this.term = BytesRef.deepCopyOf( term );
            this.tf = tf;
            this.df = df;
        }

        /**
         * Byte array representation.
         * This word's term as a utf8 byte array.
         * @return A {@link BytesRef} containing this word's term's bytes.
         */
        public BytesRef bytes() {
            return BytesRef.deepCopyOf( term );
        }
        
        /**
         * {@link Term} representation.
         * This word's term's as a search term.
         * @return A {@link Term} containing this word's term and field.
         */
        public Term term() {
            return new Term( field, term.utf8ToString() );
        }
        
        /**
         * Postings for this word's term in its lexicon's field.
         * @param ds A document set
         * @return A {@link PostingsEnum} with all postings for this word's term.
         * @throws IOException 
         */
        public PostingsEnum postings( DocSet ds ) throws IOException {
            return ds != null ? ds.filter( ir.postings( term() ) ) : ir.postings( term() );
        }
        
        @Override
        public int compareTo( Word o ) {
            int c = Long.compare( o.tf, this.tf );
            return c != 0 ? c : this.term.compareTo( o.term );
        }        
        
        @Override
        public String toString() {
            return term.utf8ToString();
        }
    }
    
    /**
     * Simple struct to hold the representation of a lexicon as a set of three same-length arrays.
     * 
     * Consider this to be a naive form of serialization of this lexicon's data.
     * 
     * Instances of this class can be used to pass lexicon data to external libraries and services 
     * consistently.
     */
    public static class LxcnArrays {
        /** Term array **/
        public final String[] terms;
        /** Term frequency array **/
        public final long[] tf;
        /** Document frequency array **/
        public final long[] df;
        
        /**
         * Create a new LxcnArrays instance from the given arrays
         * @param terms A lexicon's terms
         * @param tf A lexicon's tf array
         * @param df A lexicon's df array
        **/
        public LxcnArrays( String[] terms, long[] tf, long[] df ) {
            this.terms = terms;
            this.tf = tf;
            this.df = df;
        }
    }
    
    private static class LxcnAccesor implements Accesor<SortedMap<Integer,Word>,Word,String,Long> {

        public static final String TF_KEY = "tf";
        public static final String DF_KEY = "df";
        
        private final SortedSet<String> cols = new TreeSet<>();
        private final Map<String,Long> vals = new HashMap<>();
        
        @Override
        public SortedSet<Word> rows( SortedMap<Integer,Word> data ) {
            return new TreeSet<>( data.values() );
        }

        @Override
        public SortedSet<String> cols( SortedMap<Integer,Word> data ) {
            cols.clear();
            cols.add( "tf" );
            cols.add( "df" );
            return cols;
        }

        @Override
        public Map<String,Long> values( SortedMap<Integer,Word> data, Word r ) {
            vals.clear();
            vals.put( "tf", r.tf );
            vals.put( "df", r.df );
            return vals;
        }

        @Override
        public Function<Word,String> rowFunc() {
            return ( w ) -> w.term.utf8ToString();
        }

        @Override
        public Function<String,String> colFunc() {
            return ( s ) -> s;
        }

        @Override
        public Function<Long,String> valFunc() {
            return ( l ) -> l.toString();
        }
    }
}
