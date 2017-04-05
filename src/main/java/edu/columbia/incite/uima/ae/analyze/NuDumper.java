/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.analyze;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.ConfigurableResource;
import edu.columbia.incite.uima.api.SessionResource;
import edu.columbia.incite.uima.api.SimpleResource;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.api.corpus.Tokens.LemmaSet;
import edu.columbia.incite.uima.api.corpus.Tokens.LexAction;
import edu.columbia.incite.uima.api.corpus.Tokens.POSClass;
import edu.columbia.incite.uima.res.corpus.TermNormal;
import edu.columbia.incite.uima.res.corpus.TermNormal.Conf;
import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.io.FileUtils;
import edu.columbia.incite.util.string.CSVWriter;


/**
 *
 * @author gorgonzola
 */
public class NuDumper extends StructuredReader {
    
    public static final String DOC_SEP   = "\n";
    public static final String TOKEN_SEP = " ";
    public static final String SEC_SEP   = DOC_SEP;
    public static final String EOF       = "[EOF]" + DOC_SEP;
    public static final String EXT       = ".dump";
        
    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter( name = PARAM_OUTPUT_DIR, mandatory = false, defaultValue = "data/corpusDump",
        description = "Output Directory"
    )
    private String outputDir;

    public static final String PARAM_DUMP_RAW = "dumpRaw";
    @ConfigurationParameter( name = PARAM_DUMP_RAW, mandatory = false, defaultValue = "false",
        description = "Ignore all parameters and dump raw covered text (will produce duplications "
            + "if token annotations are not a segmentation type or more than one type is included)" 
    )
    private Boolean dumpRaw;
    
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
    private String[] replaceLemmaSets;
    
    public static final String PARAM_LEMMASETS_DELETE = "deleteLemmaSets";
    @ConfigurationParameter( name = PARAM_LEMMASETS_DELETE, mandatory = false )
    private String[] deleteLemmaSets;
        
    // Stats
    
    public static final String PARAM_COLLECT_VOCABULLARY = "collectVocabulary";
    @ConfigurationParameter( name = PARAM_COLLECT_VOCABULLARY, mandatory = false, defaultValue = "false" )
    private Boolean collectVocabulary;
    
    public static final String PARAM_SPLIT_VOCAB_COUNTS_BY = "splitVocabBy";
    @ConfigurationParameter( name = PARAM_SPLIT_VOCAB_COUNTS_BY, mandatory = false, defaultValue = "" )
    private String splitVocabBy;
    
    public static final String PARAM_BUILD_LEMMA_POS_TABLE = "buildLemmaPosTable";
    @ConfigurationParameter( name = PARAM_BUILD_LEMMA_POS_TABLE, mandatory = false, defaultValue = "false" )
    private Boolean buildLemmaPosTable;
    
    public static final String PARAM_LEMMA_POS_TABLE_OUTFILE = "lptOUtFile";
    @ConfigurationParameter( name = PARAM_LEMMA_POS_TABLE_OUTFILE, mandatory = false, defaultValue = "lpt.csv" )
    private File lptOutFile;

    public static final String PARAM_VOCAB_TABLE_OUTFILE = "vocabOUtFile";
    @ConfigurationParameter( name = PARAM_VOCAB_TABLE_OUTFILE, mandatory = false, defaultValue = "vocab.csv" )
    private File vocabOutFile;
    
    public static final String LPT_TABLE = "lpTable";
    @ExternalResource( key = LPT_TABLE, mandatory = false )
    private DataTable lpTable;
    
    public static final String VOCAB_TABLE = "vocabTable";
    @ExternalResource( key = LPT_TABLE, mandatory = false )
    private DataTable vocabTable;
    
    private Long lptSssn;
    private Long vocabSssn;
    
    private TermNormal termNormal;
    private Writer out;
    
    private Datum metadata;
        
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        if( dumpRaw ) {
            getLogger().log( Level.INFO, "Dumping raw text, ignoring all configuration settings.");
        } else {
            Conf c = new Conf();
            
            List<POSClass> pos = new ArrayList<>();            
            if( excludeNonLex ) pos.addAll( Arrays.asList( POSClass.LEX_CLASSES ) );
            else pos.addAll( Arrays.asList( POSClass.ALL_CLASSES ) );
            if( includePunc ) pos.add( POSClass.PUNC );
            c.setLexClasses( pos.toArray( new POSClass[pos.size()] ) );
            
            LexAction action;
            if( addPos ) action = LexAction.POST;
            else if( lemmatize ) action = LexAction.LEMMA;
            else action = LexAction.ASIS;
            c.setLexAction( action );
            
            c.setNonLexAction( Tokens.NonLexAction.DELETE );
            
            if( deleteLemmaSets != null && deleteLemmaSets.length > 0 ) {
                c.setLemmaDeletions( // JAVA SUCKS
                    Arrays.stream( deleteLemmaSets ).map( 
                        ( s ) -> LemmaSet.valueOf( s ) 
                    ).toArray( i -> new LemmaSet[i] ) 
                );
            }
            
            if( replaceLemmaSets != null && replaceLemmaSets.length > 0 ) {
                c.setLemmaDeletions( // JAVA SUCKS
                    Arrays.stream( replaceLemmaSets ).map( 
                        ( s ) -> LemmaSet.valueOf( s ) 
                    ).toArray( i -> new LemmaSet[i] ) 
                );
            }
            
            termNormal = new TermNormal( c.commit() );
            
            if( collectVocabulary ) {
                try {
                    vocabTable.configure( vocabOutFile );
                } catch ( ResourceConfigurationException ex ) {
                    throw new ResourceInitializationException( ex );
                }        
                this.vocabSssn = lpTable.openSession();
            }
            if( buildLemmaPosTable ) {
                try {
                    lpTable.configure( lptOutFile );
                } catch ( ResourceConfigurationException ex ) {
                    throw new ResourceInitializationException( ex );
                }        
                this.lptSssn = lpTable.openSession();
            }
        }
    }
     
    @Override
    public void collectionProcessComplete() {
        if( buildLemmaPosTable ) {
            lpTable.closeSession( lptSssn );
        }
    }
    
    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );
        metadata = getMetadata();
        try {
            out = FileUtils.getWriter( outputDir, getDocumentId() + EXT, true, true );
        } catch ( IOException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }
    
    @Override
    protected void postProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.postProcess( jcas );
        metadata = null;
        try {
            this.out.append( EOF );
            this.out.close();
        } catch ( IOException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }

    @Override
    protected void read( 
        Annotation doc, Collection<AnnotationFS> covers, Collection<AnnotationFS> tokens 
    ) {
        if( checkDoc( doc ) && checkCovers( covers ) ) {
            try {
                if( dumpRaw ) {
                        out.append( doc.getCoveredText() );
                } else {
                    for( AnnotationFS t : tokens ) {
                        String txt = termNormal.term( t );
                        if( txt.length() <= 0 ) continue;
                        out.append( txt );
                        out.append( TOKEN_SEP );
                        
                        if( buildLemmaPosTable || collectVocabulary ) {
                        }
                    }
                }
                out.append( DOC_SEP );
            } catch ( IOException ex ) {
                getLogger().log( Level.SEVERE, "I/O error when trying to write", ex );
            }
        }
    }
    
    protected boolean checkCovers( Collection<AnnotationFS> covers ) {
        return true;
    }
    
    protected boolean checkDoc( Annotation doc ) {
        return true;
    }
    
    public static class DataTable extends Resource_ImplBase implements 
        SimpleResource<Table<String,String,AtomicLong>>,
        ConfigurableResource<File>, SessionResource<Long> {

        private final Table<String,String,AtomicLong> table = HashBasedTable.create();
        
        private File outFile;
                
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
                try ( PrintStream ps = new PrintStream( outFile ) ) {
                    CSVWriter.write( ps, table );
                } catch ( FileNotFoundException ex ) {
                    getLogger().log( Level.SEVERE, "I/O error when trying to write data to disk", ex );
                }
            }
        }

        @Override
        public void configure( File out ) throws ResourceConfigurationException {
            if( outFile == null || !outFile.equals( out ) ) {
                this.outFile = out;
            }
        }
    }
    
}
