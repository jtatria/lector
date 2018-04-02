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
package edu.columbia.incite.io.charstreams;


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class BufferFilter extends Resource_ImplBase implements CharStreamProcessor {
        
    public static final String PARAM_FORMAT_PATTERNS = "formatStrings";
    @ConfigurationParameter( name = PARAM_FORMAT_PATTERNS, mandatory = false )
    private String[] formatStrings = new String[]{
//        "^\\s+$", "", // delete empty.
        "\\n", " ",     // remove new lines.
        "\\s+", " ",    // collapse whitespace
//        "\\s+$", "",  // trim tail
//        "^\\s+", "",  // trim head
//        "(\\p{Alnum})\\s+(\\p{Punct})", "$1$2", // remove space before punctuation
    };
    
    public static final String RES_SPLIT_CHECK = "splitCheck";
    @ExternalResource( key = RES_SPLIT_CHECK, api = SplitCheck.class, mandatory = false )
    private SplitCheck splitCheck; 
    
    private final Map<Pattern,String> normalRules = new HashMap<>();

    private final ThreadLocal<StringBuilder> buffer = ThreadLocal.withInitial( () -> new StringBuilder() );
    private final ThreadLocal<Boolean> split = ThreadLocal.withInitial( () -> false );
    private final ThreadLocal<String> splitMark = ThreadLocal.withInitial( () -> null );
    
    @Override
    public boolean initialize( ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams )
        throws ResourceInitializationException {
        boolean ret = super.initialize( aSpecifier, aAdditionalParams );
        
        if( formatStrings != null && formatStrings.length % 2 != 0 ) {
            throw new ResourceInitializationException( new IllegalArgumentException(
                "Arguments for configuration parameter '" + PARAM_FORMAT_PATTERNS + "' must come "
                + "in key/value pairs but found an odd number of arguments: [" 
                + formatStrings.length + "]."
            ) );
        } else {
            cookPatterns( formatStrings );
        }
        
        return ret;
    }
    
    @Override
    public void consume( char[] chars, int offset, int length ) {
        String chunk = normalize( new String( chars, offset, length ) );
        StringBuilder myBldr = buffer.get();
        
        if( split.get() ) {
            String pre = getLastWord();
            String pos = getIncomringWord( chunk );
            if( !pre.isEmpty() && !pos.isEmpty() ) {
                if( checkSplit( pre, pos, splitMark.get() ) ) myBldr.append( EOW );
            }
            split.remove();
            splitMark.remove();
        }
        
        char last = myBldr.length() > 0 ? myBldr.charAt( myBldr.length() - 1 ) : '\u0000';
        char inc  = chunk.charAt( 0 );
        
        if( last == '\u0000' || Character.isWhitespace( last ) ) {
            while( Character.isWhitespace( inc ) && chunk.length() > 0 ) {
                chunk = chunk.substring( 1 );
                if( "".equals( chunk ) ) return;
                inc = chunk.charAt( 0 );
            }
        }
        
        buffer.get().append( chunk );
    }

    private String getLastWord() {
        if( buffer.get().length() == 0 ) return "";
        StringBuilder bldr = new StringBuilder();
        int pos = buffer.get().length();
        while( Character.isWhitespace( buffer.get().charAt( --pos ) ) ) {
            bldr.append( buffer.get().charAt( pos ) );
        }
        return bldr.reverse().toString();
    }

    private String normalize( String chunk ) {
        String out = chunk;
        for( Entry<Pattern,String> e : normalRules.entrySet() ) {
            out = e.getKey().matcher( out ).replaceAll( e.getValue() );
        }
        return out;
    }

    private String getIncomringWord( String chunk ) {
        if( "".equals( chunk ) ) return "";
        StringBuilder bldr = new StringBuilder();
        int pos = 0;
        while( pos < chunk.length() && Character.isWhitespace( chunk.charAt( pos ) ) ) {
            bldr.append( chunk.charAt( pos++ ) );
        }
        return bldr.toString();
    }

    private boolean checkSplit( String pre, String pos, String tag ) {
        return splitCheck != null ? splitCheck.split( pre, pos, tag ) : false;
    }

    @Override
    public void addBreak( String brk ) {
        if( brk.equals( EOL ) ) {
            buffer.get().append( EOL );
        } else {
            split.set( true );
            splitMark.set( brk );
        }
    }

    @Override
    public int curLength() {
        return buffer.get().length();
    }

    @Override
    public String result() {
        return buffer.get().toString();
    }

    @Override
    public void reset() {
        buffer.remove();
    }

    private void cookPatterns( String[] formatStrings ) {
        for( int i = 0; i < formatStrings.length / 2; i++ ) {
            String in = formatStrings[ i * 2 ];
            String out = formatStrings[ i * 2 + 1 ];
            Pattern rule = Pattern.compile( in );
            normalRules.put( rule, out );
        }
    }
}
