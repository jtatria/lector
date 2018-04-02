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
 *
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class Automatons {
    
    public static Automaton union( String... terms ) {
        List<String> list = Arrays.asList( terms );
        Collections.sort( list );
        List<BytesRef> brs = list.stream().map(
            s -> new BytesRef( s )
        ).collect( Collectors.toList() );
        return Automata.makeStringUnion( brs );
    }
    
    public static Automaton concatenate( Automaton... aus ) {
        List<Automaton> list = new ArrayList<>();
        for( Automaton au : aus ) {
            list.add( au );
        }
        return Operations.concatenate( list );
    }
}
