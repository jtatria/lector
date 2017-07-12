/* 
 * Copyright (C) 2017 José Tomás Atria <jtatria at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.columbia.incite.corpus;

import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * @author José Tomás Atria <jtatria@gmail.com>
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
    /** Catch-all for non PTB tags **/
    O( new String[ ]{ "", "#", "$", "''", "-LRB-", "-RRB-", "FW", "LS", "POS", "UH", "``" } ),
    /** Prepositions and particles: IN, RP, TO **/
    PP( new String[ ]{ "IN", "RP", "TO" } ),
    /** Pronouns: PR, PRP, WP, WP$ **/
    PR( new String[ ]{ "PRP", "PRP$", "WP", "WP$" } ),
    /** Verbs: MD, VB, VBD, VBG, VBN, VBP, VBZ **/
    V( new String[ ]{ "MD", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ" } ),
    /** Punctuation: commas, colons, periods and SYM **/
    PUNC( new String[ ]{ ",", ".", ":", "SYM" } ),
    ;
    
    private final static Map<BytesRef,POSClass> map;
    static {
        Map<BytesRef,POSClass> tmp = new HashMap<>();
        for( POSClass pc : POSClass.values() ) {
            for( String tag : pc.members ) {
                tmp.put( new BytesRef( tag ), pc );
            }
        }
        map = ImmutableMap.copyOf( tmp );
    }
    
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
    
    public static POSClass getPOSClass( BytesRef tag ) {
        return map.get( tag );
    }
    
    public static Automaton make( POSClass... classes ) {
        List<Automaton> collect = Arrays.stream( classes ).map( au -> au.automaton ).collect( Collectors.toList() );
        return Operations.union( collect );
    }
    
    
}
