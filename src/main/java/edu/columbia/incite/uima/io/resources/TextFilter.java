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
package edu.columbia.incite.uima.io.resources;

import org.apache.uima.resource.Resource;

/**
 * Resource to filter text from a data source before adding it to a CAS's document text.
 * The interface offers three methods: {@link #normalize(String)} applies the replacement rules
 * passed as configuration parameters and returns the filtered string;
 * {@link #appendToBuffer(StringBuffer, String)} will apply the transformation rules to an incoming
 * chunk and resolve collisions between the filtered chunk and the string already contained in the 
 * buffer before appending the chunk to the buffer; {@link #breakLine(StringBuffer)} will insert 
 * whatever characters are used to mark a line break and optionally remove trailing spaces in the 
 * string already contained in the buffer.
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public interface TextFilter extends Resource {
    
    /**
     * Apply all substitution rules to the given {@code chunk} and return its canonical form.
     *
     * @param chunk The chunk to be formatted.
     *
     * @return The string corresponding to the incoming chunk after all substitution rules have
     *         been applied.
     */
    String normalize( String chunk );

    /**
     * Append the given {@code chunk} to the given {@code target} buffer.
     * By contract, this will first apply all substitution rules as if the {@link #normalize(String)}
     * method had been called on the {@code chunk}, then resolve collisions as defined in the
     * current configuration.
     *
     * @param target The chunk to be formatted.
     * @param chunk  The buffer to which the formatted string will be appended.
     */
    void appendToBuffer( StringBuffer target, String chunk );
    
    /**
     * Communicate to this filter that the next buffer update may occur mid-word and needs to be 
     * checked.
     * @param mark
     */
    void mark( String mark );
}
