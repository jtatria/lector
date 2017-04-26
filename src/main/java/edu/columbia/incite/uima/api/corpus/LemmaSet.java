/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.corpus;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RegExp;

/**
 * A collection of regular patterns for common lemmas of dubious lexicality in all syntatic
 * roles. e.g. cardinal numbers, even when the POS is not CARD.
 * Automata in this class should be tested against Lemma strings, NOT canonical forms,
 * i.e. the lemma "dog" instead of the raw text "dogs" or the canonical form "N_NN_dog".
 *
 * Lemmas can be obtained by calling the static method
 * {@link Tokens#lemma(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)}
 */
public class LemmaSet implements Predicate<String>, Function<String, String> {

    /** Aribtrary punctuation marks: **/
    public static final LemmaSet L_PUNCT = new LemmaSet( "[!\"#$%&()*+,-./:;<=>?@|—\\\\~{}_^'¡£¥¦§«°±³·»¼½¾¿–—‘’‚“”„†•…₤™✗]+", "L_PUNCT" );
    /** Two-letter-or-less lemmas **/
    public static final LemmaSet L_SHORT = new LemmaSet( ".{0,2}", "L_SHORT" );
    /** British money amounts **/
    public static final LemmaSet L_MONEY = new LemmaSet( "[0-9]+-?[lds]\\.?", "L_MONEY" );
    /** Ordinal numbers **/
    public static final LemmaSet L_ORD = new LemmaSet( "[0-9]*((1[123]|[0456789])th|1st|2nd|3rd)", "L_ORD" );
    /** Any number **/
    public static final LemmaSet L_NUMBER = new LemmaSet( "[0-9,.]+", "L_NUMBER" );
    
    private final String rx;
    private final String label;
    private final Automaton au;
    private final CharacterRunAutomaton cra;

    private LemmaSet( String rx, String label ) {
        this.rx = rx;
        this.label = label;
        this.au = new RegExp( rx ).toAutomaton();
        this.cra = new CharacterRunAutomaton( au );
    }

    /**
     * Combine the given {@link LemmaSet} values into an automaton equal to the union of
     * all given sets.
     * @param inc   An array of LemmaSet values that will be accepted by the returned automaton.
     * @return  An {@link Automaton} that will accept tokens accepted by any of the given set's
     * automata.
     */
    public static Automaton make( LemmaSet... inc ) {
        Automaton out = Automata.makeEmpty();
        Arrays.sort( inc );
        for ( LemmaSet lc : inc ) {
            out = Operations.union( out, lc.au );
        }
        return out;
    }

    /**
     * Functional interface for use as predicate in lambda expressions.
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
        return cra.run( t ) ? this.label : t;
    }

    public static Automaton au( LemmaSet... lss ) {
        Automaton out = Automata.makeEmpty();
        for ( LemmaSet ls : lss ) {
            Operations.union( out, ls.au );
        }
        return out;
    }
    
}
