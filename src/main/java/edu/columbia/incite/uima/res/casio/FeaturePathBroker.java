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
package edu.columbia.incite.uima.res.casio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.DataFieldType;
import edu.columbia.incite.util.data.Datum;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class FeaturePathBroker extends Resource_ImplBase implements FeatureBroker<Datum> {

    public static final String SEP = String.valueOf( TypeSystem.FEATURE_SEPARATOR );
    
    public static final String PARAM_FEAT_PATHS = "featurePaths";
    @ConfigurationParameter( name = PARAM_FEAT_PATHS, mandatory = false,
        description = "Feature path strings for specific data extraction requesrs."
    )
    private String[] featurePaths;
    
    public static final String PARAM_USE_FEAT_DOMAIN = "useDomain";
    @ConfigurationParameter( name = PARAM_USE_FEAT_DOMAIN, mandatory = false,
        description = "Use feature domain instead of annotation type for field names" )
    private Boolean useDomain;
    
    public static final String PARAM_USE_PARENT_TYPES = "useParents";
    @ConfigurationParameter( name = PARAM_USE_PARENT_TYPES, mandatory = false,
        description = "Consider parent types when retrieveing data requests."
    )
    private boolean useParents;
    
    public static final String PARAM_OMIT_BUILTINS = "includeBuiltIns";
    @ConfigurationParameter( name = PARAM_OMIT_BUILTINS, mandatory = false, defaultValue = "false",
        description = "If no defined requests given, include built-in UIMA features in data extraction."
    )
    private boolean includeBuiltIns;

    public static final String PARAM_DEREFERENCE = "dereference";
    @ConfigurationParameter( name = PARAM_DEREFERENCE, mandatory = false, defaultValue = "true",
        description = "If no defined requests given, dereference non-primitive feature structures."
    )
    private boolean dereference;

    private final Map<Type,List<FeaturePath>> fpCache = new ConcurrentHashMap<>();

    private TypeSystem ts;
    
    public FeaturePathBroker() {
        // stub for uima-fit instantiation
    }
    
    public FeaturePathBroker( String[] fps, boolean useParents ) {
        this.featurePaths  = fps;
        this.useParents = useParents;
    }
    
    @Override
    public boolean initialize( ResourceSpecifier spec, Map<String,Object> params )
    throws ResourceInitializationException {
        boolean ret = super.initialize( spec, params );
        
        this.useParents = ( featurePaths == null || featurePaths.length == 0 ) ? false : useParents;
        
        return ret;
    }
    
    @Override
    public Datum values( AnnotationFS ann ) throws CASException {
        Datum d = new Datum();
        values( ann, d );
        return d;
    }

    @Override
    public void values( AnnotationFS ann, Datum tgt ) throws CASException {
        Type key = resolveTypeKey( ann );
        List<FeaturePath> fps = collectFeaturePaths( key, ann );
        for( FeaturePath fp : fps ) getDataFromPaths( fp, ann, tgt );
    }
    
    public static void main( String[] args ) {
        String t1 = "Type:parentFeature/childFeature";
        String t2 = ":parentFeature/childFeature";
        String t3 = "Type:parentFeature/childFeature:error";
        String[] t = new String[]{ t1, t2, t3 };
        for( int i = 0; i < t.length; i++ ) {
            String[] split = t[i].split( "" + TypeSystem.FEATURE_SEPARATOR );
            int stop = 0;
        }
    }

    @Override
    public void configure( CAS cas ) throws ResourceConfigurationException {
        // Extraction mode, no conf needed.
        if( featurePaths == null || featurePaths.length == 0 ) return;
        
        // No typesystem change, nothing to do.
        if( ts != null && ts.equals( cas.getTypeSystem() ) ) return;

        for( String path : featurePaths ) {
            Type type = null;
            Matcher m = Pattern.compile( "^(.*)" + TypeSystem.FEATURE_SEPARATOR ).matcher( path );
            if( m.find() ) {
                type = ts.getType( m.group( 1 ) );
                path = m.replaceAll( "" );
            }
            FeaturePath fp = cas.createFeaturePath();
            try {
                fp.initialize( path );
            } catch ( CASException ex ) {
                throw new ResourceConfigurationException( ex );
            }
            if( !fpCache.containsKey( type ) ) fpCache.put( type, new ArrayList<>() );
            fpCache.get( type ).add( fp );
        }
    }

    private Type resolveTypeKey( AnnotationFS ann ) {
        if( featurePaths == null || featurePaths.length == 0 || !useParents ) return ann.getType();
        Type type = ann.getType();
        while( useParents && !fpCache.containsKey( type ) && type != null ) type = ts.getParent( type );
        return type != null ? type : ann.getType();
    }

    private List<FeaturePath> collectFeaturePaths( Type type, AnnotationFS ann ) throws CASException {
        if( featurePaths == null || featurePaths.length == 0 ) {
            if( !fpCache.containsKey( type ) ) {
                List<FeaturePath> fps = new ArrayList<>();
                for( Feature f : ann.getType().getFeatures() ) {
                    if( isOmited( f ) ) continue;
                    FeaturePath fp = ann.getView().createFeaturePath();
                    fp.typeInit( type );
                    fps.addAll( makePaths( ann, fp, f ) );
                }

                fpCache.put( type, fps );
            }
            return fpCache.get( type );
        } else {
            List<FeaturePath> fps = new ArrayList<>();
            if( fpCache.containsKey( null ) ) fps.addAll( fpCache.get( null ) );
            if( fpCache.containsKey( type ) ) fps.addAll( fpCache.get( type ) );
            return fps;
        }
    }

    private boolean isOmited( Feature f ) {
        if( isBuiltIn( f ) ) {
            return !includeBuiltIns;
        } else {
            return false;
        }
    }

    private List<FeaturePath> makePaths( AnnotationFS ann, FeaturePath fp, Feature f ) {
        List<FeaturePath> fps = new ArrayList<>();
        if( f.getRange().isPrimitive() ) {
            fp.addFeature( f );
            fps.add( fp );
        } else if( dereference ) {
            for( Feature child : f.getRange().getFeatures() ) {
                FeaturePath copy = copyFP( ann.getCAS(), fp );
                copy.addFeature( f );
                fps.addAll( makePaths( ann, copy, child ) );
            }
        }
        return fps;
    }
    
    private FeaturePath copyFP( CAS cas, FeaturePath fp ) {
        FeaturePath copy = cas.createFeaturePath();
        for( int i = 0; i < fp.size(); i++ ) {
            copy.addFeature( fp.getFeature( i ) );
        }
        return copy;
    }

    private boolean isBuiltIn( Feature f ) {
        return f.getDomain().getName().equals( CAS.TYPE_NAME_ANNOTATION )
            || f.getDomain().getName().equals( CAS.TYPE_NAME_ANNOTATION_BASE )
            || f.getDomain().getName().equals( CAS.TYPE_NAME_DOCUMENT_ANNOTATION )
            || f.getDomain().getName().equals( CAS.TYPE_NAME_SOFA );
    }

    
    protected void getDataFromPaths( FeaturePath fp, AnnotationFS ann, Datum tgt ) 
    throws CASException {
        Feature last = fp.getFeature( fp.size() - 1 );
        String name = useDomain ?
            last.getDomain().getShortName() + SEP + last.getShortName() :
            ann.getType().getShortName() + SEP + last.getShortName();

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
                break; // TODO Not suppoerted yet.
            case TYPE_CLASS_FSARRAY :
                break; // TODO Not supported yet.

            case TYPE_CLASS_INVALID : throw new CASException();

            default: throw new AssertionError( fp.getTypClass( ann ).name() );
        }
    }
    
    private void addScalar( String name, Datum tgt, DataFieldType ft, Serializable v ) {
        if( v == null ) return;
        tgt.put( new DataField( name, ft ), v );
    }

    private void addArray( String name, Datum tgt, DataFieldType ft, String array ) {
        if( array == null ) return;
        int i = 0;
        for( String v : array.split( "," ) ) {
            tgt.put( new DataField( name + Integer.toString( i++ ), ft ), ft.decode( v ) );
        }
    }
}
