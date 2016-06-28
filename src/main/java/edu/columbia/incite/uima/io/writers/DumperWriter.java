/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.io.writers;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.corpus.Entities;
import edu.columbia.incite.uima.api.corpus.Entities.EntityAction;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.api.corpus.Tokens.LexAction;
import edu.columbia.incite.uima.api.corpus.Tokens.LexClass;
import edu.columbia.incite.uima.api.corpus.Tokens.LemmaSet;
import edu.columbia.incite.uima.api.corpus.Tokens.NonLexAction;
import edu.columbia.incite.uima.api.types.Document;
import edu.columbia.incite.uima.api.types.Span;
import edu.columbia.incite.uima.util.Types;
import edu.columbia.incite.util.collection.CollectionTools;
import edu.columbia.incite.util.io.FileUtils;

import static edu.columbia.incite.uima.util.Types.*;

/**
 *
 * @author gorgonzola
 */

public class DumperWriter extends AbstractFileWriter {
    
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
//        defaultValue = "DELETE"
        defaultValue = "TYPE"
//        defaultValue = "TYPE_ID"
//        defaultValue = "TYPE_ID_COVERED"
//        defaultValue = "TYPE_ID_COVERED_DUMP"
        
    )
    private EntityAction eAction;
    
    public static final String PARAM_ADD_TYPE_TO_TERM = "addType";
    @ConfigurationParameter( name = PARAM_ADD_TYPE_TO_TERM, mandatory = false,
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
    
    public static final String PARAM_ADD_TEXT = "addTxt";
    @ConfigurationParameter( name = PARAM_ADD_TEXT, mandatory = false,
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
            "L_PUNCT",
            "L_NUMBER",
            "L_SHORT",
            "L_MONEY",
            "L_ORD",
        }
    )
    private String[] lemmaSubstitutions;
    
    public static final String PARAM_MARKED_SUBSTITUTIONS = "markedSubstitutions";
    @ConfigurationParameter( name = PARAM_MARKED_SUBSTITUTIONS, mandatory = false,
        description = "List of substitutions to be marked instead of deleted. If empty, all are deleted. If it contains '*', all are marked.",
        defaultValue = {
            "L_NUMBER",
            "L_MONEY",
            "L_ORD",
        }
    )
    private String[] markedSubstitutions;
    
    // Nuclear option: ignore everything and just dump SOFA strings.
    public static final String PARAM_DUMP_RAW = "dumpRaw";
    @ConfigurationParameter( name = PARAM_DUMP_RAW, mandatory = false,
        description = "KISS option: Ignore any transformations and dump everything as-is.",
        defaultValue = "false"
    )
    private Boolean dumpRaw;
//    
//    public static final String PARAM_POST_NORMALIZE = "normalize";
//    @ConfigurationParameter( name = PARAM_POST_NORMALIZE, mandatory = false,
//        description = "Normalize tokens after all standard actions, i.e. before any custom "
//            + "substitutions",
//        defaultValue = "true"
//    )
//    private boolean normalize;
    
    // Non-lexical tokens.
    private CharacterRunAutomaton lexicalCra;
    // Non-lexical overrides.
    private CharacterRunAutomaton overrideCra;
    // Substitution candidates
    private CharacterRunAutomaton substCra;
    // Substitions.
    private List<LemmaSet> substs;
    private Set<LemmaSet> mSubsts;
    
    // NIO open options.
    private StandardOpenOption[] opts;
    // Charset.
    private Charset cs;

    // Per-CAS data.
    // Document index
    private final Map<AnnotationFS,Collection<AnnotationFS>> docIndex = new HashMap<>();
    private final Map<AnnotationFS,Collection<AnnotationFS>> coverIndex = new HashMap<>();
    // Cover types.
    private Type cType;
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
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );
        
        cType = secTypeName != null ?
                         Types.checkType( jcas.getTypeSystem(), secTypeName ) :
                         jcas.getCasType( DocumentAnnotation.type );
        
        Type docType = Types.checkType( jcas.getTypeSystem(), docTypeName );
        coverIndex.putAll( CasUtil.indexCovered( jcas.getCas(), cType, docType ) );
        
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
        
        CharacterRunAutomaton wind = new CharacterRunAutomaton( Automata.makeString( "wind" ) );
        
        try( Writer wrtr = FileUtils.getWriter( dir, file, mkd, ow, cs, opts ) ) {
            
            FSIterator<Annotation> cIt = jcas.getAnnotationIndex( cType ).iterator();
            while( cIt.hasNext() ) {
                AnnotationFS cover = cIt.next();
                if( filterCover( cover ) ) continue;
                
                for( AnnotationFS doc : coverIndex.get( cover ) ) {

                    if( dumpRaw ) { // KISS: dump sofaString and break.
                        wrtr.append( doc.getCoveredText() );
                        continue;
                    }
                    
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

                        // Stanford doesn't know the difference between wind and wound.
                        if( wind.run( txt ) ) txt = "wound";
                        
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
            
        } catch ( IOException ex ) {
            getLogger().log( Level.SEVERE, "Exception occurred when trying to process CAS {0}: {1}",
                new Object[]{ getDocumentId(), ex.toString() }
            );
        }
    }
    
    @Override
    public void postProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.postProcess( jcas );
        this.coverIndex.clear();
        this.docIndex.clear();
        this.typesToDump.clear();
        this.tType = null;
        this.cType = null;
        this.eType = null;
    }

    private String serialize( AnnotationFS ann ) {
        if( isToken( ann ) ) {
            return getTokenCharTerm( ann );
        } else if( isEntity( ann ) ) {
            return getEntityCharTerm( ann );
        } else {
            // This should never happen!
            return ann.getCoveredText();
        }
    }

    private boolean isToken( AnnotationFS ann ) {
        return isType( ann, tType );
    }

    private String getTokenCharTerm( AnnotationFS token ) {
        Token t = (Token) token;
        String out;
        if( lexicalCra.run( Tokens.pos( t ) ) || overrideCra.run( Tokens.build( t ) ) ) {
            out = lAction.apply( t );
        } else {
            out = nlAction.apply( t );
        }
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
        for( LemmaSet ls : substs ) {
            if( ls.test( txt ) ) {
                if( mSubsts.contains( ls ) ) {
                    return ls.apply( txt );
                }
                else return "";
            }
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
