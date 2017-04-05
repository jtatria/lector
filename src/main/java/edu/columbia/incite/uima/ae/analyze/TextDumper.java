/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.analyze;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import edu.columbia.incite.uima.api.corpus.Entities;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.api.corpus.Tokens.LemmaSet;
import edu.columbia.incite.uima.api.corpus.Tokens.POSClass;
import edu.columbia.incite.uima.res.corpus.TermNormal;
import edu.columbia.incite.uima.res.corpus.TermNormal.Conf;
import edu.columbia.incite.util.io.FileUtils;

/**
 *
 * @author gorgonzola
 */
public class TextDumper extends StructuredReader {
    
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
    
    public static final String PARAM_ACTION_ENTITIES = "entityAction";
    @ConfigurationParameter( name = PARAM_ACTION_ENTITIES, mandatory = false,
        description = "Action to take on found entities. See EntityAction's documentation for "
            + "legal values.",
//        defaultValue = "DELETE"
        defaultValue = "TYPE"
//        defaultValue = "TYPE_ID"
//        defaultValue = "TYPE_ID_COVERED"
//        defaultValue = "TYPE_ID_COVERED_DUMP"
        
    )
    private Entities.EntityAction eAction;
        
    // Token class definitions.
    public static final String PARAM_LEXICAL_CLASSES = "lexicalClasses";
    public static final String[] DFLT = new String[]{};
    @ConfigurationParameter( name = PARAM_LEXICAL_CLASSES, mandatory = false,
        // TODO: All token parsing formats and stuff should be spawned-off, and consolidated with 
        // SVM's token parsing.
        description = "List of regular expressions patterns indicating lexical token classes",
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
    
    // Token transformations.
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
    private Tokens.NonLexAction nlAction;
    
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
    private Tokens.LexAction lAction;
    
    public static final String PARAM_LEMMA_MARKS = "lemmaMarks";
    @ConfigurationParameter( name = PARAM_LEMMA_MARKS, mandatory = false,
        description = "List of lemma sets that should be replaced with the set name: i.e. 450d -> MONEY",
        defaultValue = {
            "L_PUNCT",
            "L_NUMBER",
            "L_SHORT",
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
    private Writer out;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        Conf c = new Conf();
        if( lexicalClasses != null && lexicalClasses.length != 0 ) {
            c.setLexClasses( Arrays.stream( lexicalClasses ).map(
                ( i ) -> POSClass.valueOf( i )
            ).toArray( v -> new POSClass[v] ) );
        }
        if( lexicalOverrides != null && lexicalOverrides.length != 0 ) {
            c.setLexOverrides( lexicalOverrides );
        }
        
        if( lemmaMarks != null && lemmaMarks.length != 0 ) {
            c.setLemmaSubstitutions(
                Arrays.stream( lemmaMarks ).map(
                    LemmaSet::valueOf
                ).toArray( v -> new LemmaSet[v] )
            );
        }
        
        if( lemmaDeletions != null && lemmaDeletions.length != 0 ) {
            c.setLemmaDeletions( 
                Arrays.stream( lemmaDeletions ).map(
                    LemmaSet::valueOf
                ).toArray( v -> new LemmaSet[v] )
            );
        }
        
        c.setEntityAction( eAction  );
        c.setNonLexAction( nlAction );
        c.setLexAction(    lAction  );
        
        this.termNormal = new TermNormal( c );
    }
    
    @Override
    public void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );
        try {
            out = FileUtils.getWriter( outputDir, getDocumentId() + EXT, true, true );
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
                    }
                }
                out.append( DOC_SEP );
            } catch ( IOException ex ) {
                Logger.getLogger(TextDumper.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }
    }
    
    @Override
    protected void postProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.postProcess( jcas );
        try {
            this.out.append( EOF );
            this.out.close();
        } catch ( IOException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }

    protected boolean checkCovers( Collection<AnnotationFS> covers ) {
        return true;
    }
    
    protected boolean checkDoc( Annotation doc ) {
        return true;
    }

}
