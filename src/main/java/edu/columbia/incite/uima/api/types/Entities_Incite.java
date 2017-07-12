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
package edu.columbia.incite.uima.api.types;

import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.types.Span;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Entities_Incite {
    
    public static String[] parse( AnnotationFS ann, boolean dump ) {
        if( !isEntity( ann ) ) throw new IllegalArgumentException();
        Span span = (Span) ann;
        String type  = span.getType().getShortName();
        String id    = span.getId();
        String text  = span.getCoveredText();
        String dumps = dump ? span.toString() : "";
        return new String[]{ type, id, text, dumps };
    }
    
    public static boolean isEntity( AnnotationFS ann ) {
        return ann.getClass().isAssignableFrom( Span.class );
    }
}
