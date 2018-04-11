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
package edu.columbia.incite.uima.tools;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.resource.Resource;

/**
 * TODO: document this.
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public interface TextFilter extends Resource {
    
    public static final String EOL = "\n";
    public static final String EOW = " ";
    
    /**
     * Consume the given characters.
     * 
     * Read in characters from the given buffer and add them to the internal buffer, applying any 
     * processing necessary.
     * 
     * @param chars A character array to consume characters from..
     * @param offset A position to start reading characters in the array.
     * @param length Number of characters to read.
     */
    void consume( char[] chars, int offset, int length );
    
    /**
     * Signal to this processor that an in-line mark has occurred.
     * 
     * This is usually reserved for new line characters, but it allows for a string key to be 
     * passed to deal with mixed-content XML that may indicate newlines as XML elements.
     * 
     * @param brk An optional String to indicate different in-line marks.
     */
    void addBreak( String brk );
    
    /**
     * Size of accumulated text in characters.
     * @return This filter's internal buffer length.
     */
    int curLength();
    
    /**
     * Get output.
     * 
     * Returns all accumulated text so far.
     * 
     * @return A string containing all of the text accumulated by this filter.
     */
    String result();

    /**
     * Reset filter Destroys accumulated text..
     */
    void reset();

    /**
     * Thread-safe no-op filter implementation.
     */
    public static class Noop extends Resource_ImplBase implements TextFilter {

        private final ThreadLocal<StringBuilder> buffer = ThreadLocal.withInitial( 
            () -> new StringBuilder() 
        );
        
        @Override
        public void consume( char[] chars, int offset, int length ) {
            buffer.get().append( new String( chars, offset, length ) );
        }

        @Override
        public void addBreak( String brk ) {
            if( brk.equals( TextFilter.EOL ) ) buffer.get().append( TextFilter.EOL );
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
        
    }
    
}
