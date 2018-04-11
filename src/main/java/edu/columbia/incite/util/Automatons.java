/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.Operations;

/**
 * Utility methods for dealing with Lucene's automata.
 * 
 * (Yes, the plural of automaton is automata, but Lucene already has a class with that name).
 * 
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class Automatons {
    
    /**
     * Create an automaton that will accept the union of the given terms.
     * 
     * Note that the given terms will be sorted for performance reasons.
     * 
     * @param terms Terms to include in the resulting automaton.
     * 
     * @return An automaton that will accept any of the given terms.
     */
    public static Automaton union( String... terms ) {
        List<String> list = Arrays.asList( terms );
        Collections.sort( list );
        List<BytesRef> brs = list.stream().map(
            s -> new BytesRef( s )
        ).collect( Collectors.toList() );
        return Automata.makeStringUnion( brs );
    }
    
    /**
     * Concatenate the given automata.
     * 
     * @param aus Automata to concatenate.
     * 
     * @return An automaton that will accept a string that is equal to a concatenation of 
     *         strings that would be accepted by the given automata.
     */
    public static Automaton concatenate( Automaton... aus ) {
        List<Automaton> list = new ArrayList<>();
        for( Automaton au : aus ) {
            list.add( au );
        }
        return Operations.concatenate( list );
    }
}
