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

import edu.columbia.incite.corpus.Lexicon;
import edu.columbia.incite.corpus.DocMap;
import edu.columbia.incite.run.Status;
import edu.columbia.incite.util.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.util.BytesRef;

import edu.columbia.incite.run.Progress;
import edu.columbia.incite.util.DSVWriter;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
 public class FreqWorker {

     public static final String NAME = "Counting frequencies";
    // data objects
    private final long[][] data;

    // parameters
    public static final String NO_SPLIT_KEY = "total";

    // helper objects
    private final Lexicon lxcn;
    private final DocMap<String> splits;
    private final Status prog;
    
    public FreqWorker( Lexicon lxcn ) throws IOException {
        this( lxcn, null );
    }
    
    public FreqWorker( Lexicon lxcn, DocMap<String> splits ) {
        this( lxcn, splits, null );
    }
    
    public FreqWorker( Lexicon lxcn, DocMap<String> splits, Progress out ) {
        this.lxcn = lxcn;
        this.splits = splits;
        this.data = new long[ lxcn.size ][ splits == null ? 1 : splits.numOutputs() ];
        this.prog = out == null ? Status.make( NAME ) : new Status( NAME, out );
    }

    public int flags() {
        return PostingsEnum.FREQS;
    }
    
    public Runnable work( final BytesRef term, final PostingsEnum pEnum ) {
        prog.add();
        return () -> {
            try {
                int row = lxcn.getIndex( term );
                while( pEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS ) {
                    long freq = pEnum.freq();
                    int doc = pEnum.docID();
                    int col = (int) ( splits != null ? splits.outputKey( splits.get( doc ) ) : 0l );
                    data[row][col] += freq;
                }
                prog.update();
            } catch( IOException ex ) {
                Logger.getLogger(FreqWorker.class.getName() ).log( Level.SEVERE, null, ex );
            }
        };
    }
    
    public void report() {
        this.prog.report();
    }
    
    public long[] status() {
        return this.prog.status();
    }
    
    public long[][] data() {
        return this.data;
    }

    public static void write( Lexicon lxcn, long[][] data, DocMap<String> splits, Path file ) 
    throws IOException {
        List<String> rows = Arrays.asList( lxcn.terms() );
        BiMap<Long,String> map = splits.outputMap();
        List<String> cols = map.keySet().stream().sorted().map(
            ( l ) -> map.get( l )
        ).collect( Collectors.toList() );
        DSVWriter.write( FileUtils.getWriter( file ), data, rows, cols, Lexicon.TERM_ID );
    }
}
