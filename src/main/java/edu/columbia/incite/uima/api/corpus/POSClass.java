/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.corpus;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.MinimizationOperations;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * All possible lexical classes. Values correspond to the names of known sub-types of
 * {@link de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS}.
 * This class contains the static method
 * {@link #make(POSClass...) }
 * to produce an {@link Automaton} instance that will be used in lexicality tests.
 */
public enum POSClass implements Predicate<String> {
    /** Adjectives: JJ, JJS, JJR **/
    ADJ( "ADJ" + Tokens.SEP + "JJ[SR]?", new String[ ]{ "JJ", "JJS", "JJR" } ),
    /** Adverbs: RB, RBR, RBS, WRB **/
    ADV( "ADV" + Tokens.SEP + "(WRB|RB[RS]?)", new String[ ]{ "RB", "RBR", "RBS", "WRB" } ),
    /** Articles and determinants: DT, EX, PDT, WDT **/
    ART( "ART" + Tokens.SEP + "(DT|EX|[PW]DT)", new String[ ]{ "DT", "EX", "PDT", "WDT" } ),
    /** Cardinals: CD **/
    CARD( "CARD" + Tokens.SEP + "CD", new String[ ]{ "CD" } ),
    /** Coordinating conjunctions: CC **/
    CONJ( "CONJ" + Tokens.SEP + "CC", new String[ ]{ "CC" } ),
    /** Nouns: NN, NNS **/
    NN( "NN" + Tokens.SEP + "NNS?", new String[ ]{ "NN", "NNS" } ),
    /** Proper nouns: NNP, NNPS **/
    NP( "NP" + Tokens.SEP + "NNPS?", new String[ ]{ "NNP", "NNPS" } ),
    /** Catch-all for non Penn treebank tags **/
    O( "O" + Tokens.SEP + "(\\#|``|''|\\$|FW|LS|POS|-[RL]RB-|UH)?", new String[ ]{ "", "#", "``", "''", "$", "FW", "LS", "POS", "-LRB-", "-RRB-", "UH" } ),
    /** Prepositions and particleds: IN, RP, TO **/
    PP( "PP" + Tokens.SEP + "(IN|RP|TO)", new String[ ]{ "IN", "RP", "TO" } ),
    /** Pronouns: PR, PRP, WP, WP$ **/
    PR( "PR" + Tokens.SEP + "(PR|W)P\\$?", new String[ ]{ "PRP", "PRP$", "WP", "WP$" } ),
    /** Punctuation: commas, colons, periods and SYM **/
    PUNC( "PUNC" + Tokens.SEP + "(SYM|[,:\\.])", new String[ ]{ ",", ":", ".", "SYM" } ),
    /** Verbs: MD, VB, VBD, VBG, VBN, VBP, VBZ **/
    V( "V" + Tokens.SEP + "(MD|VB[DGNPZ]?)", new String[ ]{ "MD", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ" } );
    
    public static final POSClass[] ALL_CLASSES = new POSClass[ ]{
        ADJ,
        ADV,
        ART,
        CARD,
        CONJ,
        NN,
        NP,
        O,
        PP,
        PR,
//        PUNC,
        V
    };
    public static final POSClass[] LEX_CLASSES = new POSClass[ ]{
        ADJ,
        ADV,
//        ART,
//        CARD,
//        CONJ,
        NN,
        NP,
//        O,
//        PP,
//        PR,
//        PUNC,
        V
    };
    public static final Map<String, POSClass> MEMBERS;
    static {
        Map<String, POSClass> tmp = new HashMap<>();
        for ( POSClass p : POSClass.values() ) {
            for ( String m : p.tags ) {
                tmp.put( m, p );
            }
        }
        MEMBERS = ImmutableMap.copyOf( tmp );
    }
    /** RegEx string for this class **/
    public final String rx;
    /** POS tags in this class **/
    public final String[] tags;
    private final Automaton au;
    private final CharacterRunAutomaton cra;

    private POSClass( String regexp, String[] tags ) {
        this.rx = regexp;
        this.tags = tags;
        RegExp regex = new RegExp( regexp );
        this.au = regex.toAutomaton();
        this.cra = new CharacterRunAutomaton( au );
    }

    /**
     * Obtain an automaton to test for members of this class.
     * @return An {@link Automaton}.
     */
    public Automaton au() {
        return this.au;
    }

    /**
     * Obtain a compiled automaton to test for members of this class.
     * @return An {@link CharacterRunAutomaton}.
     */
    public CharacterRunAutomaton cra() {
        return this.cra;
    }

    /** Functional interface for use in lamda expressions.
     * @param t A {@link Token}.
     * @return  {@code true} if the POS group for the given token is accepted by this class's
     * automaton.
     */
    @Override
    public boolean test( String t ) {
        return cra.run( t );
    }

    /**
     * Combine the given {@link POSClass} instances to produce an automaton to test tokens for
     * lexicality.
     * @param inc   An array of LexClass values that will be accepted by the returned automaton.
     * @return  An {@link Automaton} that will accept tokens corresponding to any of the given
     * classes.
     */
    public static Automaton make( POSClass... inc ) {
        Automaton out = Automata.makeEmpty();
        for ( POSClass lc : inc ) {
            out = Operations.union( out, lc.au );
        }
//        MinimizationOperations.minimize( out, Operations.DEFAULT_MAX_DETERMINIZED_STATES );
        return out;
    }

    public static Automaton make( Collection<POSClass> inc ) {
        Automaton out = Automata.makeEmpty();
        for ( POSClass lc : inc ) {
            out = Operations.union( out, lc.au );
        }
//        MinimizationOperations.minimize( out, Operations.DEFAULT_MAX_DETERMINIZED_STATES );
        return out;
    }
    
}
