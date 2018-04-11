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


import edu.columbia.incite.corpus.POSClass;
import edu.columbia.incite.util.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.util.BytesRef;

import edu.columbia.incite.corpus.Lexicon;
import edu.columbia.incite.run.Status;
import edu.columbia.incite.run.Progress;
import edu.columbia.incite.util.DSVWriter;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class POSCWorker {
    
    public static final String NAME = "Counting POS tags";
    // data objects
    private final long[][] data;
    
    // helper objects
    private final Lexicon lxcn;
    private final Status prog;
    
    public POSCWorker( Lexicon lxcn ) {
        this( lxcn, null );
    }
    
    public POSCWorker( Lexicon lxcn, Progress prog ) {
        this.data = new long[ lxcn.size() ][ POSClass.values().length ];
        this.lxcn = lxcn;
        this.prog = prog == null ? Status.make( NAME ) : new Status( NAME, prog );
    }
    
    public int flags() {
        return PostingsEnum.ALL;
    }
    
    public Runnable work( final BytesRef term, final PostingsEnum pEnum ) {
        prog.add();
        return () -> {
            try {
                int row = lxcn.getIndex( term );
                while( pEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS ) {
                    int freq = pEnum.freq();
                    for( int i = 0; i < freq; i++ ) {
                        pEnum.nextPosition();
                        POSClass pos = POSClass.getPOSClass( pEnum.getPayload() ); 
                        data[row][pos.ordinal()]++;
                    }
                }
                prog.update();
            } catch( IOException ex ) {
                Logger.getLogger(POSCWorker.class.getName() ).log( Level.SEVERE, null, ex );
            }
        };
    }
    
    public long[][] data() {
        return this.data;
    }
    
    public static void write( Lexicon lxcn, long[][] data, Path file ) throws IOException {
        List<String> rows = Arrays.asList( lxcn.terms() );
        List<String> cols = new ArrayList<>();
        for( POSClass pos : POSClass.values() ) {
            cols.add( pos.toString() );
        }
        DSVWriter.write( FileUtils.getWriter( file ), data, rows, cols, Lexicon.TERM_ID );
    }
    
    public void report() {
        this.prog.report();
    }
    
    public long[] status() {
        return this.prog.status();
    }
}
