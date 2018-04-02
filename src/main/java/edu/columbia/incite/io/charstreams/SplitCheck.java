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

import org.apache.uima.resource.Resource;

/**
 * Interface for an object capable of determining whether two consecutive non-whitespace character 
 * sequences in an input character stream should be interpreted as one continuous sequence or two 
 * separate sequences. The paradigmatic use of this interface is in the normalization and processing
 * of an XML document's character stream in a SAX content handler.
 * 
 * This is usually necessary given the joys of XML parsing, since the SAX specification (the XML 
 * standard, really) imposes no constraint over when or how a SAX handler's
 * @link{ContentHandler.character} method will be called when processing mixed-content XML 
 * documents. E.g. if a source uses XML elements to indicate newline or paragraph breaks that split 
 * words, the parts of the word before and after the break will be passed to a handler in different 
 * chunks.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public interface SplitCheck extends Resource {
    
    /**
     * Determine whether the given strings representing two consecutive character streams should be 
     * interpreted as parts of one continuous string or should be split into two separate strings.
     * 
     * The tag parameter is used to pass the name of XML tags that may have occurred in between the
     * two segments of the character stream in a mixed-content XML document.
     * 
     * @param pre A string representing the first (leftmost) char sequence.
     * @param pos A string representing the second (rightmost) char sequence.
     * @param tag The name of any XML tags found in between the two stream segments.
     * 
     * @return {@code true} if the given strings should be split, {{@code false} if they should be 
     *         joined.
     */
    boolean split( String pre, String pos, String tag );
    
}
