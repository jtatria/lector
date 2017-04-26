/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.corpus;

import java.util.Locale;
import java.util.function.Function;

import org.apache.uima.cas.text.AnnotationFS;

//import edu.columbia.incite.uima.api.types.Span;

/**
 *
 * @author gorgonzola
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
        return InciteEntities.parse( span, dump );
    }
        
    public static boolean isEntity( AnnotationFS span ) {
        return InciteEntities.isEntity( span );
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
