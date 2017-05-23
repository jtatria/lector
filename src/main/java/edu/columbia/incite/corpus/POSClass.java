/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.corpus;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;

/**
 *
 * @author jta
 */
public enum POSClass implements Predicate<String> {
    /** Adjectives: JJ, JJS, JJR **/
    ADJ( new String[ ]{ "JJ", "JJR", "JJS" } ),
    /** Adverbs: RB, RBR, RBS, WRB **/
    ADV( new String[ ]{ "RB", "RBR", "RBS", "WRB" } ),
    /** Articles and determinants: DT, EX, PDT, WDT **/
    ART( new String[ ]{ "DT", "EX", "PDT", "WDT" } ),
    /** Cardinals: CD **/
    CARD( new String[ ]{ "CD" } ),
    /** Coordinating conjunctions: CC **/
    CONJ( new String[ ]{ "CC" } ),
    /** Nouns: NN, NNS **/
    NN( new String[ ]{ "NN", "NNS" } ),
    /** Proper nouns: NNP, NNPS **/
    NP( new String[ ]{ "NNP", "NNPS" } ),
    /** Catch-all for non Penn treebank tags **/
    O( new String[ ]{ "", "#", "$", "''", "-LRB-", "-RRB-", "FW", "LS", "POS", "UH", "``" } ),
    /** Prepositions and particleds: IN, RP, TO **/
    PP( new String[ ]{ "IN", "RP", "TO" } ),
    /** Pronouns: PR, PRP, WP, WP$ **/
    PR( new String[ ]{ "PRP", "PRP$", "WP", "WP$" } ),
    /** Punctuation: commas, colons, periods and SYM **/
    PUNC( new String[ ]{ ",", ".", ":", "SYM" } ),
    /** Verbs: MD, VB, VBD, VBG, VBN, VBP, VBZ **/
    V( new String[ ]{ "MD", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ" } ),
    ;
        
    public final String[] members;
    public final Automaton automaton;
    public final CharacterRunAutomaton cra;
    
    private POSClass( String[] members ) {
        this.members = members;
        
        Set<BytesRef> bytes = Arrays.stream( members ).map(
            ( String s ) -> new BytesRef( s.getBytes( StandardCharsets.UTF_8 ) ) 
        ).collect( Collectors.toSet() );
        
        this.automaton = Automata.makeStringUnion( bytes );
        this.cra = new CharacterRunAutomaton( this.automaton );
    }
    
    @Override
    public boolean test( String t ) {
        return cra.run( t );
    }
    
    public static Automaton make( POSClass... classes ) {
        List<Automaton> collect = Arrays.stream( classes ).map( au -> au.automaton ).collect( Collectors.toList() );
        return Operations.union( collect );
    }
}
