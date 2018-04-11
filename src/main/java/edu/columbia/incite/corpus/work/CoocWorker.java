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
package edu.columbia.incite.corpus.work;

import edu.columbia.incite.util.SparseMatrix;
import edu.columbia.incite.corpus.Lexicon;

import com.google.common.base.Stopwatch;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;

import edu.columbia.incite.run.Logs;
import edu.columbia.incite.run.Status;
import edu.columbia.incite.run.Progress;


/**
 * Co-occurrence counts
 * 
 * This worker builds a co-occurrence matrix for the terms in a {@link Lexicon}. It works 
 * on a per-document basis, to collect all cooc counts from a set of documents and produces a 
 * {@link SparseMatrix} with cooc values for the terms in the lexicon found in the sampled 
 * documents.
 * 
 * It is thread-safe, in that the {@link Runnable} tasks it produces can be executed in parallel.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class CoocWorker {
    
    /** String used in progress reports. **/
    public static final String NAME = "Counting co-occurrences";
    
    // data objects
    private final ThreadLocal<SparseMatrix> tlMatrix = ThreadLocal.withInitial( () -> initData() );
    private final List<SparseMatrix> matrices = new ArrayList<>();
    
    // parameters
    private final int wPre;
    private final int wPos;
    
    // helper objects
    private final LeafReader indx;
    private final Lexicon    lxcn;
    private final Status   prog;
    
    /**
     * Create a new worker for the given lexicon using data from the given index to count 
     * co-occurrences in a window with the given pre and pos widths.
     * 
     * @param lxcn A corpus' {@link Lexicon}.
     * @param ir    An (atomic) index reader.
     * @param pre   The size of the window before each context word.
     * @param pos   The size of the window after each context word.
     */
    public CoocWorker( Lexicon lxcn, LeafReader ir, int pre, int pos ) {
        this( lxcn, ir, pre, pos, null );
    }
    
    /**
     * Create a new worker for the given lexicon using data from the given index to count 
     * co-occurrences in a window with the given pre and pos widths, and report progress status 
     * in the given out.
     * 
     * @param lxcn A corpus' {@link Lexicon}.
     * @param ir    An (atomic) index reader.
     * @param pre   The size of the window before each context word.
     * @param pos   The size of the window after each context word.
     * @param out   A {@link Progress} object to report work.
     */
    public CoocWorker( Lexicon lxcn, LeafReader ir, int pre, int pos, Progress out ) {
        this.wPre = pre;
        this.wPos = pos;
        this.indx = ir;
        this.lxcn = lxcn;
        this.prog = out == null ? Status.make( NAME ) : new Status( NAME, out );
    }

    /**
     * Produce a runnable task for this worker.
     * 
     * The returned task will collect cooc counts from the given document when executed, updating 
     * counts in this worker's data.
     * 
     * @param doc A document number
     * @return A {@link Runnable} object with the work corresponding to the given document.
     */
    public Runnable work( final int doc ) {
        prog.add();
        return () -> {
            try {
                Terms tv = indx.getTermVector( doc, lxcn.field() );
                if( tv != null ) {
                    TIntIntMap wrk = new TIntIntHashMap();
                    PostingsEnum p = null;
                    int max = 0;
                    TermsEnum tEnum = lxcn.filter( tv );
                    // Collect position info for each term
                    while( tEnum.next() != null ) {
                        p = tEnum.postings( p, PostingsEnum.POSITIONS );
                        while( p.nextDoc() != PostingsEnum.NO_MORE_DOCS ) {
                            int f = p.freq();
                            for( int i = 0; i < f; i++ ) {
                                int pos = p.nextPosition();
                                max = pos > max ? pos : max; // record max position
                                wrk.put( pos, lxcn.getIndex( tEnum.term() ) );
                            }
                        }
                    }
                    
                    // Naive implementation.
                    for( int i = 0; i <= max; i++ ) {
                        if( !wrk.containsKey( i ) ) continue;
                        int lo = ( i - wPre < 0 ) ? 0 : i - wPre;
                        int hi = ( i + wPos > max ) ? max : i + wPos;
                        for( int j = lo; j <= hi; j++ ) {
                            if( i == j ) continue;
                            if( !wrk.containsKey( j ) ) continue; // j was a filtered term.
                            addDelta( wrk.get( i ), i, wrk.get( j ), j );
                        }
                    }
                    
                }
            } catch( IOException ex ) {
                Logger.getLogger( "" ).log( Level.SEVERE, null, ex );
            }
            prog.update();
        };
    }

    private void addDelta( int pre_i, int pre_p, int pos_i, int pos_p ) {
        //            double delta = lxcn.weight( pre_i, pre_p, pos_i, pos_p ); // TODO
        double delta = 1d / Math.abs( pre_p - pos_p );
        update( pre_i, pos_i, delta );
    }

    private void update( int i, int j, double d ) {
        tlMatrix.get().update( i, j, d );
    }

    private SparseMatrix initData() {
        SparseMatrix m = new SparseMatrix();
        synchronized( matrices ) {
            matrices.add( m );
        }
        return m;
    }

    /**
     * Get this worker's results.
     * 
     * Produces a {@link SparseMatrix} instance with cooccurrence counts for all processed 
     * documents.
     *
     * It is not recommended to call this method until all tasks produced by this worker have been 
     * executed, as this method will reap all internal data storages and merge them into one data 
     * set. Updates during this process may corrupt data or get lost.
     * 
     * After this method returns, additional work can be submitted and executed, but the results 
     * of these will be accumulated from scratch.
     * 
     * @return A {@link SparseMatrix} with cooc counts from all documents processed so far.
     */
    public SparseMatrix data() {
        Logs.infof( "%s: merging %d partial datasets", NAME, matrices.size() );
        Stopwatch sw = Stopwatch.createUnstarted();
        int i = 0;
        SparseMatrix out = matrices.get( i++ );
        sw.start();
        for( ; i < matrices.size(); i++ ) {
            SparseMatrix m = matrices.get( i );
            out.merge( m );
            matrices.set( i, null );
        }
        matrices.clear();
        sw.stop();
        Logs.infof( "%s: datasets merged in %d seconds", NAME, sw.elapsed( TimeUnit.SECONDS ) );
        return out;
    }

    public void report() {
        this.prog.report();
    }

    public long[] status() {
        return this.prog.status();
    }
    
}
