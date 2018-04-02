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

import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RegExp;

/**
 * Class that implements an abstract named string set using Lucene's @link{Automata}.
 * 
 * This class is used to implement all term sets used for deletion or substitutions.
 * 
 * In the latter case, this class's contract will replace any member in the set by the set's 
 * name.
 * 
 * Instances of this class can be used as @link{Predicate} and @link{Function} taking @link{String} 
 * inputs in lambda expressions.
 * 
 * As predicate, it returns @code{true} if the passed string is contained in this set. As function, 
 * replaces the given string by this set's name iff the given string is contained in this set.
 * 
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class TermSet implements Predicate<String>, Function<String,String> {

    /**
     * The universal set; i.e. contains all strings. 
     */
    public static final TermSet ANY   = new TermSet( Automata.makeAnyString(), "ANY"   );
    
    /**
     * The empty set; i.e. contains no strings.
     */
    public static final TermSet EMPTY = new TermSet( Automata.makeEmpty(),     "EMPTY" );
        
    private final String rx;
    private final String label;
    private final Automaton au;
    private final CharacterRunAutomaton cra;

    /**
     * Advanced: Build a new @link{TermSet} from the given automaton with the given name.
     * @param au    An @link{Automaton}
     * @param label A string to use as this set's name.
     */
    public TermSet( Automaton au, String label ) {
        this.rx    = null;
        this.label = label;
        this.au    = au;
        this.cra   = new CharacterRunAutomaton( au );
    }

    /**
     * Construct a new @link{TermSet} from the given regular expression.
     * NB: uses Lucene's @link{RegExp} language.
     * @param rx    A regular expression string to be interpreted using @link{RegExp}'s language.
     * @param label A string to use as this set's name.
     */
    public TermSet( String rx, String label ) {
        this.rx    = rx;
        this.label = label;
        this.au    = new RegExp( rx ).toAutomaton();
        this.cra   = new CharacterRunAutomaton( au );
    }

    /**
     * Obtain this term set's compiled automaton.
     * @return A @link{CharacterRunAutomaton} containing this set's definition.
     */
    public CharacterRunAutomaton cra() {
        return this.cra;
    }
    
    /**
     * Add the given @link{TermSet} to this one.
     * 
     * Keeps this set's name and adds all elements in the given set to this set.
     * 
     * @param other A @link{TermSet}
     * @return This set, now also containing all elements of the given set not already contained 
     *         in this set.
     */
    public TermSet merge( TermSet other ) {
        return union( this.label, this, other );
    }
    
    @Override
    public boolean test( String t ) {
        return cra.run( t );
    }

    @Override
    public String apply( String t ) {
        return cra.run( t ) ? this.label : t;
    }

    @Override
    public String toString() {
        return String.format( "%s : %s", this.label, this.rx );
    }

    /**
     * Default name for term sets created as an unnamed union of a set of @link{TermSet}s.
     */
    public static final String UNION = "UNION";
    
    /**
     * Create a new @link{TermSet} from the union of the given sets.
     * @param label A label for the new set. If null, the new set will use @link{TermSet.UNION} as 
     *              name.
     * @param inc   @link{TermSets} to include in the resulting set.
     * @return A @link{TermSet} corresponding to the union of the given sets, with the given name 
     *         or the default name is the given name is null.
     */
    public static TermSet union( String label, TermSet... inc ) {
        Automaton out = Automata.makeEmpty();
        // TODO: LemmaSet is not comparable, but we should still optimize automata for unions.
        // Arrays.sort( inc ); 
        for ( TermSet ts : inc ) {
            out = Operations.union( out, ts.au );
        }
        // MinimizationOperations.minimize( out, Operations.DEFAULT_MAX_DETERMINIZED_STATES );
        return new TermSet( out, label == null ? UNION : label );
    }
}
