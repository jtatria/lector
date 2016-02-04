/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.tools.casio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.DataFieldType;

/**
 * An implementation of a {@link FeatureBroker} that extracts all values found in an annotation with no prior knowledge.
 * 
 * Primitive feature data is always added; Built-in UIMA features are excluded by default by may be included. Non-primitive feature values are either ommitted or dereferenced recursively.
 * 
 * Internally, this feature broker will build and maintain a collection of feature paths to access data from different annotation types as they are found.
 * 
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class FeatureExtractor extends Resource_ImplBase implements FeatureBroker<Datum> {
    
    /**
     * Omit built-in UIMA features (begin, end, sofa, etc). Enabled by default.
     */
    public static final String PARAM_OMIT_BUILTINS = "omitBuiltIns";
    @ConfigurationParameter( name = PARAM_OMIT_BUILTINS, mandatory = false, defaultValue = "true" )
    private boolean omitBuiltIns = true;
    
    /**
     * Follow non-primitive feature values and add values from referenced features.
     */
    public static final String PARAM_DEREFERENCE = "dereference";
    @ConfigurationParameter( name = PARAM_DEREFERENCE, mandatory = false, defaultValue = "true" )
    private boolean dereference = false;
    
    /**
     * Include SOFA string in extracted values. Disabled by default.
     */
    public static final String PARAM_INCLUDE_SOFASTRING = "includeSofa";
    @ConfigurationParameter( name = PARAM_INCLUDE_SOFASTRING, mandatory = false, defaultValue = "true" )
    private boolean includeSofa = false;
    
    private Map<Type,List<FeaturePath>> cache = new ConcurrentHashMap<>();
    
    @Override
    public Datum values( AnnotationFS ann ) throws CASException {
        Datum d = new Datum();
        values( ann, d );
        return d;
    }

    @Override
    public void values( AnnotationFS ann, Datum tgt ) throws CASException {
        Type type = getEffectiveType( ann );
        List<FeaturePath> featurePaths = getFeaturePaths( type, ann );
        for( FeaturePath fp : featurePaths ) addData( fp, ann, tgt );
//        {
//            fp.typeInit( type );
            
//        }
    }
    
    protected void addData( FeaturePath fp, AnnotationFS ann, Datum tgt ) throws CASException {
        Feature last = fp.getFeature( fp.size() - 1 );
        String name = last.getDomain().getShortName() + "_" + last.getShortName();

        switch( fp.getTypClass( ann ) ) {
            case TYPE_CLASS_STRING :
                addScalar( name, tgt, DataFieldType.STRING, fp.getStringValue( ann ) );
                break;
            case TYPE_CLASS_BYTE :
                addScalar( name, tgt, DataFieldType.BYTE, fp.getByteValue( ann ) );
                break;
            case TYPE_CLASS_INT :
                addScalar( name, tgt, DataFieldType.INTEGER, fp.getIntValue( ann ) );
                break;
            case TYPE_CLASS_SHORT :
                addScalar( name, tgt, DataFieldType.INTEGER, fp.getShortValue( ann ) );
                break;
            case TYPE_CLASS_LONG :
                addScalar( name, tgt, DataFieldType.LONG, fp.getLongValue( ann ) );
                break;
            case TYPE_CLASS_FLOAT :
                addScalar( name, tgt, DataFieldType.FLOAT, fp.getFloatValue( ann ) );
                break;
            case TYPE_CLASS_DOUBLE :
                addScalar( name, tgt, DataFieldType.DOUBLE, fp.getDoubleValue( ann ) );
                break;
            case TYPE_CLASS_BOOLEAN :
                addScalar( name, tgt, DataFieldType.BOOLEAN, fp.getBooleanValue( ann ) );
                break;
            
            case TYPE_CLASS_STRINGARRAY : 
                addArray( name, tgt, DataFieldType.STRING, fp.getValueAsString( ann ) );
                break;
            case TYPE_CLASS_BYTEARRAY :
                addArray( name, tgt, DataFieldType.BYTE, fp.getValueAsString( ann ) );
                break;
            case TYPE_CLASS_INTARRAY :
                addArray( name, tgt, DataFieldType.INTEGER, fp.getValueAsString( ann ) );
                break;
            case TYPE_CLASS_SHORTARRAY :
                addArray( name, tgt, DataFieldType.INTEGER, fp.getValueAsString( ann ) );
                break;
            case TYPE_CLASS_LONGARRAY :
                addArray( name, tgt, DataFieldType.LONG, fp.getValueAsString( ann ) );
                break;
            case TYPE_CLASS_FLOATARRAY :
                addArray( name, tgt, DataFieldType.FLOAT, fp.getValueAsString( ann ) );
                break;
            case TYPE_CLASS_DOUBLEARRAY :
                addArray( name, tgt, DataFieldType.DOUBLE, fp.getValueAsString( ann ) );
                break;
            case TYPE_CLASS_BOOLEANARRAY :
                addArray( name, tgt, DataFieldType.BOOLEAN, fp.getValueAsString( ann ) );
                break;
                
            case TYPE_CLASS_FS :
                break;
            case TYPE_CLASS_FSARRAY :
                break;
                
            case TYPE_CLASS_INVALID :
                throw new CASException();
                
            default:
                throw new AssertionError( fp.getTypClass( ann ).name() );
        }
        
    }

    private void addScalar( String name, Datum tgt, DataFieldType ft, Serializable v ) {
        if( v == null ) return;
        tgt.set(new DataField( name, ft ), v );
    }

    private void addArray( String name, Datum tgt, DataFieldType ft, String array ) {
        if( array == null ) return;
        int i = 0;
        for( String v : array.split( "," ) ) {
            tgt.set(new DataField( name + Integer.toString( i++ ), ft ), ft.decode( v ) );
        }
    }

    protected Type getEffectiveType( AnnotationFS ann ) {
        return ann.getType();
    }

    protected List<FeaturePath> getFeaturePaths( Type type, AnnotationFS ann ) throws CASException {
        if( cache.containsKey( type ) ) return cache.get( type );
        
        List<FeaturePath> fps = new ArrayList<>();
        for( Feature f : ann.getType().getFeatures() ) {
            if( isOmited( f ) ) continue;
            FeaturePath fp = ann.getView().createFeaturePath();
            fp.typeInit( type );
            fps.addAll( makePaths( ann, fp, f ) );
        }
        
        cache.put( type, fps );
        return fps;
    }

    private List<FeaturePath> makePaths( AnnotationFS ann, FeaturePath fp, Feature f ) {
        List<FeaturePath> fps = new ArrayList<>();
        if( f.getRange().isPrimitive() ) {
            fp.addFeature( f );
            fps.add( fp );
        }
        else if( dereference ) {
            for( Feature child : f.getRange().getFeatures() ) {
                FeaturePath copy = copy( ann.getCAS(), fp );
                copy.addFeature( f );
                fps.addAll( makePaths( ann, copy, child ) );
            }
        }
        return fps;
    }

    private FeaturePath copy( CAS cas, FeaturePath fp ) {
        FeaturePath copy = cas.createFeaturePath();
        for( int i = 0; i < fp.size(); i++ ) {
            copy.addFeature( fp.getFeature( i ) );
        }
        return copy;
    }
    
    private boolean isOmited( Feature f ) {
        if( f.getName().equals( CAS.FEATURE_BASE_NAME_SOFA ) ) {
            return !includeSofa;
        } else if( isBuiltIn( f ) ) {
            return omitBuiltIns;
        } else {
            return false;
        }
    }

    private boolean isBuiltIn( Feature f ) {
        return f.getDomain().getName().equals( CAS.TYPE_NAME_ANNOTATION )
            || f.getDomain().getName().equals( CAS.TYPE_NAME_ANNOTATION_BASE )
            || f.getDomain().getName().equals( CAS.TYPE_NAME_DOCUMENT_ANNOTATION )
            || f.getDomain().getName().equals( CAS.TYPE_NAME_SOFA );
    }

}
