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
//import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.SparseFixedBitSet;

/**
 * BitSet based implementation of a document set, designed to work seamlessly with Lucene's search 
 * results and postings iterators.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class DocSet implements Predicate<Integer>, Iterable<Integer> {
    
    private final BitSet bs;
    private int size = -1;
 
    /**
     * Create a new document set with the given maxDocs capacity, including all documents contained 
     * in the given {@link TopDocs} instance.
     * @param hits A {@link TopDocs} with results from a query.
     * @param maxDoc The maximum capacity of this DocSet. Typically the total number of docs in a 
     * corpus.
     */
    public DocSet( TopDocs hits, int maxDoc ) {
        if( ( (double) hits.totalHits / maxDoc ) > .5 ) {
            this.bs = new FixedBitSet( maxDoc );
        } else {
            this.bs = new SparseFixedBitSet( maxDoc );
        }
        for( ScoreDoc sd : hits.scoreDocs ) {
            this.bs.set( sd.doc );
        }
    }
    
    /** Create an empty DocSet of the given total capacity.
     * @param maxDoc The maximum capacity of this DocSet. Typically the total number of docs in a 
     * corpus
     */
    public DocSet( int maxDoc ) {
        this( maxDoc, true );
    }
    
    /** Create an empty DocSet of the given total capacity.
     * @param maxDoc The maximum capacity of this DocSet. Typically the total number of docs in a 
     * @param sparse If {@code true}, force this DocSet to use a sparse bitset.
     */
    public DocSet( int maxDoc, boolean sparse ) {
        this.bs = sparse ? new SparseFixedBitSet( maxDoc ) : new FixedBitSet( maxDoc );
    }

    /**
     * Add the given document number to this DocSet.
     * @param doc A document number.
     */
    public void add( int doc ) {
        this.bs.set( doc );
        this.size = -1;
    }

    /**
     * Remove the given document number from this DocSet.
     * @param doc A document number.
     */
    public void remove( int doc ) {
        this.bs.clear( doc );
        this.size = -1;
    }

    /**
     * Add all documents from the given {@link DocIdSetIterator} instance.
     * 
     * The state of the iterator after this is method is undefined.
     * 
     * @param docs A {@link DocIdSetIterator} instance.
     * @throws IOException
     */
    public void add( DocIdSetIterator docs ) throws IOException {
        if( docs == null ) return;
        this.bs.or( docs );
        this.size = -1;
    }

    /**
     * Produce a {@link PostingsEnum} instance containing only the documents in the given 
     * {@link PostingsEnum} instance that are also contained in this DocSet.
     * 
     * @param docs A {@link PostingsEnum} instance.
     * @return A {@link PostingsEnum} instance containing only documents already in this DocSet
     */
    public PostingsEnum filter( PostingsEnum docs ) {
        return new FilteredPostingsEnum( docs, this.bs );
    }

    /**
     * Advanced: Get a reference to this DocSet's underlying {@link BitSet}.
     * @return A reference to this DocSet's {@link BitSet}.
     */
    public BitSet bits() {
        return this.bs;
    }
    
    /** 
     * Get an estimate of the size of this DocSet, i.e. its cardinality; the number of elements 
     * contained in it.
     * 
     * NB: NOT the total capacity, equal to the maximum allowed document.
     * 
     * @return The number of documents that are contained in this DocSet.
     */
    public int size() {
        this.size = this.size == -1 ? this.bits().cardinality() : this.size;
        return this.size;
    }
    
    /**
     * Create an integer array containing the document numbers of documents contained in this 
     * DocSet.
     * 
     * This will create a new array and copy all document numbers.
     * 
     * @return An integer array with all entries in this DocSet.
     */
    public int[] vector() {
        int[] out = new int[size()];
        int i = 0;
        for( int k : this ) {
            out[i++] = k;
        }
        return out;
    }
    
    @Override
    public boolean test( Integer t ) {
        return this.bs.get( t );
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int cur = -1;

            @Override
            public boolean hasNext() {
                return bs.nextSetBit( cur + 1 ) != PostingsEnum.NO_MORE_DOCS;
            }

            @Override
            public Integer next() {
                int nsb = bs.nextSetBit( cur + 1 );
                if( nsb == PostingsEnum.NO_MORE_DOCS ) throw new NoSuchElementException();
                cur = nsb;
                return cur;
            }
        };
    }

    // TODO: remove pending api
    public DocSet intersect( DocSet oth ) {
        return intersect( this, oth );
    }
    
    public DocSet complement() {
        return complement( this );
    }
    
    public static DocSet complement( DocSet src ) {
        DocSet tgt = new DocSet( src.bs.length() );
        for( int d : src ) {
            if( !src.test( d ) ) tgt.add( d );
        }
        return tgt;
    }
    
    // TODO: add copy constructor for non-destructive set operations.
    public static DocSet intersect( DocSet ds1, DocSet ds2 ) {
        DocSet tgt = ds1.size() > ds2.size() ? ds2 : ds1;
        DocSet src = tgt == ds1 ? ds2 : ds1;
        for( int d : tgt ) {
            if( !src.test( d ) ) tgt.remove( d );
        }
        return tgt;
    }
    
    public class FilteredPostingsEnum extends PostingsEnum {

        private final BitSet bs;
        private final PostingsEnum src;

        private int doc = -1;

        public FilteredPostingsEnum( PostingsEnum pEnum, BitSet bs ) {
            this.src = pEnum;
            this.bs = bs;
        }

        @Override
        public int docID() {
            return doc;
        }

        @Override
        public int nextDoc() throws IOException {
            return advance( doc + 1 );
        }

        // TODO: profile and optimize.
        @Override
        public int advance( int target ) throws IOException {
            while( doc != PostingsEnum.NO_MORE_DOCS ) {
                // get next bit.
                int nb = bs.nextSetBit( target );
                if( nb == PostingsEnum.NO_MORE_DOCS ) {
                    doc = PostingsEnum.NO_MORE_DOCS;
                    break;
                }

                // get next doc
                int nd = src.advance( nb );
                if( nd > bs.length() ) {
                    doc = PostingsEnum.NO_MORE_DOCS;
                    break;
                }

                // if next doc is bit, return
                if( bs.get( nd ) ) {
                    doc = nd;
                    break;
                } else { // search from next bit.
                    target = ++nd;
                }
            }
            return doc;
        }

        // NOTE Advance logic for java bitsets, which use different sentinels than lucene bitsets.
        // NOTE Left here for future reference; don't remove!
//        @Override
//        public int advance( int target ) throws IOException {
//            while( doc != PostingsEnum.NO_MORE_DOCS ) {
//
//                // Check target is within bounds and not overflowed
//                if( target < 0 || target >= bs.length() ) {
//                    doc = PostingsEnum.NO_MORE_DOCS;
//                    break;
//                }
//
//                // Get next set bit from target
//                int nsb = bs.nextSetBit( target );
//                if( nsb != -1 ) { // bit found. advance src to next equal or larger than nbs
//                    int np = src.advance( nsb - 1 );
//                    // next doc equal or larger than nbs is not set, search again from one larger
//                    if( !bs.get( np ) ) {
//                        target = np + 1;
//                    } else { // match! set and bail.
//                        doc = np;
//                        break;
//                    }
//                } else { // no more set bits.
//                    doc = PostingsEnum.NO_MORE_DOCS;
//                }
//            }
//            return doc;
//        }

        @Override public long cost() {
            return bs.cardinality();
        }

        @Override public int freq() throws IOException { return src.freq(); }
        @Override public int nextPosition() throws IOException { return src.nextPosition(); }
        @Override public int startOffset() throws IOException { return src.startOffset(); }
        @Override public int endOffset() throws IOException { return src.endOffset(); }
        @Override public BytesRef getPayload() throws IOException { return src.getPayload(); }
    }
}
