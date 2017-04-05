/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.util;

import java.lang.reflect.Field;
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

import static edu.columbia.incite.util.reflex.ReflectionUtils.getFields;

/**
 * Utility methods to facilitate UIMA-FIT component configuration and instantiation.
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public abstract class ComponentFactory {

    /**
     * Extract all configuration parameters from UIMA-FIT annotations and build a 
     * {@link Properties} object with its default values.
     * @param clz A UIMA Component class
     * @return A {@link Properties} instance with suitable key-value pairs.
     */
    public static Properties makeDefaultProperties( Class clz ) {
        return makeDefaultProperties( clz, null );
    }

    /**
     * Extract all configuration parameters from UIMA-FIT annotations and fill the given 
     * {@link Properties} instance with its default values.
     * If props is null, a new empty instance will be created.
     * @param clz A UIMA Component class
     * @param props An existing Properties object
     * @return The given {@link Properties} instance with suitable key-value pairs added.
     */
    public static Properties makeDefaultProperties( Class clz, Properties props ) {
        props = props != null ? props : new Properties();
        
        List<Field> fs = getFields( clz );
        for( Field f : fs ) {
            if( isConfigurationParameter( f ) ) {
                String key = f.getAnnotation( ConfigurationParameter.class ).name();
                String[] dfvs = f.getAnnotation( ConfigurationParameter.class ).defaultValue();
                String dfv = String.join( ",", dfvs );
                props.setProperty( f.getDeclaringClass().getName() + "." + key, dfv );
            } else if( isExternalResource( f ) ) {
                String key = f.getAnnotation( ExternalResource.class ).key();
                props.setProperty( f.getDeclaringClass().getName() + "." + key, "RESOURCE_IMPL_CLASS_HERE" );
                Class<? extends Resource> api = f.getAnnotation( ExternalResource.class ).api();
                props = makeDefaultProperties( api, props );
            }
        }
        
        return props;
    }
    
    /**
     * Parse the given Properties object to extract conf values for the given UIMA component class.
     * Properties should be have the same names as the "name" entries in UIMA-FIT annotations, 
     * fully qualified with the concrete class name, as such: a.uima.component.Class.parameter.
     * @param clz A UIMA component class, annotated with UIMA-FIT annotations.
     * @param conf  A Properties object.
     * @return An even-numbered sorted list with "name,value" objects.
     */
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

    /**
     * Create an external resource description from the given Properties instance for the given 
     * component class.
     * @param clz   A UIMA class implementing {@link Resource}
     * @param props A {@link Properties} instance.
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
     * @param clz   A UIMA class implementing {@link AnalysisComponent}
     * @param props A {@link Properties} instance.
     * @return A valid {@link AnalysisEngineDescription} suitable for construction of the given 
     * component class instances.
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
     * Note that this method only allows linear analysis pipelines that do not require a 
     * FlowController.
     * @param primitives A list of primitive analysis engine descriptions.
     * @return A valid {@link AnalysisEngineDescription}
     * @throws org.apache.uima.resource.ResourceInitializationException
     */
    public static AnalysisEngineDescription makeAggregateDescription(
        List<AnalysisEngineDescription> primitives
    ) throws ResourceInitializationException {
        List<String> aeNames = checkNames( primitives );
        return AnalysisEngineFactory.createEngineDescription( primitives, aeNames, null, null, null );
    }

    /**
     * Create a collection reader description from the given Properties.
     * @param clz     A class implementing {@link CollectionReader}
     * @param props     A Properties object with suitable key-value pairs (see 
     * {@link #makeDefaultProperties(java.lang.Class)} )
     * @return A valid {@link AnalysisEngineDescription}
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
