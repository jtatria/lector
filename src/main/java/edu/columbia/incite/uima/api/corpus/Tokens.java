
package edu.columbia.incite.uima.api.corpus;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RegExp;

/**
 *
 * @author gorgonzola
 */
public class Tokens {
    /** Token part separator: _ **/
    public static final String SEP = "_";
    /** Generic non-lexical mark: %%% **/
    public static final String NL_MRKR = "%%%";
    
    /** POS group index in canonical array **/
    public static final int PGI  = 0;
    /** POS tag index in canonical array **/
    public static final int PTI  = 1;
    /** Lemma index in canonical array **/
    public static final int LMI  = 2;
    /** Raw text index in canonical array **/
    public static final int RAW  = 3;
    
    /**
     * Produce canonical String array for the given token.
     * This method omits raw text.
     * @param token A {@link Token}.
     * @return A String[] containing the parts of the canonical form of the given token.
     */
    public static String[] parse( Token token ) {
        return parse( token, false );
    }
    
    /** Produce canonical String array for the given token.
     * @param token A {@link Token}.
     * @param addTxt if {@code true}, include raw text.
     * @return A String[] containing the parts of the canonical form of the given token.
     */
    public static String[] parse( Token token, boolean addTxt ) {
        String posg  = token.getPos().getType().getShortName();
        String posf  = token.getPos().getPosValue();
        String lemma = token.getLemma().getValue().toLowerCase( Locale.ROOT );
        String raw   = addTxt ? token.getCoveredText().toLowerCase( Locale.ROOT ) : null;
        return addTxt ? new String[]{ posg, posf, lemma, raw } : new String[]{ posg, posf, lemma };
    }
    
    /**
     * Get POS group and tag for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * @param token A {@link Token}.
     * @return The String representation of the POS group for the given token.
     */
    public static String pos( Token token ) {
        String[] parse = parse( token );
        return String.join(SEP, parse[PGI], parse[PTI] );
    }
    
    /**
     * Get POS group for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * The returned String can be used to obtain a {@link LexClass} instance with {@link LexClass#valueOf(java.lang.String) }.
     * @param token A {@link Token}.
     * @return The String representation of the POS group for the given token.
     */
    public static String posG( Token token ) {
        return parse( token )[PGI];
    }
    
    /**
     * Get POS tag for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * @param token A {@link Token}.
     * @return The String representation of the POS tag for the given token.
     */    
    public static String posT( Token token ) {
        return parse( token )[PTI];
    }

    /**
     * Get Lemma for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * The returned String can be tested agains the tests defined in a {@link LemmaSet}.
     * @param token A {@link Token}.
     * @return The String representation of the Lemma for the given token.
     */        
    public static String lemma( Token token ) {
        return parse( token )[LMI];
    }
    
    /**
     * Get raw text for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * The returned String can be tested agains the tests defined in a {@link LemmaSet}.
     * @param token A {@link Token}.
     * @return The String representation of the POS group for the given token.
     */        
    public static String raw( Token token ) {
        return parse( token )[RAW];
    }
    
    /** 
     * Produce canonical String representation for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * This method omits raw text.
     * @param token A {@link Token}
     * @return The canonical string representation of the given token.
     */
    public static String build( Token token ) {
        return build( token, false );
    }

    /** 
     * Produce canonical String representation for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * @param token A {@link Token}
     * @param addTxt    if {@code true}, append raw text to the canonical form.
     * @return The canonical string representation of the given token.
     */    
    public static String build( Token token, boolean addTxt ) {
        return String.join( SEP, parse( token, addTxt ) );
    }
    
    /**
     * Actions for string serialization of lexical tokens. See {@link LexClass} for details about lexicality.
     */
    public enum LexAction implements Function<Token,String> {
        /** Keep original text **/
        ASIS(  ( Token token ) -> parse( token, true )[RAW] ),
        /** Keep lemmatized form **/
        LEMMA( ( Token token ) -> parse( token )[LMI] ),
        /** Keep POS tag group and lemmatized form **/
        POSG(  ( Token token ) -> {
            String[] parse = parse( token );
            return String.join( SEP, parse[PGI], parse[LMI] );
        } ),
        /** Keep POS tag group, POS tag, and lemmatized form **/
        POST(  ( Token token ) -> build( token ) ),
        /** Keep POS tag group, POS tag, lemma, and raw text **/
        FULL(  ( Token token ) -> build( token, true ) ),
        ;

        private final Function<Token,String> func;
        
        private LexAction( Function<Token,String> func ) {
            this.func = func;
        }
        
        @Override
        public String apply( Token token ) {
            return func.apply( token );
        }
    }

    /**
     * Actions for string serialization of non-lexical tokens. See {@link LexClass for details about lexicality determination.
     **/
    public enum NonLexAction implements Function<Token,String> {
        /** Delete everyhing **/
        DELETE( ( Token t ) -> "" ),
        /** Replace with {@link Tokens#NL_MRKR} **/
        MARK(   ( Token t ) -> Tokens.NL_MRKR ),
        /** Replace with POS group **/
        POSG(   ( Token t ) -> parse( t )[PGI] ),
        /** Replace with POS tag **/
        POST(   ( Token t ) -> parse( t )[PTI] ),
        /** Replace with lemma **/
        LEMMA(  ( Token t ) -> parse( t )[LMI] ),
        ;

        private final Function<Token,String> func;
        
        private NonLexAction( Function<Token,String> func ) {
            this.func = func;
        }
        
        @Override
        public String apply( Token t ) {
            return func.apply( t );
        }
    }
    
    /**
     * All possible lexical classes. Values correspond to the names of known sub-types of 
     * {@link de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS}.
     * This class contains the static method 
     * {@link #make(edu.columbia.incite.uima.api.corpus.Tokens.LexClass...) } 
     * to produce an {@link Automaton} instance that will be used in lexicality tests.
     */
    public enum LexClass implements Predicate<Token> {
        /** Adjectives: JJ, JJS, JJR **/
        ADJ( "ADJ" + SEP + "JJ[SR]?", new String[]{ "JJ", "JJS", "JJR" } ),
        /** Adverbs: RB, RBR, RBS, WRB **/
        ADV( "ADV" + SEP + "(WRB|RB[RS]?)", new String[]{ "RB", "RBR", "RBS", "WRB" } ),
        /** Articles and determinants: DT, EX, PDT, WDT **/
        ART( "ART" + SEP + "(DT|EX|[PW]DT)", new String[]{ "DT", "EX", "PDT", "WDT" } ),
        /** Cardinals: CD **/
        CARD( "CARD" + SEP + "CD", new String[]{ "CD" } ),
        /** Coordinating conjunctions: CC **/
        CONJ( "CONJ" + SEP + "CC", new String[]{ "CC" } ),
        /** Nouns: NN, NNS **/
        NN( "NN" + SEP + "NNS?", new String[]{ "NN", "NNS" } ),
        /** Proper nouns: NNP, NNPS **/
        NP( "NP" + SEP + "NNPS?", new String[]{ "NNP", "NNPS" } ),
        /** Catch-all for non Penn treebank tags **/
        O( "O" + SEP + "(\\#|``|''|\\$|FW|LS|POS|-[RL]RB-|UH)?", new String[]{ "", "#", "``", "''", "$", "FW", "LS", "POS", "-LRB-", "-RRB-", "UH" } ),
        /** Prepositions and particleds: IN, RP, TO **/
        PP( "PP" + SEP + "(IN|RP|TO)", new String[]{ "IN", "RP", "TO" } ),
        /** Pronouns: PR, PRP, WP, WP$ **/
        PR( "PR" + SEP + "(PR|W)P\\$?", new String[]{ "PRP", "PRP$", "WP", "WP$" } ),
        /** Punctuation: commas, colons, periods and SYM **/
        PUNC( "PUNC" + SEP + "(SYM|[,:\\.])", new String[]{ ",", ":", ".", "SYM" } ),
        /** Verbs: MD, VB, VBD, VBG, VBN, VBP, VBZ **/
        V( "V" + SEP + "(MD|VB[DGNPZ]?)", new String[]{ "MD", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ" } ),
        ;

        public final static Map<String,LexClass> MEMBERS;
        static {
            Map<String,LexClass> tmp = new HashMap<>();
            for( LexClass p : LexClass.values() ) {
                for( String m : p.tags ) {
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

        private LexClass( String regexp, String[] tags ) {
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

        /** Functional interface for use in lamdas.
         * @param t A {@link Token}.
         * @return  {@code true} if the POS group for the given token is accepted by this class's automaton.
         */
        @Override
        public boolean test( Token t ) {
            return cra.run( Tokens.pos( t ) );
        }
        
        /**
         * Combine the given {@link LexClass} instances to produce an automaton to test tokens for lexicality.
         * @param inc   An array of LexClass values that will be accepted by the returned automaton.
         * @return  An {@link Automaton} that will accept tokens corresponding to any of the given classes.
         */
        public static Automaton make( LexClass... inc ) {
            Automaton out = Automata.makeEmpty();
            for( LexClass lc : inc ) {
                out = Operations.union( out, lc.au );
            }
            return out;
        }
    }
    
    /**
     * A collection of regular patterns for common lemmas of dubious lexicality in all syntatic 
     * roles. e.g. cardinal numbers, even when the POS is not CARD.
     * Automata in this enum assume they will be tested against Lemma strings, NOT canonical forms,
     * i.e. "dog" instead of "N_NN_dog". Lemmas can be obtained by 
     * {@link Tokens#lemma(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)}
     */
    public enum LemmaSet implements Predicate<String>, Function<String,String> {
        /** Aribtrary punctuation marks **/
        L_PUNCT(  "[!\"#$%&()*+,-./:;<=>?@|—\\\\~{}_^'¡£¥¦§«°±³·»¼½¾¿–—‘’‚“”„†•…₤™✗]+" ),
        /** Two-letter-or-less lemmas **/
        L_SHORT(  ".{0,2}" ),
        /** British money amounts **/
        L_MONEY(  "[0-9]+-?[lds]\\.?" ),
        /** Ordinal numbers **/
        L_ORD( "[0-9]*((1[123]|[0456789])th|1st|2nd|3rd)" ),
        /** Any number **/
        L_NUMBER( "[0-9,.]+" ),
        ;
        
        private Automaton au;
        private CharacterRunAutomaton cra;
        
        private LemmaSet( String rx ) {
            this.au = new RegExp( rx ).toAutomaton();
            this.cra = new CharacterRunAutomaton( au );
        }
        
        /**
         * Combine the given {@link LemmaSet} instances into an automaton equal to the union of 
         * all given sets.
         * @param inc   An array of LemmaSet values that will be accepted by the returned automaton.
         * @return  An {@link Automaton} that will accept tokens accepted by any of the given set's
         * automata.
         */
        public static Automaton make( LemmaSet... inc ) {
            Automaton out = Automata.makeEmpty();
            for( LemmaSet lc : inc ) {
                out = Operations.union( out, lc.au );
            }
            return out;
        }
        
        /**
         * Functional interface for use as predicates in lambda expressions.
         * @param t A {@link Token}.
         * @return  {@code true} if the given token is accepted by this set's automaton.
         */
        @Override
        public boolean test( String t ) {
            return cra.run( t );
        }

        /**
         * Functional interface for use as function in lambda expressions to replace lemmas in this
         * set by the set's name.
         * @param t A {@link Token}
         * @return A string corresponding to this class's name if the token is a member of this class.
         */
        @Override
        public String apply( String t ) {
            return cra.run( t ) ? this.name() : t;
        }
    }
}
