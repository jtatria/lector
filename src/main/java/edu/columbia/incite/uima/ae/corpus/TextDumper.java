/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.corpus;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import edu.columbia.incite.uima.api.corpus.Entities.EntityAction;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.api.corpus.Tokens.LexAction;
import edu.columbia.incite.uima.api.corpus.Tokens.LexClass;
import edu.columbia.incite.uima.api.corpus.Tokens.LemmaSet;
import edu.columbia.incite.uima.api.corpus.Tokens.NonLexAction;
import edu.columbia.incite.uima.api.types.Span;
import edu.columbia.incite.util.collection.CollectionTools;
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
    @ConfigurationParameter( name = PARAM_OUTPUT_DIR, mandatory = false, defaultValue = "data/corpusDump" )
    private String outputDir;
    
    public static final String PARAM_WIPE_EXISTING = "wipeExisting";
    @ConfigurationParameter( name = PARAM_WIPE_EXISTING, mandatory = false, defaultValue = "true" )
    private Boolean wipeExisting;
    
    public static final String PARAM_DUMP_RAW = "dumpRaw";
    @ConfigurationParameter( name = PARAM_DUMP_RAW, mandatory = false, defaultValue = "false" )
    private Boolean dumpRaw;
    
    // Entity transformations.
    public static final String PARAM_ACTION_ENTITIES = "entityAction";
    @ConfigurationParameter( name = PARAM_ACTION_ENTITIES, mandatory = false,
        description = "Action to take on found entities. See EAction's documentation for values.",
//        defaultValue = "DELETE"
        defaultValue = "TYPE"
//        defaultValue = "TYPE_ID"
//        defaultValue = "TYPE_ID_COVERED"
//        defaultValue = "TYPE_ID_COVERED_DUMP"
        
    )
    private EntityAction eAction;
    
    public static final String PARAM_ADD_ENTITY_TYPE = "addType";
    @ConfigurationParameter( name = PARAM_ADD_ENTITY_TYPE, mandatory = false,
        description = "Include type names in dumped tokens. Setting this to true will force EAction"
            + " to be at least 'TYPE'",
        defaultValue = "false"
    )
    private Boolean addType;
    
    public static final String PARAM_ADD_ENTITY_ID = "addId";
    @ConfigurationParameter( name = PARAM_ADD_ENTITY_ID, mandatory = false,
        description = "Include entity id in entity tokens. This expects a feature named 'id' in "
            + "entity types. See 'Span' type definition in Incite's type system. Setting this to "
            + "true will force EAction to be at least 'TYPE_ID'",
        defaultValue = "false"
    )
    private Boolean addId;
    
    public static final String PARAM_ADD_ENTITY_TEXT = "addTxt";
    @ConfigurationParameter( name = PARAM_ADD_ENTITY_TEXT, mandatory = false,
        description = "Include covered text for entity annotations. Setting this to true will force"
            + " EAction to be at least 'TYPE_ID_COVERED'",
        defaultValue = "false"
    )
    private Boolean addTxt;
    
    // Token class definitions.
    public static final String PARAM_LEXICAL_CLASSDEF = "lexicalClasses";
    @ConfigurationParameter( name = PARAM_LEXICAL_CLASSDEF, mandatory = false,
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
    
    public static final String PARAM_NLCLASS_OVERRIDES = "lexicalOverrides";
    @ConfigurationParameter( name = PARAM_NLCLASS_OVERRIDES, mandatory = false,
        description = "Overrides for tokens that should always be considered lexical . e.g. "
            + "'PR_PRP_i' to include the singular first person 'I' even if pronouns are "
            + "excluded",
        defaultValue = {
//            "PR_PRP_i",
        }
    )
    private String[] lexicalOverrides;
    
    // Token transformations.
    public static final String PARAM_NON_LEXICAL_ACTION = "nlAction";
    @ConfigurationParameter( name = PARAM_NON_LEXICAL_ACTION, mandatory = false,
        description = "Action to take when dealing with non-lexical tokens. See NLAction for "
            + "possible values.",
        defaultValue = "DELETE"
//        defaultValue = "MARK"
//        defaultValue = "POSG"
//        defaultValue = "POSF"
//        defaultValue = "LEMMA"
    )
    private NonLexAction nlAction;
    
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
    
    public static final String PARAM_LEMMA_SUBSTITUTIONS = "lemmaSubstitutions";
    @ConfigurationParameter( name = PARAM_LEMMA_SUBSTITUTIONS, mandatory = false,
        description = "Map array with regular expressions and strings indicating substitutions"
            + "(Map-arrays are even-numbered lists in ( key0, value0, ... , keyN, valueN ) format, "
            + "a-la Perl)",
        defaultValue = {
//            "L_PUNCT",
//            "L_NUMBER",
//            "L_SHORT",
//            "L_MONEY",
//            "L_ORD",
        }
    )
    private String[] lemmaSubstitutions;
    
    public static final String PARAM_MARKED_SUBSTITUTIONS = "markedSubstitutions";
    @ConfigurationParameter( name = PARAM_MARKED_SUBSTITUTIONS, mandatory = false,
        description = "List of substitutions to be marked instead of deleted. If empty, all are "
            + "deleted. If it contains '*', all are marked.",
        defaultValue = {
//            "L_NUMBER",
//            "L_MONEY",
//            "L_ORD",
        }
    )
    private String[] markedSubstitutions;
    
    // Non-lexical tokens.
    private CharacterRunAutomaton lexicalCra;
    // Non-lexical overrides.
    private CharacterRunAutomaton overrideCra;
    // Substitution candidates
    private CharacterRunAutomaton substCra;
    // Substitions.
    private List<LemmaSet> substs;
    private Set<LemmaSet> mSubsts;
    
    private Writer out;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        if( lexicalClasses != null && lexicalClasses.length != 0 ) {
            List<LexClass> lclasses = Arrays.stream( lexicalClasses )
                .map(LexClass::valueOf )
                .collect( Collectors.toList() );
            Automaton au = LexClass.make(lclasses.toArray(new LexClass[lclasses.size()] ) );
            this.lexicalCra = new CharacterRunAutomaton( au );
        } else {
            this.lexicalCra = new CharacterRunAutomaton( Automata.makeAnyString() );
        }
        
        if( lemmaSubstitutions != null && lemmaSubstitutions.length != 0 ) {
            this.substs = Arrays.stream( lemmaSubstitutions )
                .map(LemmaSet::valueOf )
                .collect( Collectors.toList() );
            Automaton au = LemmaSet.make(substs.toArray(new LemmaSet[substs.size()] ) );
            this.substCra = new CharacterRunAutomaton( au );            
        } else {
            this.substCra = new CharacterRunAutomaton( Automata.makeEmpty() );
        }
        
        if( markedSubstitutions != null && markedSubstitutions.length != 0 ) {
            this.mSubsts = Arrays.stream( markedSubstitutions )
                .map(LemmaSet::valueOf )
                .collect( Collectors.toSet() );
        }
        
        if( lexicalOverrides != null && lexicalOverrides.length != 0 ) {
            // Accept anything that matches the given patterns.
            // Any tokens accepted by this automaton will be considered lexical always.
            Automaton au = Automata.makeEmpty();
            List<String> lOverride = CollectionTools.toSortedList( lexicalOverrides );
            for( String nlo : lOverride ) {
                RegExp rx = new RegExp( nlo );
                au = Operations.union( au, rx.toAutomaton() );
            }
            overrideCra = new CharacterRunAutomaton( au );
        } else {
            // Reject everything: Lexicality exclusively determined by class.
            overrideCra = new CharacterRunAutomaton( Automata.makeEmpty() );
        }
        
        // Validate entity actions.
        if( addType && eAction.compareTo(EntityAction.TYPE ) < 0 ) eAction = EntityAction.TYPE;
        if( addId   && eAction.compareTo(EntityAction.TYPE_ID   ) < 0 ) eAction = EntityAction.TYPE_ID;
        if( addTxt  && eAction.compareTo(EntityAction.TYPE_ID_COVERED ) < 0 ) eAction = EntityAction.TYPE_ID_COVERED;
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
                        String txt = dump( t );
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

    private String dump( AnnotationFS ann ) {
        String txt;
        if( Token.class.isAssignableFrom( ann.getClass() ) ) {
            Token t = (Token) ann;
            if( lexicalCra.run( Tokens.pos( t ) ) || overrideCra.run( Tokens.build( t ) ) ) {
                txt = lAction.apply( t );
            } else {
                txt = nlAction.apply( t );
            }
        } else if( Span.class.isAssignableFrom( ann.getClass() ) ) {
            Span ent = (Span) ann;
            txt = eAction.apply( ent );
        } else {
            txt = ann.getCoveredText();
        }
        
        if( txt.length() > 0 && substCra.run( txt ) ) {
            for( LemmaSet ls : substs ) {
                if( ls.test( txt ) ) {
                    if( mSubsts.contains( ls ) ) {
                        txt = ls.apply( txt );
                    }
                    else txt = "";
                }
            }
        }
        
        return txt;
    }    


}
