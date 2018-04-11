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
package edu.columbia.incite.uima.util;

import java.io.File;

import edu.columbia.incite.util.Reflection;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
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
 * Utility methods to facilitate UIMA-FIT component configuration and instantiation.
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class ComponentFactory {

    /**
     * Extract all configuration parameters from UIMA-FIT annotations and build a 
     * {@link Properties} object with its default values.
     * 
     * ExternalResource values will be replaces with the class name for the necessary API. This 
     * needs to be replaced with the name of a class implementing the API for instantiation.
     * 
     * @param clz A UIMA Component class
     * @return A {@link Properties} instance with suitable key-value pairs.
     */
    public static Properties makeDefaultProperties( Class clz ) {
        return makeDefaultProperties( clz, null );
    }

    /**
     * Extract all configuration parameters from UIMA-FIT annotations and fill the given 
     * {@link Properties} instance with its default values.
     * 
     * ExternalResource values will be filled with the class name for the necessary API. This 
     * needs to be replaced with the name of a class implementing the API for instantiation.
     *  
     * If props is null, a new empty instance will be created.
     * 
     * @param clz A UIMA Component class
     * @param props An existing Properties object
     * 
     * @return The given {@link Properties} instance with suitable key-value pairs added.
     */
    public static Properties makeDefaultProperties( Class clz, Properties props ) {
        props = props != null ? props : new Properties();
        
        List<Field> fs = Reflection.getFields( clz );
        for( Field f : fs ) {
            if( isConfigurationParameter( f ) ) {
                String key = f.getAnnotation( ConfigurationParameter.class ).name();
                String[] dfvs = f.getAnnotation( ConfigurationParameter.class ).defaultValue();
                String dfv = String.join( ",", dfvs );
                props.setProperty( f.getDeclaringClass().getName() + "." + key, dfv );
            } else if( isExternalResource( f ) ) {
                String key = f.getAnnotation( ExternalResource.class ).key();
                Class<? extends Resource> api = f.getAnnotation( ExternalResource.class ).api();
                props.setProperty( f.getDeclaringClass().getName() + "." + key, api.getName() );
                props = makeDefaultProperties( api, props );
            }
        }
        
        return props;
    }
    
    /**
     * Parse the given Properties object to extract configuration values for the given UIMA 
     * component class.
     * 
     * Properties entries should have the same names as the "name" or "key" entries in UIMA-FIT 
     * annotations, fully qualified with the concrete class name, as such: 
     * {@code a.uima.component.Class.parameter}. Values should be strings compatible with UIMA-FIT.
     * 
     * @param clz A UIMA component class, annotated with UIMA-FIT annotations.
     * @param props  A Properties object.
     * 
     * @return An even-numbered sorted list with "name,value" objects.
     */
    public static List parseConfForClass( Class clz, Properties props ) {
        List out = new ArrayList();

        List<Field> fields = ReflectionUtil.getFields( clz );

        for( Field f : fields ) {
            if( isConfigurationParameter( f ) ) {
                Object value = getParameterValue( clz, f, props );
                if( value != null ) {
                    out.add( getParameterName( f ) );
                    out.add(  value );
                }
            } else if( isExternalResource( f ) ) {
                Class resClass = getExternalResourceClass( clz, f, props );
                if( resClass != null ) {
                    out.add( getExternalResourceKey( f ) );
                    out.add( makeResourceDescription( resClass, props ) );
                }
            }
        }

        return out;
    }

    /**
     * Create an external resource description from the given Properties instance for the given 
     * component class.
     * 
     * @param clz   A UIMA class implementing {@link Resource}
     * @param props A {@link Properties} instance.
     * 
     * @return A valid {@link ExternalResourceDescription} suitable for construction of the given 
     * component class instances.
     */
    public static ExternalResourceDescription makeResourceDescription(
        Class<? extends Resource> clz, Properties props
    ) {
        List conf = parseConfForClass( clz, props );
        return ExternalResourceFactory.createExternalResourceDescription( clz, conf.toArray() );
    }

    /**
     * Create an analysis engine description from the given Properties instance for the given 
     * component class.
     * 
     * @param clz   A UIMA class implementing {@link AnalysisComponent}
     * @param props A {@link Properties} instance.
     * 
     * @return A valid {@link AnalysisEngineDescription} suitable for construction of the given 
     * component class instances.
     * 
     * @throws org.apache.uima.resource.ResourceInitializationException
     */
    public static AnalysisEngineDescription makeEngineDescription(
        Class<? extends AnalysisComponent> clz, Properties props
    ) throws ResourceInitializationException {
        List conf = parseConfForClass( clz, props );
        return AnalysisEngineFactory.createEngineDescription( clz, conf.toArray() );
    }

    /**
     * Create an aggregate analysis engine description from the given primitive analysis engine 
     * descriptions.
     * 
     * Note that this method only allows linear analysis pipelines that do not require a 
     * FlowController.
     * 
     * @param primitives A list of primitive analysis engine descriptions.
     * 
     * @return A valid {@link AnalysisEngineDescription}
     * 
     * @throws org.apache.uima.resource.ResourceInitializationException
     */
    public static AnalysisEngineDescription makeAggregateDescription(
        List<AnalysisEngineDescription> primitives
    ) throws ResourceInitializationException {
        List<String> aeNames = checkNames( primitives );
        return AnalysisEngineFactory.createEngineDescription( primitives, aeNames, null, null, null );
    }

    /**
     * Create a collection reader description from the given Properties using the given component 
     * class.
     * 
     * @param clz     A class implementing {@link CollectionReader}
     * @param props     A Properties object with suitable key-value pairs (see 
     * {@link #makeDefaultProperties(java.lang.Class)} )
     * 
     * @return A valid {@link CollectionReaderDescription}
     * 
     * @throws org.apache.uima.resource.ResourceInitializationException
     */
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
        UimaFitType uimafitType = UimaFitType.forClass( type );
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
                case PATH:
                    return Paths.get( argument );
                case FILE:
                    return new File( argument );
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
        if( conf.getProperty( clz.getName() + "." + key ) != null ) {
            return parseArgument( f.getType(), conf.getProperty( clz.getName() + "." + key ) );
        } else if( conf.getProperty( f.getDeclaringClass().getName() + "." + key ) != null ) {
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
        STRING(       String.class ),
        BYTE(           Byte.class ),
        SHORT(         Short.class ),
        INTEGER(     Integer.class ),
        LONG(           Long.class ),
        FLOAT(         Float.class ),
        DOUBLE(       Double.class ),
        BOOLEAN(     Boolean.class ),
        CHARACTER( Character.class ),
        LOCALE(       Locale.class ),
        PATTERN(     Pattern.class ),
        PATH(           Path.class ),
        FILE(           File.class ),
        ;

        private static final Map<Class,UimaFitType> FOR_CLASS = new HashMap<>();
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
