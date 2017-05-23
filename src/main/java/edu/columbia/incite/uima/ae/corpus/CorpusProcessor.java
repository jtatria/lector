/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.corpus;

import edu.columbia.incite.uima.ae.SegmentedEngine;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.ConfigurableResource;
import edu.columbia.incite.uima.api.SessionResource;
import edu.columbia.incite.uima.api.SimpleResource;
//import edu.columbia.incite.uima.api.corpus.POSClass;
import edu.columbia.incite.corpus.POSClass;
import edu.columbia.incite.uima.api.types.Tokens;
import edu.columbia.incite.corpus.LemmaSet;
import edu.columbia.incite.uima.res.corpus.TermNormal;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.DataFieldType;
import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.string.CSVWriter;

/**
 *
 * @author gorgonzola
 */
public abstract class CorpusProcessor extends SegmentedEngine {
    
    // Transformations
    public static final String PARAM_INCLUDE_PUNC = "includePunc";
    @ConfigurationParameter( name = PARAM_INCLUDE_PUNC, mandatory = false, defaultValue = "false" )
    private Boolean includePunc;
    
    public static final String PARAM_EXCLUDE_NONLEX = "excludeNonLex";
    @ConfigurationParameter( name = PARAM_EXCLUDE_NONLEX, mandatory = false, defaultValue = "false" )
    private Boolean excludeNonLex;
    
    public static final String PARAM_LEMMATIZE = "lemmatize";
    @ConfigurationParameter( name = PARAM_LEMMATIZE, mandatory = false, defaultValue = "true" )
    private Boolean lemmatize;
    
    public static final String PARAM_ADD_POS = "addPos";
    @ConfigurationParameter( name = PARAM_ADD_POS, mandatory = false, defaultValue = "false" )
    private Boolean addPos;
    
    public static final String PARAM_LEMMASETS_REPLACE = "replaceLemmaSets";
    @ConfigurationParameter( name = PARAM_LEMMASETS_REPLACE, mandatory = false )
    private LemmaSet[] replaceLemmaSets;
    
    public static final String PARAM_LEMMASETS_DELETE = "deleteLemmaSets";
    @ConfigurationParameter( name = PARAM_LEMMASETS_DELETE, mandatory = false )
    private LemmaSet[] deleteLemmaSets;
    
    // Stats    
    public static final String PARAM_COLLECT_VOCABULLARY = "collectVocabulary";
    @ConfigurationParameter( name = PARAM_COLLECT_VOCABULLARY, mandatory = false, defaultValue = "false" )
    private Boolean buildVocabulary;
    
    public static final String PARAM_SPLIT_VOCAB_COUNTS_BY = "splitVocabBy";
    @ConfigurationParameter( name = PARAM_SPLIT_VOCAB_COUNTS_BY, mandatory = false,
        defaultValue = "OBOSegment:year"
    )
    private String splitVocabBy;
    
    public static final String PARAM_BUILD_LEMMA_POS_TABLE = "buildLemmaPosTable";
    @ConfigurationParameter( name = PARAM_BUILD_LEMMA_POS_TABLE, mandatory = false, defaultValue = "false" )
    private Boolean buildPosTable;
    
    public static final String PARAM_LEMMA_POS_TABLE_OUTFILE = "lptOUtFile";
    @ConfigurationParameter( name = PARAM_LEMMA_POS_TABLE_OUTFILE, mandatory = false, defaultValue = "lpt.csv" )
    private File lptOutFile;

    public static final String PARAM_VOCAB_TABLE_OUTFILE = "vocabOUtFile";
    @ConfigurationParameter( name = PARAM_VOCAB_TABLE_OUTFILE, mandatory = false, defaultValue = "vocab.csv" )
    private File vocabOutFile;
    
    public static final String RES_LPT_TABLE = "lpTable";
    @ExternalResource( key = RES_LPT_TABLE, mandatory = false )
    private DataTable lpTable;
    
    public static final String RES_VOCAB_TABLE = "vocabTable";
    @ExternalResource( key = RES_VOCAB_TABLE, mandatory = false )
    private DataTable vocabTable;
    
    private Long lptSssn;
    private Long vocabSssn;
    
    protected TermNormal termNormal;
    private DataField<String> split;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        TermNormal.Conf c = new TermNormal.Conf();

        List<POSClass> pos = new ArrayList<>();
        
        if( excludeNonLex ) pos.addAll(Arrays.asList( Tokens.LEX_CLASSES ) );
        else pos.addAll(Arrays.asList( Tokens.ALL_CLASSES ) );
        
        if( includePunc ) pos.add(POSClass.PUNC );
        c.setLexClasses(pos.toArray(new POSClass[pos.size()] ) );

        Tokens.LexAction action;
        if( addPos ) action = Tokens.LexAction.ADD_POS_TAG;
        else if( lemmatize ) action = Tokens.LexAction.LEMMATIZE;
        else action = Tokens.LexAction.KEEP_AS_IS;
        c.setLexicalAction( action );

        c.setNonLexicalAction( Tokens.NonLexAction.DELETE );

        if( deleteLemmaSets != null && deleteLemmaSets.length > 0 ) {
            c.setLemmaDeletions( deleteLemmaSets );
        }

        if( replaceLemmaSets != null && replaceLemmaSets.length > 0 ) {
            c.setLemmaDeletions( replaceLemmaSets );
        }

        termNormal = new TermNormal( c.commit() );

        if( buildVocabulary ) {
            try {
                vocabTable.configure( vocabOutFile );
            } catch ( ResourceConfigurationException ex ) {
                throw new ResourceInitializationException( ex );
            }        
            this.vocabSssn = vocabTable.openSession();
            
            if( splitVocabBy != null && !"".equals( splitVocabBy ) ) {
                this.split = new DataField( splitVocabBy, DataFieldType.STRING );
            }
        }

        if( buildPosTable ) {
            try {
                lpTable.configure( lptOutFile );
            } catch ( ResourceConfigurationException ex ) {
                throw new ResourceInitializationException( ex );
            }        
            this.lptSssn = lpTable.openSession();
        }
    }
    
    @Override
    public void collectionProcessComplete() {
        if( buildPosTable ) {
            lpTable.closeSession( lptSssn );
        }
        if( buildVocabulary ) {
            vocabTable.closeSession( vocabSssn );
        }
    }

    public static final String VOCAB_TOTAL = "total";
    
    protected void updateCounts( AnnotationFS ann, String out, Datum md ) {
        if( !Tokens.isToken( ann ) ) return;
        Table<String,String,AtomicLong> pos = lpTable.get();
        Table<String,String,AtomicLong> voc = vocabTable.get();
        String col = ( split != null && md != null ) ? md.get( split ) : null;
        
        if( buildPosTable ) {
            String posT = Tokens.posT( ann );
            if( !pos.contains( out, posT ) ) {
//                    synchronized( pos ) { // Needed for Guava < 22
                        pos.put( out, posT, new AtomicLong() );
//                    }
                }
            pos.get( out, posT ).incrementAndGet();
        }
        
        if( buildVocabulary ) {
            if( !voc.contains( out, VOCAB_TOTAL ) ) {
//                synchronized( voc ) { // Needed for Guava < 22
                voc.put( out, VOCAB_TOTAL, new AtomicLong() );
//                }
            }
            voc.get( out, VOCAB_TOTAL ).incrementAndGet();
            if( col != null ) {
                if( !voc.contains( out, col ) ) {
//                        synchronized( voc ) { // Needed for Guava < 22
                        voc.put( out, col, new AtomicLong() );
//                        }
                }
                voc.get( out, col ).incrementAndGet();
            }
        }
    }

    public static class DataTable extends Resource_ImplBase implements 
        SimpleResource<Table<String,String,AtomicLong>>,
        ConfigurableResource<File>, SessionResource<Long> {
        
        public static final String PARAM_DATA_FILE = "dataFile";
        @ConfigurationParameter( name = PARAM_DATA_FILE, mandatory = true )
        private File dataFile;

        private final Table<String,String,AtomicLong> table = Tables.synchronizedTable(
            HashBasedTable.create()
        );
        
        
        @Override
        public Table<String,String,AtomicLong> get() {
            return table;
        }        

        private final AtomicLong sssn = new AtomicLong();
        private final Set<Long> openSssn = Collections.newSetFromMap( new ConcurrentHashMap<>() );
        
        @Override
        public Long openSession() {
            long sssnTk = sssn.incrementAndGet();
            openSssn.add( sssnTk );
            return sssnTk;
        }

        @Override
        public void closeSession( Long session ) {
            openSssn.remove( session );
            if( openSssn.isEmpty() ) {
                getLogger().log( 
                    Level.INFO, String.format( "DataTable closing: dumping data to %s", dataFile ) 
                );
                
                try ( PrintStream ps = new PrintStream( dataFile ) ) {
                    CSVWriter.write( ps, table );
                } catch ( FileNotFoundException ex ) {
                    getLogger().log( Level.SEVERE, "I/O error when trying to write data to disk", ex );
                }
            }
        }

        @Override
        public void configure( File out ) throws ResourceConfigurationException {
            if( dataFile == null || !dataFile.equals( out ) ) {
                this.dataFile = out;
            }
        }
    }
}
