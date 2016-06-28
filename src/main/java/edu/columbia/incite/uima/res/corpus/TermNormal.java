/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

import edu.columbia.incite.uima.api.corpus.Entities.EntityAction;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.api.corpus.Tokens.LexAction;
import edu.columbia.incite.uima.api.corpus.Tokens.LemmaSet;
import edu.columbia.incite.uima.api.corpus.Tokens.NonLexAction;
import edu.columbia.incite.uima.api.types.Span;
import edu.columbia.incite.util.collection.CollectionTools;

/**
 *
 * @author gorgonzola
 */
public class TermNormal extends Resource_ImplBase {
    
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
    public static final String PARAM_LEXICAL_CLASSES = "lexicalClasses";
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
    
    private CharacterRunAutomaton isLex;
    private CharacterRunAutomaton lexOverride;
    private CharacterRunAutomaton substitute;
    private List<LemmaSet> substitutions;
    private Set<LemmaSet> markedSubsts;
    
    @Override
    public boolean initialize( ResourceSpecifier aSpecifier, Map<String,Object> aAdditionalParams )
    throws ResourceInitializationException {
        boolean ret = super.initialize( aSpecifier, aAdditionalParams );
        
        if( lexicalClasses != null && lexicalClasses.length != 0 ) {
            List<Tokens.LexClass> lclasses = Arrays.stream( lexicalClasses )
                .map(Tokens.LexClass::valueOf )
                .collect( Collectors.toList() );
            Automaton au = Tokens.LexClass.make(lclasses.toArray(new Tokens.LexClass[lclasses.size()] ) );
            this.isLex = new CharacterRunAutomaton( au );
        } else {
            this.isLex = new CharacterRunAutomaton( Automata.makeAnyString() );
        }
        
        if( lemmaSubstitutions != null && lemmaSubstitutions.length != 0 ) {
            this.substitutions = Arrays.stream( lemmaSubstitutions )
                .map(LemmaSet::valueOf )
                .collect( Collectors.toList() );
            Automaton au = LemmaSet.make( substitutions.toArray(new LemmaSet[substitutions.size()] ) );
            this.substitute = new CharacterRunAutomaton( au );            
        } else {
            this.substitute = new CharacterRunAutomaton( Automata.makeEmpty() );
        }
        
        if( markedSubstitutions != null && markedSubstitutions.length != 0 ) {
            this.markedSubsts = Arrays.stream( markedSubstitutions )
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
            lexOverride = new CharacterRunAutomaton( au );
        } else {
            // Reject everything: Lexicality exclusively determined by class.
            lexOverride = new CharacterRunAutomaton( Automata.makeEmpty() );
        }
        
        // Validate entity actions.
        if( addType && eAction.compareTo(EntityAction.TYPE ) < 0 ) eAction = EntityAction.TYPE;
        if( addId   && eAction.compareTo(EntityAction.TYPE_ID   ) < 0 ) eAction = EntityAction.TYPE_ID;
        if( addTxt  && eAction.compareTo(EntityAction.TYPE_ID_COVERED ) < 0 ) eAction = EntityAction.TYPE_ID_COVERED;
        
        return ret;
    }
        
    public String term( Annotation ann ) {
        String txt;
        if( Token.class.isAssignableFrom( ann.getClass() ) ) {
            Token t = (Token) ann;
            if( isLex.run( Tokens.pos( t ) ) || lexOverride.run( Tokens.build( t ) ) ) {
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
        
        if( txt.length() > 0 && substitute.run( txt ) ) {            
            for( Tokens.LemmaSet ls : substitutions ) {
                if( ls.test( txt ) ) {
                    if( markedSubsts.contains( ls ) ) {
                        txt = ls.apply( txt );
                    }
                    else txt = "";
                }
            }
        }
        
        return txt;
    }
    
    public String type( Annotation ann ) {
        return ann.getType().getShortName();
    }

    public byte[] data( Annotation ann ) {
        return BytesRef.EMPTY_BYTES;
    }
    
}
