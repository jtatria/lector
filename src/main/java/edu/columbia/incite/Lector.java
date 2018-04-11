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
package edu.columbia.incite;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CompiledAutomaton;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

import edu.columbia.incite.corpus.work.CoocWorker;
import edu.columbia.incite.corpus.work.FreqWorker;
import edu.columbia.incite.corpus.work.POSCWorker;
import edu.columbia.incite.corpus.DocMap;
import edu.columbia.incite.corpus.DocSet;
import edu.columbia.incite.corpus.Lexicon;
import edu.columbia.incite.util.SparseMatrix;
import edu.columbia.incite.run.CallbackListener;
import edu.columbia.incite.run.Progress;
import edu.columbia.incite.uima.util.ComponentFactory;

import static edu.columbia.incite.run.Logs.*;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public final class Lector {

    private final Map<String,DocMap<String>> mapCache = new HashMap<>();
        
    private Conf       conf;
    private LeafReader ir;
    private Lexicon    lxcn;
    private DocSet     sample;

    public Lector() throws IOException {
        this( new Conf() );
    }
    
    public Lector( Conf conf ) throws IOException {
        initLogs( conf );
        infof( "Incite Lector running from %s", conf.homeDir() );
        this.conf = conf;
    }
    
    public void dumpCorpusData() {
        try {
            this.dumpLexicon( this.lexicon() );
            long[][] freq = this.countFrequencies( docSample() );
            this.dumpFrequencies( freq );
            long[][] posc = this.countPOSTags( docSample() );
            this.dumpPOSCounts( posc );
            SparseMatrix cooc = this.countCooccurrences( docSample() );
            this.dumpCooccurrences( cooc );
        } catch( IOException ex ) {
            Logger.getLogger(Lector.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }
    
    public long[][] countFrequencies( DocSet ds ) throws IOException {
        return countFrequencies( ds, null );
    }
    
    public long[][] countFrequencies( DocSet ds, Progress prog ) throws IOException {
        DocMap<String> splits = mapField( this.conf.fieldSplit() );
        return countFrequencies( ds, splits, prog );
    }
    
    public long[][] countFrequencies( DocSet ds, DocMap splits, Progress prog ) throws IOException {
        FreqWorker wrkr = new FreqWorker( lexicon(), splits, prog );
        LeafReader lr = indexReader();
        TermsEnum tEnum = lexicon().filter( lr.terms( lexicon().field() ) );
        ExecutorService exec = Executors.newFixedThreadPool( this.conf.threads() );
        while( tEnum.next() != null ) {
            PostingsEnum pEnum = ds != null ?
                ds.filter( tEnum.postings( null, wrkr.flags() ) ) :
                tEnum.postings( null, wrkr.flags() );
            exec.submit( wrkr.work( BytesRef.deepCopyOf( tEnum.term() ), pEnum ) );
        }
        exec.shutdown();
        boolean quiet = conf.quiet();
        while( !exec.isTerminated() ) {
            if( !quiet ) wrkr.report();
        }
        if( !quiet ) wrkr.report();
        return wrkr.data();
    }

    public void dumpFrequencies( long[][] data ) throws IOException {
        Path file = this.conf.freqFile();
        infof( "Dumping frequencies to %s", file );
        FreqWorker.write( lexicon(), data, mapField( this.conf.fieldSplit() ), file );
    }
    
    public long[][] countPOSTags( DocSet ds ) throws IOException {
        ExecutorService exec = Executors.newFixedThreadPool( this.conf.threads() );
        POSCWorker wrkr = new POSCWorker( lexicon() );
        LeafReader lr = indexReader();
        TermsEnum tEnum = lexicon().filter( lr.terms( lexicon().field() ) );
        while( tEnum.next() != null ) {
            PostingsEnum pEnum = ds != null ?
                ds.filter( tEnum.postings( null, wrkr.flags() ) ) :
                tEnum.postings( null, wrkr.flags() );
            exec.submit( wrkr.work( BytesRef.deepCopyOf( tEnum.term() ), pEnum ) );
        }
        exec.shutdown();
        boolean quiet = conf.quiet();
        while( !exec.isTerminated() ) {
            if( !quiet ) wrkr.report();
        }
        if( !quiet ) wrkr.report();
        return wrkr.data();
    }

    public void dumpPOSCounts( long[][] data ) throws IOException {
        Path file = this.conf.poscFile();
        infof( "Dumping POS counts to %s", file );
        POSCWorker.write( lexicon(), data, file );
    }
    
    public SparseMatrix countCooccurrences( DocSet ds ) {
        return countCooccurrences( ds, null );
    }
    
    public SparseMatrix countCooccurrences( DocSet ds, Progress out ) {
        int wPre = this.conf.wPre();
        int wPos = this.conf.wPos();
        infof( "Counting coccurrences over %d documents with [ %d, %d ] windows", 
            ds.size(), wPre, wPos
        );
        CoocWorker wrkr = new CoocWorker(
            lexicon(), indexReader(), this.conf.wPre(), this.conf.wPos(), out
        );
        ExecutorService exec = Executors.newFixedThreadPool( this.conf.threads() );
        for( int i : ds ) {
            final int task = i;
            exec.execute( wrkr.work( task ) );
        }
        exec.shutdown();
        boolean quiet = conf.quiet();
        while( !exec.isTerminated() ) {
            if( !quiet ) wrkr.report();
        }
        if( !quiet ) wrkr.report();
        return wrkr.data();
    }

    public void dumpCooccurrences( SparseMatrix data ) throws IOException {
        infof( "Dumping cooccurrence counts to %s", this.conf.coocFile() );
        SparseMatrix.save( data, this.conf.coocFile() );
    }
    
    public DocSet makeDocSet( String field, String regex ) throws IOException {
        Automaton au = new RegExp( regex ).toAutomaton();
        DocSet ds = makeDocSet( field, au );
        return ds;
    }

    public DocSet makeDocSet( String field, String[] terms ) throws IOException {
        List<String> list = Arrays.asList( terms );
        Set<String> set = new HashSet<>( list );
        LeafReader lr = indexReader();
        if( set.isEmpty() ) {
            return new DocSet( lr.maxDoc() );
        } else if( set.size() == 1 ) {
            DocSet ds = new DocSet( lr.maxDoc() );
            ds.add( lr.postings( new Term( field, set.iterator().next() ) ) );
            return ds;
        }
        List<BytesRef> collect = set.stream().map(
            ( s ) -> new BytesRef( s.getBytes( StandardCharsets.UTF_8 ) )
        ).collect( Collectors.toList() );
        Collections.sort( collect );
        return makeDocSet( field, Automata.makeStringUnion( collect ) );
    }

    public DocSet makeDocSet( String field, Automaton term ) throws IOException {
        if( !checkField( field ) ) { // TODO smarter errors?
            errorf( "field %s not found", field );
            return null;
        }
        infof( "Building document set over field %s", field );
        CompiledAutomaton cau = new CompiledAutomaton( term );
        LeafReader lr = indexReader();
        TermsEnum tEnum = cau.getTermsEnum( lr.terms( field ) );
        DocSet ds = new DocSet( lr.maxDoc() );
        PostingsEnum reuse = null;
        while( tEnum.next() != null ) {
            reuse = tEnum.postings( reuse );
            ds.add( reuse );
        }
        infof( "Document set contains %d documents", ds.size() );
        return ds;
    }
    
//    // TODO: remove pending API
//    public DocSet getSample( String sample ) throws IOException {
//        infof( "Gathering documents for sample '%s'", sample );
//        DocSet ds = Samples.getSample( this.indexReader(), sample );
//        infof( "Sample '%s' contains %d documents", sample, ds.size() );
//        return ds;
//    }
//    
//    // TODO: remove pending API
//    public DocSet complement( String sample ) throws IOException {
//        infof( "Gathering documents for sample '%s' complement", sample );
//        DocSet ds = Samples.complement( this.indexReader(), sample );
//        infof( "Sample '%s' complement contains %d documents", sample, ds.size() );
//        return ds;
//    }
    
    // TODO remove pending API
    public DocSet docSample() {
        try {
            this.sample = this.sample == null ?
                makeDocSet( this.conf.fieldFilter(), this.conf.filterTerm() ) : this.sample;
        } catch( IOException ex ) {
            Logger.getLogger(Lector.class.getName() ).log( Level.SEVERE, null, ex );
        }
        return this.sample;
    }

    public DocMap<String> mapField( String field ) throws IOException {
        return this.mapCache.computeIfAbsent( field,
            ( f ) -> buildDocMap( this.indexReader(), f )
        );
    }
    
    public LeafReader indexReader() {
        try {
            this.ir = this.ir == null ? openIndex() : this.ir;
        } catch( IOException ex ) {
            Logger.getLogger(Lector.class.getName() ).log( Level.SEVERE, null, ex );
        }
        return this.ir;
    }
    
    public LeafReader openIndex() throws IOException {
        Path path = this.conf.indexDir();
        Directory dir = FSDirectory.open( path );
        return SlowCompositeReaderWrapper.wrap( DirectoryReader.open( dir ) );
    }

    public Lexicon lexicon() {
        try {
            this.lxcn = this.lxcn == null ? buildLexicon() : this.lxcn;
        } catch( IOException ex ) {
            Logger.getLogger(Lector.class.getName() ).log( Level.SEVERE, null, ex );
        }
        return this.lxcn;
    }
    
    public Lexicon buildLexicon() throws IOException {
        String field  = this.conf.fieldTxt();
        int minFrq    = this.conf.minTermFreq();
        infof( "Building lexicon over %s with minfreq %d", field, minFrq );
        Lexicon out  = new Lexicon( this.indexReader(), field, minFrq );
        infof( "Lexicon contains %d terms with a %4.2f%% coverage over the corpus"
            , out.size(), out.cover() * 100
        );
        return out;
    }

    public void dumpLexicon( Lexicon lxcn ) throws IOException {
        Path path = this.conf.lxcnFile();
        infof( "Dumping lexicon data to %s", path.toString() );
        Lexicon.write( lxcn, this.conf.lxcnFile() );
    }
    
    public Conf conf() {
        return this.conf;
    }
    
    public void conf( Conf conf ) throws IOException {
        this.conf = conf;
        this.ir.close();
        this.ir = null;
        this.ir = indexReader();
        this.lxcn = null;
        this.lxcn = lexicon();
        this.sample = null;
        this.sample = docSample();
    }
    
    public static <V> DocMap<V> buildDocMap( LeafReader lr, String field ) {
        DocMap dm = null;
        try {
            infof( "Building document map for %s", field );

            TermsEnum split = lr.terms( field ).iterator();
            SortedSet<String> splits = new TreeSet<>();
            int tCt = 0;
            while( split.next() != null ) {
                splits.add( split.term().utf8ToString() );
                tCt++;
            }
            infof( "Field %s contains %d output values", field, tCt );

            Stopwatch sw = Stopwatch.createUnstarted();

            dm = new DocMap( splits );
            infof( "Populating map..." );
            sw.start();
            for( int i = 0; i < lr.maxDoc(); i++ ) {
                String s = lr.document( i ).getField( field ).stringValue();
                dm.add( i, s );
            }
            sw.stop();
            infof( "Document map populated in %d millis", sw.elapsed( TimeUnit.MILLISECONDS ) );

            dm.finish();
        } catch( IOException ex ) {
            Logger.getLogger(Lector.class.getName() ).log( Level.SEVERE, null, ex );
        }
        return dm;
    }
    
    // TODO: add method to read in text data.
    public void buildTables() {
        AnalysisEngineDescription aed = tablesAE( this.conf );
        runUimaPipeline( uimaCRD( this.conf ), aed, this.conf.threads() );
    }

    public void buildIndex() {
        AnalysisEngineDescription aed = indexAE( this.conf );
        runUimaPipeline( uimaCRD( this.conf ), aed, this.conf.threads() );
    }
    
    public static void runUimaPipeline(
        CollectionReaderDescription crd, AnalysisEngineDescription aed, int threads
    ) {
        try {
            CpeBuilder cpb = new CpeBuilder();
            cpb.setMaxProcessingUnitThreadCount( threads );
            cpb.setReader( crd );
            cpb.setAnalysisEngine( aed );
            CollectionProcessingEngine cpe = cpb.createCpe( new CallbackListener() );
            cpe.process();
        } catch( IOException | SAXException | CpeDescriptorException | InvalidXMLException |
            ResourceInitializationException ex 
        ) {
            Logger.getLogger(Lector.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }
    
    public static CollectionReaderDescription uimaCRD( Conf conf ) {
        CollectionReaderDescription crd = null;
        try {
            crd = ComponentFactory.makeReaderDescription( conf.uimaReader(), conf.getProps() );
        } catch( ResourceInitializationException ex ) {
            Logger.getLogger(Lector.class.getName() ).log( Level.SEVERE, null, ex );
        }
        return crd;
    }
    
    public static AnalysisEngineDescription tablesAE( Conf conf ) {
        AnalysisEngineDescription aed = null;
//        try {
//            aed = AnalysisEngineFactory.createEngineDescription(
//                BuildTables.class
//                , BuildTables.PARAM_OUTPUT_DIR, conf.tablesDir().toString()
//            );
//        } catch( ResourceInitializationException ex ) {
//            Logger.getLogger(OBO.class.getName() ).log( Level.SEVERE, null, ex );
//        }
        return aed;
    }
    
    public static AnalysisEngineDescription indexAE( Conf conf ) {
//        ExternalResourceDescription iw = ExternalResourceFactory.createExternalResourceDescription(
//            LuceneIndexWriter.class
//            , LuceneIndexWriter.PARAM_INDEX_DIR, conf.indexDir().toString()
//        );
//
//        ExternalResourceDescription fb = ExternalResourceFactory.createExternalResourceDescription(
//            POBDocFields.class
//        );
//
//        ExternalResourceDescription ts = ExternalResourceFactory.createExternalResourceDescription(
//            POBTokenFields.class
//        );

        AnalysisEngineDescription aed = null;
//        try {
//            aed = AnalysisEngineFactory.createEngineDescription(
//                CorpusIndexer.class
//                , CorpusIndexer.RES_INDEX_WRITER , iw
//                , CorpusIndexer.RES_LUCENEFB     , fb
//                , CorpusIndexer.RES_STREAMS      , ts
////                , CorpusIndexer.PARAM_DRY_RUN, true
//            );
//        } catch( ResourceInitializationException ex ) {
//            Logger.getLogger( OBO.class.getName() ).log( Level.SEVERE, null, ex );
//        }
        return aed;
    }

    // TODO: move this to util clas
    private boolean checkField( String field ) throws IOException {
        for( String f : this.indexReader().fields() ) {
            if( f.equals( field ) ) return true;
        }
        return false;
    }
}
