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

import edu.columbia.incite.analysis.index.Tokenizer;

/**
 * Enumeration of POS classes that encapsulates the tags used in a specific tag set.
 * 
 * Currently wraps the Penn Treebank POS tag set from DKPro's implementation.
 * 
 * Instances of this enumeration can be used as @link{Predicate}s for strings in lambda expressions.
 * 
 * TODO: add support for different tag sets by refactoring this enumeration into a generic interface.
 * TODO: uses Lucene's automata
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
    
    public static final POSClass[] WORDS = new POSClass[]{
        ADJ, ADV, ART, CARD, CONJ, NN, NP, O, PP, PR, V
    };
    
    public static final POSClass[] LEXICALS = new POSClass[]{
        ADJ, ADV, NN, NP, V
    };
        
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
    
    /**
     * This class's member tags.
     */
    public final String[] members;
    
    /**
     * Automaton accepting all of this class's meber tags.
     * Use this to compile new run time automata.
     */
    public final Automaton automaton;
    
    /**
     * Pre-compiled @link{CharacterRunAutomaton} from this class's member tags.
    */
    public final CharacterRunAutomaton cra;
    
    private POSClass( String[] members ) {
        this.members = members;
        Set<BytesRef> bytes = Arrays.stream( members ).map(
            ( String s ) -> new BytesRef( s.getBytes( Tokenizer.CS ) ) 
        ).collect( Collectors.toSet() );
        this.automaton = Automata.makeStringUnion( bytes );
        this.cra = new CharacterRunAutomaton( this.automaton );
    }
    
    @Override
    public boolean test( String t ) {
        return cra.run( t );
    }
    
    /**
     * Obtain the POSClass corresponding to the given POS tag's UTF8 string.
     * @param tag A POS tag, in UTF8 string format.
     * @return 
     */
    public static POSClass getPOSClass( String tag ) {
        return map.get( new BytesRef( tag.getBytes( Tokenizer.CS ) ) );
    }
    
    /**
     * Obtain the POSClass correspinding to the given bytes.
     * Bytes should correspond to a POS tag's string in UT8.
     * @param tag A POS tag, as a bytearray containing the tag's UTF8 string bytes.
     * @return 
     */
    public static POSClass getPOSClass( BytesRef tag ) {
        return map.get( tag );
    }
    
    /**
     * Create an @link{Automaton} from the union of the given POSClasses.
     * @param classes
     * @return 
     */
    public static Automaton union( POSClass... classes ) {
        List<Automaton> collect = Arrays.stream( classes ).map(
            pos -> pos.automaton 
        ).collect( Collectors.toList() );
        return Operations.union( collect );
    }
}
