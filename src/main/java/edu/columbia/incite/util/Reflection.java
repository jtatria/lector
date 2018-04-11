/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resources;
import org.apache.uima.resource.Resource;

/**
 *
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class Reflection {
    /**
     * Populate a list of fields with all fields annotated with the given annotation class for the
     * given class.
     * This will include all fields from the given class and all of its super-classes.
     *
     * @param clazz      Class to retrieve fields for.
     *
     * @return The list, populated with fields annotated with the given annotation class for the
     *         given class and all of its super classes.
     */
    public static final List<Field> getFields( Class clazz ) {
        return Reflection.getFields( null, clazz, null );
    }
    
    public static final List<Field> getFields( Class clazz, Class annotation ) {
        return Reflection.getFields( null, clazz, annotation );
    }
    
    public static final List<Field> getFields( List<Field> tgt, Class clazz, Class annotation ) {
        tgt = tgt == null ? new ArrayList<>() : tgt;
        if( clazz.getSuperclass() != null ) {
            Reflection.getFields( tgt, clazz.getSuperclass(), annotation );
        }
        for( Field field : clazz.getDeclaredFields() ) {
            if( annotation == null ) {
                tgt.add( field );
            } else if( field.getAnnotation( annotation ) != null ) {
                tgt.add( field );
            }
        }
        return tgt;
    }
    
    /**
     * Delete all references contained in fields annotated with 
     * {@link Reflection.CasData} held by the given object.
     * 
     * @param self  The object for which resources will be released.
     */
    public static void destroyFor( Object self ) {
        for( Field field : self.getClass().getDeclaredFields() ) {
            if( field.getAnnotation( CasData.class ) != null ) {
                field.setAccessible( true );
                try {
                    field.set( self, null );
                } catch( IllegalArgumentException | IllegalAccessException ex ) {
                    Logger.getLogger( Resources.class.getName() ).log(
                    Level.SEVERE, "Resource reflection exception: {0}", ex.toString()
                );
                }
            }
        }
    }
    
    /**
     * Clear collections in fields annotated with 
     * {@link Reflection.CasData} held by the given object.
     * 
     * @param self The object whose resource collections will be cleared.
     */
    public static void clearFor( Object self ) {
        for( Field field : self.getClass().getDeclaredFields() ) {
            if( field.getAnnotation( CasData.class ) != null ) {
                field.setAccessible( true );
                try {
                    Collection col = (Collection) field.get( self );
                    col.clear();
                } catch ( IllegalArgumentException | IllegalAccessException ex ) {
                    Logger.getLogger( CasData.class.getName() ).log( Level.SEVERE, null, ex );
                }
            }
        }
    }
    
    @Retention( RetentionPolicy.RUNTIME )
    @Target( ElementType.FIELD )
    public static @interface CasData {    
    }
}
