/*
 * Copyright (C) 2015 Jose Tomas Atria <jtatria@gmail.com>
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
package edu.columbia.incite.uima.ae;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.types.Document;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class Engine extends AbstractEngine {

    private static int global = 0;
    private int local;

    @Override
    public void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        Document dmd = jcas.getAnnotationIndex( Document.class ).iterator().next();
        getLogger().log( Level.INFO, "Marker engine running on cas {0}. {1} CASes seen, {2} total CASes.",
            new Object[]{ dmd.getId(), Integer.toString( local++ ), Integer.toString( global++ ) }
        );

        Map<String,Long> anns = new HashMap<>();
        FSIterator<Annotation> it = jcas.getAnnotationIndex().iterator();
        while( it.hasNext() ) {
            Annotation ann = it.next();
            String k = ann.getType().getShortName();
            if( !anns.containsKey( k ) ) anns.put( k, 0l );
            anns.put( k, anns.get( k ) + 1 );
        }

        for( Entry<String,Long> e : anns.entrySet() ) {
            String msg = String.format( "%s\t%d", e.getKey(), e.getValue() );
            getLogger().log( Level.INFO, String.format( "Found annotations: %s", msg ) );
        }
    }
}
