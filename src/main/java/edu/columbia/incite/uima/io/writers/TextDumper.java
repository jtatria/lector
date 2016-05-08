/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.io.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.corpus.Entities;
import edu.columbia.incite.uima.api.corpus.Entities.EAction;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.api.corpus.Tokens.LAction;
import edu.columbia.incite.uima.api.corpus.Tokens.NLAction;
import edu.columbia.incite.uima.api.types.Document;
import edu.columbia.incite.uima.api.types.Span;
import edu.columbia.incite.uima.util.Annotations;
import edu.columbia.incite.uima.util.Types;
import edu.columbia.incite.util.collection.CollectionTools;
import edu.columbia.incite.util.io.FileUtils;

import static edu.columbia.incite.uima.util.Types.*;

/**
 *
 * @author gorgonzola
 */
public class TextDumper extends AbstractFileWriter {
    
    public static final String DOC_SEP   = "\n";
    public static final String TOKEN_SEP = " ";
    public static final String SEC_SEP   = DOC_SEP;
    public static final String EOF       = "[EOF]" + DOC_SEP;

    // IO Settings.
    public static final String PARAM_CHARSET = "charset";
    @ConfigurationParameter( name = PARAM_CHARSET, mandatory = false,
        description = "Charset to use when writing text",
        defaultValue = "UTF-8"
    )
    private String charsetName;
    
    public static final String PARAM_OPEN_OPTIONS = "openOptions";
    @ConfigurationParameter( name = PARAM_OPEN_OPTIONS, mandatory = false,
        description = "Open options for files (CREATE, APPEND, TRUNCATE, WRITE, etc).",
        defaultValue = { "CREATE", "WRITE", "TRUNCATE_EXISTING" }
    )
    private String[] ooptsNames;
    
    // Type definitions.
    
    public static final String PARAM_SECTION_TYPENAME = "sectionTypeName";
    @ConfigurationParameter( name = PARAM_SECTION_TYPENAME, mandatory = false,
        description = "If present, only dump docs contained by annotations of this type."
    )
    private String secTypeName;
    
    public static final String PARAM_CTX_TYPENAME = "docTypeName";
    @ConfigurationParameter( name = PARAM_CTX_TYPENAME, mandatory = true,
        description = "Typename for indexing context annotations. Defaults to Paragraphs",
        defaultValue = "edu.columbia.incite.uima.api.types.Paragraph"
    )
    private String docTypeName;
    
    public static final String PARAM_ENTITY_TYPE = "entityType";
    @ConfigurationParameter( name = PARAM_ENTITY_TYPE, mandatory = false,
        description = "Type name for additional 'entity' annotations to be dumped. Typicaly results from NER, etc.",
        defaultValue = "edu.columbia.incite.uima.api.types.Span"
    )
    private Class entTypeName; // TODO: NOT USED! Token and entity types are hardcoded for now.
    
    // Entity transformations.
    public static final String PARAM_ACTION_ENTITIES = "entityAction";
    @ConfigurationParameter( name = PARAM_ACTION_ENTITIES, mandatory = false,
        description = "Action to take on found entities. See EAction's documentation for values.",
//        defaultValue = "NONE"
        defaultValue = "ADD_TYPE"
//        defaultValue = "ADD_ID"
//        defaultValue = "ADD_TEXT"
//        defaultValue = "ADD_DUMP"
        
    )
    private EAction eAction;
    
    public static final String PARAM_ADD_TYPE_TO_TERM = "addType";
    @ConfigurationParameter( name = PARAM_ADD_TYPE_TO_TERM, mandatory = false,
        description = "Include type names in dumped tokens. Setting this to true will force EAction"
            + " to be at least 'ADD_TYPE'",
        defaultValue = "false"
    )
    private Boolean addType;
    
    public static final String PARAM_ADD_ENTITY_ID = "addId";
    @ConfigurationParameter( name = PARAM_ADD_ENTITY_ID, mandatory = false,
        description = "Include entity id in entity tokens. This expects a feature named 'id' in "
            + "entity types. See 'Span' type definition in Incite's type system. Setting this to "
            + "true will force EAction to be at least 'ADD_ID'",
        defaultValue = "false"
    )
    private Boolean addId;
    
    public static final String PARAM_ADD_TEXT = "addTxt";
    @ConfigurationParameter( name = PARAM_ADD_TEXT, mandatory = false,
        description = "Include covered text for entity annotations. Setting this to true will force"
            + " EAction to be at least 'ADD_TEXT'",
        defaultValue = "false"
    )
    private Boolean addTxt;
    
    // Token class definitions.
    public static final String PARAM_LEXICAL_CLASSDEF = "lexicalClassDef";
    @ConfigurationParameter( name = PARAM_LEXICAL_CLASSDEF, mandatory = false,
        // TODO: All token parsing formats and stuff should be spawned-off, and consolidated with 
        // SVM's token parsing.
        description = "List of regular expressions patterns indicating non-lexical token classes",
        defaultValue = {
//            "ADJ_JJ[SR]?",
            "ADV_(WRB|RB[RS]?)",
            "ART_(DT|EX|[PW]DT)",
            "CARD_CD",
            "CONJ_CC",
//            "NN_NNS?",
//            "NP_NNPS?",
            "O_(\\#|``|''|\\$|FW|LS|POS|-[RL]RB-|UH)?",
            "PP_(IN|RP|TO)",
            "PR_(PR|W)P\\$?",
            "PUNC_(SYM|[,:\\.])",
//            "V_(MD|VB[DGNPZ]?)",
            }
    )
    private String[] nlClass;
    
    public static final String PARAM_NLCLASS_OVERRIDES = "nlOverrides";
    @ConfigurationParameter( name = PARAM_NLCLASS_OVERRIDES, mandatory = false,
        description = "Overrides for non-lexical clases that should be included anyway. e.g. "
            + "'PR_PRP_i' to include the singular first person 'I' even if other pronouns are "
            + "excluded",
        defaultValue = {
//            "PR_PRP_i",
        }
    )
    private String[] nlOverrides;

    // Token transformations.
    public static final String PARAM_NON_LEXICAL_ACTION = "nlAction";
    @ConfigurationParameter( name = PARAM_NON_LEXICAL_ACTION, mandatory = false,
        description = "Action to take when dealing with non-lexical tokens. See NLAction for "
            + "possible values.",
        defaultValue = "DELETE"
//        defaultValue = "MARK"
//        defaultValue = "POSG"
//        defaultValue = "POSF"
        
    )
    private NLAction nlAction;
    
    public static final String PARAM_LEXICAL_ACTION = "lAction";
    @ConfigurationParameter( name = PARAM_LEXICAL_ACTION, mandatory = false,
        description = "Action to take when dealing with lexical tokens. See LAction for "
            + "possible values",
        defaultValue = "ASIS"
//        defaultValue = "LEMMA"
//        defaultValue = "POSG"
//        defaultValue = "POSF"
//        defaultValue = "FULL"
    )
    private LAction lAction;
    
    public static final String PARAM_LEMMA_SUBSTITUTIONS = "lemmaSubstitutions";
    @ConfigurationParameter( name = PARAM_LEMMA_SUBSTITUTIONS, mandatory = false,
        description = "Map array with regular expressions and strings indicating substitutions"
            + "(Map-arrays are even-numbered lists in ( key0, value0, ... , keyN, valueN ) format, "
            + "a-la Perl)",
        defaultValue = {
            // Dangling puntuation marks (including those not recognized by the POS tagger).
            // In an ideal world, these would be represented by UNICODE classes, but our automata
            // are not entirelly smart about them.
            // TODO: test this and maybe patch automata to properly support unicode classes.
//            "[!\"#$%&()*+,-./:;<=>?@|—\\\\~{}_^'¡£¥¦§«°±³·»¼½¾¿–—‘’‚“”„†•…₤™✗]+", "",
//            "[!\"#$%&()*+,-./:;<=>?@|—\\\\~{}_^'¡"
//                + "£"
//                + "¥¦§«°±³·»¼½¾¿–—‘’‚“”„†•…₤™✗]+", "",
            // Two or less letter words. Not recommended, too much breakage.
//            ".{0,2}", "",
            // TODO: this does not work, because stanford is splitting some moneis from their 
            // suffixes.
            // (British) Money amounts
//            "[0-9]+-?[lds]\\.?", "MONEY",
            // Ordinal numbers
//            "[0-9]*(13th|[0456789]th|1st|2nd|3rd)", "ORDINAL",
        }
    )
    private String[] lemmaSubstitutions;
    
    // Nuclear option: ignore everything and just dump SOFA strings.
    public static final String PARAM_DUMP_RAW = "dumpRaw";
    @ConfigurationParameter( name = PARAM_DUMP_RAW, mandatory = false,
        description = "KISS option: Ignore any transformations and dump everything as-is.",
        defaultValue = "false"
    )
    private Boolean dumpRaw;
    
    public static final String PARAM_POST_NORMALIZE = "normalize";
    @ConfigurationParameter( name = PARAM_POST_NORMALIZE, mandatory = false,
        description = "Normalize tokens after all standard actions, i.e. before any custom "
            + "substitutions",
        defaultValue = "true"
    )
    private boolean normalize;
    
    // Non-lexical tokens.
    private CharacterRunAutomaton nlCra;
    // Non-lexical overrides.
    private CharacterRunAutomaton nlOverrideCra;
    // Substitution candidates
    private CharacterRunAutomaton substCra;
    // Substition map.
    private Map<CharacterRunAutomaton,String> substs;
    
    // NIO open options.
    private StandardOpenOption[] opts;
    // Charset.
    private Charset cs;

    // Per-CAS data.
    // Document index
    private final Map<AnnotationFS,Collection<AnnotationFS>> docIndex = new HashMap<>();
    private final Map<AnnotationFS,Collection<AnnotationFS>> coverIndex = new HashMap<>();
    // Cover types.
    private Type coverType;
    // Types that will be dumped
    private final Set<Type> typesToDump = new HashSet<>();
    // Token type
    private Type tType;
    // Entity type
    // TODO: this is a scalar value: all dumped entities must be this or inherit from this.
    private Type eType;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );

        if( !charsetName.startsWith( "UTF" ) ) {
            getLogger().log( Level.WARNING, 
                "Obsolete charset requested for output. It is highly recommended to use some "
                    + "Unicode variant for dumping text!"
            );
        }
        this.cs = Charset.forName( charsetName );
        
        List<StandardOpenOption> optsL = new ArrayList<>();
        for( String s : ooptsNames ) {
            optsL.add( StandardOpenOption.valueOf( s ) );
        }
        this.opts = optsL.toArray( new StandardOpenOption[optsL.size()] );
        
        // If we have a valid non-lexical class definition...
        if( nlClass != null && nlClass.length != 0 ) {
            // Accept anything that matches any of the given patterns.
            // Any token accepted by this automaton will be assumed to point to a non-lexical term.
            Automaton au = Automata.makeEmpty();
            List<String> nls = CollectionTools.toSortedList( nlClass );
            for( String nl : nls ) {
                RegExp rx = new RegExp( nl + "_.*" ); // Add suffix to accept non-empty lemmas.
                au = Operations.union( au, rx.toAutomaton() );
            }
            nlCra = new CharacterRunAutomaton( au );
        } else {
            // Reject everything. All tokens are assumed to point to lexical terms.
            nlCra = new CharacterRunAutomaton( Automata.makeEmpty() );
        }
        
        if( lemmaSubstitutions != null && lemmaSubstitutions.length != 0 ) {
            if( lemmaSubstitutions.length % 2 != 0 ) throw new ResourceInitializationException(
                "{0} parameters must come in pairs.",
                new Object[]{ PARAM_LEMMA_SUBSTITUTIONS }
            );
            
            Automaton au = Automata.makeEmpty();
            substs = new HashMap<>();
            for( int i = 0; i < lemmaSubstitutions.length; i++ ) {
                Automaton rx = new RegExp( lemmaSubstitutions[i] ).toAutomaton();
                au = Operations.union( au, rx );
                substs.put( new CharacterRunAutomaton( rx ), lemmaSubstitutions[++i] );
            }
            substCra = new CharacterRunAutomaton( au );
        } else {
            substCra = new CharacterRunAutomaton( Automata.makeEmpty() );
        }
        
        // If we have valid overrides...
        if( nlOverrides != null && nlOverrides.length != 0 ) {
            // Accept anything that matches the given patterns.
            // Any tokens accepted by this automaton will be considered lexical always.
            Automaton au = Automata.makeEmpty();
            List<String> nlos = CollectionTools.toSortedList( nlOverrides );
            for( String nlo : nlos ) {
                RegExp rx = new RegExp( nlo );
                au = Operations.union( au, rx.toAutomaton() );
            }
            nlOverrideCra = new CharacterRunAutomaton( au );
        } else {
            // Reject everything. All tokens will have lexicality defined by class definition, above
            nlOverrideCra = new CharacterRunAutomaton( Automata.makeEmpty() );
        }
        
        // Validate entity actions.
        if( addType && eAction.compareTo( EAction.ADD_TYPE ) < 0 ) eAction = EAction.ADD_TYPE;
        if( addId   && eAction.compareTo( EAction.ADD_ID   ) < 0 ) eAction = EAction.ADD_ID;
        if( addTxt  && eAction.compareTo( EAction.ADD_TEXT ) < 0 ) eAction = EAction.ADD_TEXT;
    }
    
    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );
        
        coverType = secTypeName != null ?
                         Types.checkType( jcas.getTypeSystem(), secTypeName ) :
                         jcas.getCasType( DocumentAnnotation.type );
        Type docType = Types.checkType( jcas.getTypeSystem(), docTypeName );
        coverIndex.putAll( CasUtil.indexCovered( jcas.getCas(), coverType, docType ) );
        
        Collection<AnnotationFS> select = CasUtil.select( jcas.getCas(), coverType );
        for( AnnotationFS ann :  select ) {
            try {
                String toString = ann.toString();
            } catch ( Exception ex ) {
                int i = 0;
            }
        }
        
        // All TCAS types.
        Type annType = Types.checkType( jcas.getTypeSystem(), CAS.TYPE_NAME_ANNOTATION );
        docIndex.putAll( CasUtil.indexCovered( jcas.getCas(), docType, annType ) );
        
        // TODO: this needs to be refactored.
        tType = tType == null ? jcas.getCasType( Token.type ) : tType;
        eType = eType == null ? jcas.getCasType( Span.type  ) : eType;
        typesToDump.add( tType );
        typesToDump.add( eType  );
        
        getLogger().log( Level.INFO, "Dumping {1} context annotations for document {0}."
            , new Object[]{ getDocumentId(), Integer.toString( docIndex.keySet().size() ) }
        );
    }
    
    @Override 
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        String  dir  = getDir();
        String  file = getFileNameForCas( jcas );
        boolean mkd  = getMakeDirs();
        boolean ow   = getOverwrite();
        try( Writer wrtr = FileUtils.getWriter( dir, file, mkd, ow, cs, opts ) ) {
            
            FSIterator<Annotation> cIt = jcas.getAnnotationIndex( coverType ).iterator();
            while( cIt.hasNext() ) {
                AnnotationFS cover = cIt.next();
                
                if( filterCover( cover ) ) continue;
                
                for( AnnotationFS doc : coverIndex.get( cover ) ) {

//                    if( dumpRaw ) { // KISS: dump sofaString and break.
//                        wrtr.append( doc.getCoveredText() );
//                        continue;
//                    }
//
                    for( AnnotationFS ann : filterTypes( docIndex.get( doc ), typesToDump ) ) {
                        // TODO: this is here because Incite's document annotation has no extent, i.e.
                        // begin == end == 0, which implies that it gets sorted after all other
                        // annotations that have begin offset 0, e.g. any document-wide containers,
                        // the standard UIMA TCas DocumentAnnotation, etc. Eventually, this should be
                        // fixed in the type system definition, maybe removing Document's inheritance
                        // from TCAS annotations, but I have not yet figured out the implications of
                        // this for Document metadata access, as this would most likely remove Document 
                        // annotations from the standard FSIndexes, which may break downstream analyses
                        // (Ours tend to use UIMA-fit's indexes, which do not entirelly rely on 
                        // FSIndexes... something that should also be looked into... some day).
                        if( ann.getClass().isAssignableFrom( Document.class ) ) {
                            continue;
                        }

                        // Make txt aplying lexical/entity transformations.
                        String txt = serialize( ann );
                        if( txt.length() == 0 ) continue;

                        // Apply substitutions.
                        if( substCra.run( txt ) ) txt = applySubstitutions( txt );
                        if( txt.length() == 0 ) continue;

                        // If we survived, append and break.
                        wrtr.append( txt );
                        wrtr.append( TOKEN_SEP );
                    }
                    // Break document.
                    wrtr.append( DOC_SEP );
                }
                wrtr.append( SEC_SEP );
            }
            wrtr.append( EOF );
            
        } catch ( Exception ex ) {
            getLogger().log( Level.SEVERE, "Exception occurred when trying to process CAS {0}: {1}",
                new Object[]{ getDocumentId(), ex.toString() }
            );
        }
        
        this.coverIndex.clear();
        this.docIndex.clear();
        this.typesToDump.clear();
    }

    private String serialize( AnnotationFS ann ) {
        if( isToken( ann ) ) {
            return getTokenCharTerm( ann );
        } else if( isEntity( ann ) ) {
            return getEntityCharTerm( ann );
        } else {
            return ann.getCoveredText();
        }
    }

    private boolean isToken( AnnotationFS ann ) {
        return isType( ann, tType );
    }

    private String getTokenCharTerm( AnnotationFS token ) {
        Token t = (Token) token;
        String nrml = Tokens.build( t );
        String out;
        if( nlCra.run( nrml ) && !nlOverrideCra.run( nrml ) ) {
            out = nlAction.apply( t );
        } else out = lAction.apply( t );
        if( normalize ) out = normalize( out );
        return out;
    }

    private boolean isEntity( AnnotationFS ann ) {
        return isType( ann, eType );
    }

    private String getEntityCharTerm( AnnotationFS entity ) {
        String ent = eAction.apply( (Span) entity );
        return ent.length() != 0 ? String.join( Entities.SEP, Entities.BASE, ent ) : Entities.BASE;
    }

    private String applySubstitutions( String txt ) {
        for( CharacterRunAutomaton cra : substs.keySet() ) {
            if( cra.run( txt ) ) return substs.get( cra );
        }
        return txt;
    }

    private String normalize( String txt ) {
        return txt.toLowerCase();
    }

    private boolean filterCover( AnnotationFS cover ) {
        return false;
    }
}
