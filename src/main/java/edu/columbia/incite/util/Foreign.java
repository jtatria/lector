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
package edu.columbia.incite.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;

import edu.columbia.incite.corpus.POSClass;
import edu.columbia.incite.run.Logs;
import edu.columbia.incite.run.Memory;
import edu.columbia.incite.run.Progress.Log;
import edu.columbia.incite.run.Progress;

/**
 * Convenience methods for calling Lector from outside Java, typically R.
 * 
 * This class basically does two things: converts java lists to plain arrays and releases memory 
 * from objects created externally.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Foreign {
    
    /**
     * Wrapper for {@link LeafReader#terms(java.lang.String) }
     * 
     * @param lr    A LeafReader instance.
     * @param field A field name
     * @return A {@code String[]} containing all terms found in the requested field.
     * 
     * @throws IOException if thrown by the given LeafReader
     */
    public static String[] terms( LeafReader lr, String field ) throws IOException {
        if( lr == null ) throw new IllegalArgumentException( "lr can't be null" );
        if( field == null ) throw new IllegalArgumentException( "field can't be null" );
        Terms terms = lr.terms( field );
        if( terms == null ) return new String[0];
        TermsEnum tEnum = terms.iterator();
        List<String> out = new ArrayList();
        while( tEnum.next() != null ) {
            out.add( tEnum.term().utf8ToString() );
        }
        return out.toArray( new String[ out.size() ] );
    }
    
    /**
     * Wrapper for {@link LeafReader#fields() }
     * 
     * @param lr    A LeafReader instance.
     * @return A {@code String[]} containing all terms found in the requested field.
     * 
     * @throws IOException if thrown by the given LeafReader
     */
    public static String[] fields( LeafReader lr ) throws IOException {
        if( lr == null ) throw new IllegalArgumentException( "LeafReader can't be null" );
        Fields flds = lr.fields();
        if( flds == null ) return new String[0];
        List<String> out = new ArrayList();
        for( String f : flds ) {
            out.add( f );
        }
        return out.toArray( new String[ out.size() ] );
    }

    /**
     * Obtain a copy of {@link POSClass#WORDS } as a String array.
     * 
     * @return A {@code String[]} containing all POSClass values included in {@link POSClass#WORDS}.
     */
    public static String[] posClasses() {
        return Arrays.stream( POSClass.WORDS ).map(
            ( p ) -> p.name()
        ).toArray( ( l ) -> new String[l] );
    }

    /**
     * Obtain a copy of {@link POSClass#LEXICALS } as a String array.
     * 
     * @return A {@code String[]} containing all POSClass values included in {@link POSClass#LEXICALS}.
     */    
    public static String[] lexClasses() {
        return Arrays.stream( POSClass.LEXICALS ).map(
            ( p ) -> p.name()
        ).toArray( ( l ) -> new String[l] );
    }
    
    /**
     * Attempt to release memory from the given object.
     * 
     * This is typically used to recover memory from the intermediate objects created by external 
     * method calls after the data returned by those calls has been copied to foreign data 
     * structures by the external caller e.g. the arrays returned by methods in this class should 
     * be copied to external native data structures before being released with this method to 
     * recover the java memory used by them.
     * 
     * @param <T> Type for {@code data}.
     * @param data Some Object.
     * @return {@code data}, set to null.
     */
    public static <T> T release( T data ) {
        return Memory.release( data );
    }
    
    /** 
     * Get a safe {@link Progress} report that will print messages to a log instead of trying to 
     * control a PrintStream directly.
     * 
     * @return A {@link Progress} instance safer for external callers than the one returned by 
     * default.
     */
    public static Progress report() {
        return new Log( Logs.logger(), Level.INFO );
    }
}
