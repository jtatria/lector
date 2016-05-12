
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
    
    public static final String SEP = "_";
    public static final String NL_MRKR = "%%%";
    
    public static final int PGI  = 0;
    public static final int PFI  = 1;
    public static final int LMI  = 2;
    public static final int RAW  = 3;
    
    public static String[] parse( Token token ) {
        return parse( token, false );
    }
    
    public static String[] parse( Token token, boolean addTxt ) {
        String posg  = token.getPos().getType().getShortName();
        String posf  = token.getPos().getPosValue();
        String lemma = token.getLemma().getValue().toLowerCase( Locale.ROOT );
        String raw   = addTxt ? token.getCoveredText().toLowerCase( Locale.ROOT ) : null;
        return addTxt ? new String[]{ posg, posf, lemma, raw } : new String[]{ posg, posf, lemma };
    }
    
    public static String pos( Token token ) {
        String[] parse = parse( token );
        return String.join( SEP, parse[PGI], parse[PFI] );
    }
    
    public static String posG( Token token ) {
        return parse( token )[PGI];
    }
    
    public static String posF( Token token ) {
        return parse( token )[PFI];
    }
    
    public static String lemma( Token token ) {
        return parse( token )[LMI];
    }
    
    public static String raw( Token token ) {
        return parse( token )[RAW];
    }
    
    public static String build( Token token ) {
        return build( token, false );
    }
    
    public static String build( Token token, boolean addTxt ) {
        return String.join( SEP, parse( token, addTxt ) );
    }
    
    public enum LAction implements Function<Token,String> {
        /** Keep original text **/
        ASIS(   ( Token token ) -> parse( token, true )[RAW] ),
        /** Keep lemmatized form **/
        LEMMA( ( Token token ) -> parse( token )[LMI] ),
        /** Keep POS tag group and lemmatized form **/
        POSG(  ( Token token ) -> {
            String[] parse = parse( token );
            return String.join( SEP, parse[PGI], parse[LMI] );
        } ),
        /** Keep POS tag group, tag, and lemmatized form **/
        POSF(  ( Token token ) -> build( token ) ),
        FULL(  ( Token token ) -> build( token, true ) ),
        ;

        private final Function<Token,String> func;
        
        private LAction( Function<Token,String> func ) {
            this.func = func;
        }
        
        @Override
        public String apply( Token token ) {
            return func.apply( token );
        }
    }

    public enum NLAction implements Function<Token,String> {
        DELETE( ( Token t ) -> "" ),
        MARK(   ( Token t ) -> Tokens.NL_MRKR ),
        POSG(   ( Token t ) -> parse( t )[PGI] ),
        POSF(   ( Token t ) -> parse( t )[PFI] ),
        LEMMA(  ( Token t ) -> parse( t )[LMI] ),
        ;

        private final Function<Token,String> func;
        
        private NLAction( Function<Token,String> func ) {
            this.func = func;
        }
        
        @Override
        public String apply( Token t ) {
            return func.apply( t );
        }
    }
    
    public enum LClass implements Predicate<Token> {
        /** Adjectives: JJ, JJS, JJR **/
        ADJ( "ADJ_JJ[SR]?", new String[]{ "JJ", "JJS", "JJR" } ),
        /** Adverbs: RB, RBR, RBS, WRB **/
        ADV( "ADV_(WRB|RB[RS]?)", new String[]{ "RB", "RBR", "RBS", "WRB" } ),
        /** Articles and determinants: DT, EX, PDT, WDT **/
        ART( "ART_(DT|EX|[PW]DT)", new String[]{ "DT", "EX", "PDT", "WDT" } ),
        /** Cardinals: CD **/
        CARD( "CARD_CD", new String[]{ "CD" } ),
        /** Coordinating conjunctions: CC **/
        CONJ( "CONJ_CC", new String[]{ "CC" } ),
        /** Nouns: NN, NNS **/
        NN( "NN_NNS?", new String[]{ "NN", "NNS" } ),
        /** Proper nouns: NNP, NNPS **/
        NP( "NP_NNPS?", new String[]{ "NNP", "NNPS" } ),
        /** Catch-all for non Penn treebank tags **/
        O( "O_(\\#|``|''|\\$|FW|LS|POS|-[RL]RB-|UH)?", new String[]{ "", "#", "``", "''", "$", "FW", "LS", "POS", "-LRB-", "-RRB-", "UH" } ),
        /** Prepositions and particleds: IN, RP, TO **/
        PP( "PP_(IN|RP|TO)", new String[]{ "IN", "RP", "TO" } ),
        /** Pronouns: PR, PRP, WP, WP$ **/
        PR( "PR_(PR|W)P\\$?", new String[]{ "PRP", "PRP$", "WP", "WP$" } ),
        /** Punctuation: commas, colons, periods and SYM **/
        PUNC( "PUNC_(SYM|[,:\\.])", new String[]{ ",", ":", ".", "SYM" } ),
        /** Verbs: MD, VB, VBD, VBG, VBN, VBP, VBZ **/
        V( "V_(MD|VB[DGNPZ]?)", new String[]{ "MD", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ" } ),
        ;

        public final static Map<String,LClass> MEMBERS;
        static {
            Map<String,LClass> tmp = new HashMap<>();
            for( LClass p : LClass.values() ) {
                for( String m : p.members ) {
                    tmp.put( m, p );
                }
            }
            MEMBERS = ImmutableMap.copyOf( tmp );
        }

        /** RegEx string for this POS tag group **/
        public final String rx;
        /** POS tags in this groups **/
        public final String[] members;

        private final Automaton au;
        private final CharacterRunAutomaton cra;

        private LClass( String regexp, String[] members ) {
            this.rx = regexp;
            this.members = members;

            RegExp regex = new RegExp( regexp );
            this.au = regex.toAutomaton();
            this.cra = new CharacterRunAutomaton( au );
        }

        public Automaton au() {
            return this.au;
        }
        
        public CharacterRunAutomaton cra() {
            return this.cra;
        }

        @Override
        public boolean test( Token t ) {
            return cra.run( Tokens.pos( t ) );
        }
        
        public static Automaton make( LClass... inc ) {
            Automaton out = Automata.makeEmpty();
            for( LClass lc : inc ) {
                out = Operations.union( out, lc.au );
            }
            return out;
        }
    }
    
    public enum LSubst implements Predicate<String>, Function<String,String> {
        L_PUNCT( "[!\"#$%&()*+,-./:;<=>?@|—\\\\~{}_^'¡£¥¦§«°±³·»¼½¾¿–—‘’‚“”„†•…₤™✗]+" ),
        L_SHORT( ".{0,2}" ),
        L_MONEY( "[0-9]+-?[lds]\\.?" ),
        L_ORD(   "[0-9]*(13th|[0456789]th|1st|2nd|3rd)" ),
        L_NUMBER( "[0-9,.]+" ),
        ;
        
        private Automaton au;
        private CharacterRunAutomaton cra;
        
        private LSubst( String rx ) {
            this.au = new RegExp( rx ).toAutomaton();
            this.cra = new CharacterRunAutomaton( au );
        }
        
        public static Automaton make( LSubst... inc ) {
            Automaton out = Automata.makeEmpty();
            for( LSubst lc : inc ) {
                out = Operations.union( out, lc.au );
            }
            return out;
        }

        @Override
        public boolean test( String t ) {
            return cra.run( t );
        }

        @Override
        public String apply( String t ) {
            return cra.run( t ) ? this.name() : t;
        }
    }
}
