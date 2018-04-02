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
package edu.columbia.incite.run.debug;

import edu.columbia.incite.analysis.AbstractProcessor;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Level;


/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Marker extends AbstractProcessor {

    private static final AtomicInteger ct = new AtomicInteger();
    private int local;

    public static final Pattern P = Pattern.compile( "(.*)\\.([A-Za-z]+)$" );
    
    public static final String PARAM_PRINT_ANNOTATION_COUNTS = "printAnnCounts";
    @ConfigurationParameter( name = PARAM_PRINT_ANNOTATION_COUNTS, mandatory = false, defaultValue = "false" )
    Boolean printAnnCounts;
    
    public static final String PARAM_QUIET = "quiet";
    @ConfigurationParameter( name = PARAM_QUIET, mandatory = false, defaultValue = "false" )
    Boolean quiet;
    
    @Override
    public void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        if( quiet ) return;
        String id = getDocumentId();
        getLogger().log( Level.INFO, "Marker engine running on cas {0}. {1} CASes seen, {2} total CASes.",
            new Object[]{ id, Integer.toString( local++ ), Integer.toString( ct.incrementAndGet() ) }
        );

        if( printAnnCounts ) {
            Map<String,Long> anns = new TreeMap<>();
            FSIterator<Annotation> it = jcas.getAnnotationIndex().iterator();
            while( it.hasNext() ) {
                Annotation ann = it.next();
                String k = ann.getType().getName();
                if( !anns.containsKey( k ) ) anns.put( k, 0l );
                anns.put( k, anns.get( k ) + 1 );
            }

            StringBuilder sb = new StringBuilder();
            sb.append( "\nDocument: " );
            sb.append( id );
            sb.append( ". Annotations:" );

            String curPkg = "";
            for( Entry<String,Long> e : anns.entrySet() ) {
                String fqtn = e.getKey();
                int sep = fqtn.lastIndexOf( TypeSystem.NAMESPACE_SEPARATOR );
                String pkg = fqtn.substring( 0, sep );
                String type = fqtn.substring( sep + 1, fqtn.length() );
                if( !curPkg.equals( pkg ) ) {
                    sb.append( String.format( "\n%s: ", pkg ) );
                    curPkg = pkg;
                }
                sb.append( String.format( "%s:%d ", type, e.getValue() ) );
            }
            sb.append( "\n" );
            getLogger().log( Level.INFO, String.format( sb.toString() ) );
        }
    }
}
