/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.casio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceConfigurationException;

import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.util.data.Datum;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class FeaturePathBroker extends FeatureExtractor implements FeatureBroker<Datum> {

    public static final String PARAM_FEAT_PATHS = "featPaths";
    @ConfigurationParameter( name = PARAM_FEAT_PATHS, mandatory = false )
    private String[] featPaths;
    
    public static final String PARAM_USE_PARENT_TYPES = "usParents";
    @ConfigurationParameter( name = PARAM_USE_PARENT_TYPES, mandatory = false )
    private boolean useParents;
    
    private Map<Type,List<FeaturePath>> typeMap;
    
    private TypeSystem ts;
    
    public FeaturePathBroker( String[] paths, boolean useParents ) {
        this.featPaths = paths;
        this.useParents = useParents;
    }
    
    @Override
    public void configure( CAS cas ) throws ResourceConfigurationException {
        if( ts != null && ts.equals( cas.getTypeSystem() ) ) return;

        for( String path : featPaths ) {
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
            if( !typeMap.containsKey( type ) ) typeMap.put( type, new ArrayList<>() );
            typeMap.get( type ).add( fp );
        }
    }
    
    @Override
    protected Type getEffectiveType( AnnotationFS ann ) {
        Type type = ann.getType();
        if( typeMap.containsKey( type ) ) return type;
        else if( useParents ) {
            while( !typeMap.containsKey( type ) && type != null ) {
                type = ts.getParent( type );
            }
        }
        return type == null ? ann.getType() : type;
    }

    @Override
    protected List<FeaturePath> getFeaturePaths( Type type, AnnotationFS fs ) {
        List<FeaturePath> fps = new ArrayList<>();
        if( typeMap.containsKey( null ) ) fps.addAll( typeMap.get( null ) );
        if( typeMap.containsKey( type ) ) fps.addAll( typeMap.get( type ) );
        return fps;
    }

}
