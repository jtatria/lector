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

import java.util.Locale;
import java.util.function.Function;

import org.apache.uima.cas.text.AnnotationFS;

//import edu.columbia.incite.uima.api.types.Span;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Entities {
    
    // This class uses Span as base entity type. This is because it is easier to create entity 
    // types by extending Span rather than the simple Entity type (e.g. in OBO, OBOEntity extends 
    // OBOSpan extends Span, so OBOEntities are not Entities), because Entity is a fallback type.
    // This should probabaly be revised in the TypeSystem to rename Span to Entity and Entity to 
    // GenericEntity or some such.
    public static final String BASE = "ENT";
    public static final String SEP = Tokens.SEP;
    
    public static final int TYPE = 0;
    public static final int ID   = 1;
    public static final int TEXT = 2;
    public static final int DUMP = 3;
    
    public static String[] parse( AnnotationFS span ) {
        return parse( span, false );
    }
    
    public static String[] parse( AnnotationFS span, boolean dump ) {
        return Entities_Incite.parse( span, dump );
    }
        
    public static boolean isEntity( AnnotationFS span ) {
        return Entities_Incite.isEntity( span );
    }
    
    public enum EntityAction implements Function<AnnotationFS,String> {
        /** remove entities **/
        DELETE,
        /** change entities by their UIMA type **/
        TYPE,
        /** change entities by their UIMA type + id **/
        TYPE_ID,
        /** change entities by their UIMA type + id + covered text **/
        TYPE_ID_COVERED,
        /** change entities by their UIMA type + id + covered text + ent.toString() **/
        TYPE_ID_COVERED_DUMP,
        ;
        
        @Override
        public String apply( AnnotationFS t ) {
            String[] parse = parse( t );
            String[] parts = new String[this.ordinal()];
            System.arraycopy( parse, 0, parts, 0, this.ordinal() );
            return String.join( SEP, parts ).toUpperCase( Locale.ROOT );
        }
    }
}
