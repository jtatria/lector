/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.corpus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;

import edu.columbia.incite.uima.ae.AbstractEngine;
import edu.columbia.incite.uima.api.ConfigurableResource;
import edu.columbia.incite.uima.api.SessionResource;
import edu.columbia.incite.uima.api.SimpleResource;
import edu.columbia.incite.uima.api.corpus.Tokens.LexAction;
import edu.columbia.incite.uima.api.corpus.Tokens.NonLexAction;
import edu.columbia.incite.uima.res.corpus.TermNormal;
import edu.columbia.incite.util.string.CSVWriter;

/**
 *
 * @author gorgonzola
 */
public class LemmaPOSTable extends AbstractEngine {

    public static final String PARAM_OUTFILE = "outFile";
    @ConfigurationParameter( name = PARAM_OUTFILE, mandatory = false,
        defaultValue = "lemmaPosTable.csv"
    )
    private File outFile;
    
    public static final String RES_TABLE = "table";
    @ExternalResource( key = RES_TABLE, mandatory = true )
    private Out table;
    
    public static final String PARAM_LEXICAL_CLASSES = "lexicalClasses";
    @ConfigurationParameter( name = PARAM_LEXICAL_CLASSES, mandatory = false, 
        defaultValue = {
            "ADJ",
            "ADV",
            "ART",
            "CARD",
            "CONJ",
            "NN",
            "NP",
            "O",
            "PP",
            "PR",
//            "PUNC",
            "V",
        }
    )
    private String[] lexicalClasses;
    
    public static final String PARAM_LEXICAL_OVERRIDES = "lexicalOverrides";
        @ConfigurationParameter( name = PARAM_LEXICAL_OVERRIDES, mandatory = false,
        description = "Overrides for tokens that should always be considered lexical . e.g. "
            + "'PR_PRP_i' to include the singular first person 'I' even if pronouns are "
            + "excluded",
        defaultValue = {
            "PR_PRP_i",
        }
    )
    private String[] lexicalOverrides;
    
    public static final String PARAM_LEXICAL_ACTION = "lAction";
    @ConfigurationParameter( name = PARAM_LEXICAL_ACTION, mandatory = false,
        description = "Action to take when dealing with lexical tokens. See LAction for "
            + "possible values",
//        defaultValue = "ASIS"
        defaultValue = "LEMMA"
//        defaultValue = "POSG"
//        defaultValue = "POSF"
//        defaultValue = "FULL"
    )
    private LexAction lAction;
    
    public static final String PARAM_NON_LEXICAL_ACTION = "nlAction";
    @ConfigurationParameter( name = PARAM_NON_LEXICAL_ACTION, mandatory = false,
        description = "Action to take when dealing with non-lexical tokens. See NLAction for "
            + "possible values.",
//        defaultValue = "DELETE"
        defaultValue = "MARK"
//        defaultValue = "POSG"
//        defaultValue = "POSF"
//        defaultValue = "LEMMA"
    )
    private NonLexAction nlAction;
    
    public static final String PARAM_LEMMA_MARKS = "lemmaMarks";
    @ConfigurationParameter( name = PARAM_LEMMA_MARKS, mandatory = false,
        description = "List of lemma sets that should be replaced with the set name: i.e. 450d -> MONEY",
        defaultValue = {
            "L_PUNCT",
            "L_NUMBER",
//            "L_SHORT",
            "L_MONEY",
            "L_ORD",
        }
    )
    private String[] lemmaMarks;
    
    public static final String PARAM_LEMMA_DELETIONS = "lemmaDeletions";
    @ConfigurationParameter( name = PARAM_LEMMA_DELETIONS, mandatory = false,
        description = "List of lemma sets that should be deleted",
        defaultValue = {
            "L_PUNCT"
        }
    )
    private String[] lemmaDeletions;
    
    private TermNormal termNormal;   

    private Long sssn;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        try {
            table.configure( outFile );
        } catch ( ResourceConfigurationException ex ) {
            throw new ResourceInitializationException( ex );
        }
        
        this.sssn = table.openSession();
        
        TermNormal.Conf c = new TermNormal.Conf();
        
        if( lexicalClasses != null && lexicalClasses.length != 0 ) {
            c.setLexClasses( 
                Arrays.stream( lexicalClasses ).map(
                    Tokens.LexClass::valueOf
                ).toArray( v -> new Tokens.LexClass[v] )
            );
        }
        
        if( lexicalOverrides != null && lexicalOverrides.length != 0 ) {
            c.setLexOverrides( lexicalOverrides );
        }
        
        if( lemmaMarks != null && lemmaMarks.length != 0 ) {
            c.setLemmaSubstitutions(
                Arrays.stream( lemmaMarks ).map(
                    Tokens.LemmaSet::valueOf
                ).toArray( v -> new Tokens.LemmaSet[v] )
            );
        }
        
        if( lemmaDeletions != null && lemmaDeletions.length != 0 ) {
            c.setLemmaDeletions( 
                Arrays.stream( lemmaDeletions ).map(
                    Tokens.LemmaSet::valueOf
                ).toArray( v -> new Tokens.LemmaSet[v] )
            );
        }

        c.setNonLexAction( nlAction );
        c.setLexAction(    lAction  );
        
        this.termNormal = new TermNormal( c );
    }
    
    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        
        AnnotationIndex<Token> tIndex = jcas.getAnnotationIndex( Token.class );
        FSIterator<Token> tIt = tIndex.iterator();
        
        while( tIt.hasNext() ) {
            Token t = tIt.next();
        
            String posC  = t.getPos().getType().getShortName();
            String posT  = t.getPos().getPosValue();
            
            String col = posC + "_" + posT;
            
            String term = termNormal.term( t );
            
            AtomicLong ct = table.get().get( term, col );
            if( ct == null ) {
                synchronized( table.get() ) {
                    table.get().put( term, col, new AtomicLong() );
                }
                ct = table.get().get( term, col );
            }
            ct.incrementAndGet();
        }
    }
    
    @Override
    public void collectionProcessComplete() {
        table.closeSession( sssn );
    }
    
    public static class Out extends Resource_ImplBase implements 
        SimpleResource<Table<String,String,AtomicLong>>,
        ConfigurableResource<File>, SessionResource<Long> {

        private final Table<String,String,AtomicLong> table = HashBasedTable.create();
        
        private File outFile;
        
        @Override
        public Table<String, String, AtomicLong> get() {
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
                    Logger.getLogger( LemmaPOSTable.class.getName() ).log( Level.SEVERE, null, ex );
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
