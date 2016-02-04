/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.tools;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;

/**
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public abstract class ComponentFactory {
    
    public static List parseConfForClass( Class clz, Properties conf ) {
        List out = new ArrayList();
        
        List<Field> fields = ReflectionUtil.getFields( clz );
        
        for( Field f : fields ) {
            if( isConfigurationParameter( f ) ) {
                Object value = getParameterValue( clz, f, conf );
                if( value != null ) {
                    out.add( getParameterName( f ) );
                    out.add(  value );
                }
            } else if( isExternalResource( f ) ) {
                Class resClass = getExternalResourceClass( clz, f, conf );
                if( resClass != null ) {
                    out.add( getExternalResourceKey( f ) );
                    out.add( makeResourceDescription( resClass, conf ) );
                }
            }
        }
        
        return out;
    }
    
    public static ExternalResourceDescription makeResourceDescription( 
        Class<? extends Resource> clz, Properties props 
    ) {
        List conf = parseConfForClass( clz, props );
        return ExternalResourceFactory.createExternalResourceDescription( clz, conf.toArray() );
    }
    
    public static AnalysisEngineDescription makeEngineDescription(
        Class<? extends AnalysisComponent> clz, Properties props
    ) throws ResourceInitializationException {
        List conf = parseConfForClass( clz, props );
        return AnalysisEngineFactory.createEngineDescription( clz, conf.toArray() );
    }
    
    
    public static AnalysisEngineDescription makeAggregateDescription( 
        List<AnalysisEngineDescription> primitives 
    ) throws ResourceInitializationException {
        List<String> aeNames = checkNames( primitives );
        return AnalysisEngineFactory.createEngineDescription( primitives, aeNames, null, null, null );
    }
        
    public static CollectionReaderDescription makeReaderDescription(
        Class<? extends CollectionReader> clz, Properties props 
    ) throws ResourceInitializationException {
        List conf = parseConfForClass( clz, props );
        return CollectionReaderFactory.createReaderDescription( clz, conf.toArray() );
    }

    private static List<String> checkNames(
        List<AnalysisEngineDescription> aes
    ) {
                List<String> names = new ArrayList<>();
        for( AnalysisEngineDescription ae : aes ) {
            String name = ae.getAnnotatorImplementationName();
            int i = 0;
            while( names.contains( name ) ) {
                name = name + Integer.toString( i++ );
            }
            names.add( name );
        }
        return names;
    }
    
    private static Object parseArgument( Class type, String argument  ) {
        UimaFitType uimafitType = UimaFitType.forClass(type );
        if( uimafitType != null ) {
            switch( uimafitType ) {
                case STRING:
                    return argument;
                case BYTE:
                    return Byte.decode( argument );
                case SHORT:
                    return Short.decode( argument );
                case INTEGER:
                    return Integer.decode( argument );
                case LONG:
                    return Long.decode( argument );
                case FLOAT:
                    return Float.parseFloat( argument );
                case DOUBLE:
                    return Double.parseDouble( argument );
                case BOOLEAN:
                    return Boolean.parseBoolean( argument );
                case CHARACTER:
                    return argument;
                case LOCALE:
                    return Locale.forLanguageTag( argument );
                case PATTERN:
                    return Pattern.compile( argument );
                default: throw new AssertionError();
            }
        } else if( type.isArray() ) {
            String[] parts = argument.split( ",");
            Object[] values = new Object[parts.length];
            for( int i = 0; i < parts.length; i++ ) {
                values[i] = parseArgument( type.getComponentType(), parts[i] );
            }
            return values;
        }
        return null;
    }

    private static boolean isConfigurationParameter( Field f ) {
        return f.isAnnotationPresent( ConfigurationParameter.class );
    }

    private static Object getParameterValue( Class clz, Field f, Properties conf ) {
        String key = f.getAnnotation( ConfigurationParameter.class ).name();
        if( conf.getProperty(clz.getName() + "." + key ) != null ) {
            return parseArgument( f.getType(), conf.getProperty( clz.getName() + "." + key ) );
        } else if( conf.getProperty(f.getDeclaringClass().getName() + "." + key ) != null ) {
            return parseArgument( f.getType(), conf.getProperty( f.getDeclaringClass().getName() + "." + key ) );
        }
        else return null;
    }

    private static Object getParameterName( Field f ) {
        return f.getAnnotation( ConfigurationParameter.class ).name();
    }

    private static boolean isExternalResource( Field f ) {
        return f.isAnnotationPresent( ExternalResource.class );
    }

    private static Class getExternalResourceClass( Class clz, Field f, Properties conf ) {
        String key = f.getAnnotation( ExternalResource.class ).key();
        
        try {
            if( conf.getProperty( clz.getName() + "." + key ) != null ) {
                return Class.forName( conf.getProperty( clz.getName() + "." + key ) );
            } else if( conf.getProperty( f.getDeclaringClass().getName() + "." + key ) != null ) {
                return Class.forName( conf.getProperty( f.getDeclaringClass().getName() + "." + key ) );
            }
        } catch( ClassNotFoundException ex ) {
            throw new RuntimeException( "Unknown class requested for External Resource.", ex );
        }
        return null;
    }

    private static Object getExternalResourceKey( Field f ) {
        return f.getAnnotation( ExternalResource.class ).key();
    }
    
    private enum UimaFitType {
        STRING( String.class ),
        BYTE( Byte.class ),
        SHORT( Short.class ),
        INTEGER( Integer.class ),
        LONG( Long.class ),
        FLOAT( Float.class ),
        DOUBLE( Double.class ),
        BOOLEAN( Boolean.class ),
        CHARACTER( Character.class ),
        LOCALE( Locale.class ),
        PATTERN( Pattern.class )
        ;
        
        static Map<Class,UimaFitType> FOR_CLASS = new HashMap<>();
        static{
            for( UimaFitType t : UimaFitType.values() ) {
                FOR_CLASS.put( t.clz, t );
            }
        }
        
        private Class clz;
        
        private <T> UimaFitType( Class<T> clz ) {
            this.clz = clz;
        }
        
        public static UimaFitType forClass( Class clz ) {
            return FOR_CLASS.get( clz );
        }
    }
}
