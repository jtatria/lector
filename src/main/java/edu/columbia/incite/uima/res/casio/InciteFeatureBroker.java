/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.casio;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceConfigurationException;

import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.uima.api.types.Document;
import edu.columbia.incite.uima.api.types.Span;
import edu.columbia.incite.uima.api.types.Tuple;
import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.DataFieldType;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class InciteFeatureBroker extends Resource_ImplBase implements FeatureBroker<Datum> {

    public static final String DOC_PREFIX = "doc";
    public static final String PROC_PREFIX = "proc";
    public static final String SEP = String.valueOf( TypeSystem.FEATURE_SEPARATOR );
    
    public static final String   ID_FIELD_NAME = "id";
    public static final String  COL_FIELD_NAME = DOC_PREFIX  + SEP + "Collection";
    public static final String  URI_FIELD_NAME = DOC_PREFIX  + SEP + "URI";
    public static final String  XPT_FIELD_NAME = DOC_PREFIX  + SEP + "XPath";
    public static final String  IND_FIELD_NAME = DOC_PREFIX  + SEP + "Index";
    public static final String LAST_FIELD_NAME = PROC_PREFIX + SEP + "IsLast";
    public static final String SKIP_FIELD_NAME = PROC_PREFIX + SEP + "Skip";

    public static final String SUB_SUFFIX = "_subject";
    public static final String OBJ_SUFFIX = "_object";

    public static final DataField   ID_FIELD = new DataField(   ID_FIELD_NAME, DataFieldType.STRING );
    public static final DataField  COL_FIELD = new DataField(  COL_FIELD_NAME, DataFieldType.STRING );
    public static final DataField  URI_FIELD = new DataField(  URI_FIELD_NAME, DataFieldType.STRING );
    public static final DataField  XPT_FIELD = new DataField(  XPT_FIELD_NAME, DataFieldType.STRING );
    public static final DataField  IND_FIELD = new DataField(  IND_FIELD_NAME, DataFieldType.INTEGER );
    public static final DataField LAST_FIELD = new DataField( LAST_FIELD_NAME, DataFieldType.BOOLEAN );
    public static final DataField SKIP_FIELD = new DataField( SKIP_FIELD_NAME, DataFieldType.BOOLEAN);

    public static final String PARAM_ADD_PROC = "addProc";
    @ConfigurationParameter( name = PARAM_ADD_PROC, mandatory = false )
    private boolean addProc = false;

    @Override
    public Datum values( AnnotationFS ann, boolean merge ) throws CASException {
        if( !( ann instanceof Span ) ) return null;
        Datum d = new Datum();
        values( ann, d, merge );
        return d;
    }

    @Override
    public void values( AnnotationFS ann, Datum tgt, boolean merge ) throws CASException {
        if( !( ann instanceof Span ) ) return;
        Span span = (Span) ann;

        if( span.getId() != null )
            tgt.put( ID_FIELD, span.getId() );
        if( span.getAttributes() != null ) {
            for( int i = 0; i < span.getAttributes().size(); i++ ) {
                DataField f = new DataField( span.getAttributes( i ).getK(), DataFieldType.STRING );
                tgt.put( f, span.getAttributes( i ).getV() );
            }
        }

        if( span.getTuples() != null ) {
            for( int i = 0; i < span.getTuples().size(); i++ ) {
                Tuple tuple = span.getTuples( i );

                boolean isSubject;
                if( tuple.getSubject().equals( span ) ) isSubject = true;
                else if( tuple.getObject().equals( span ) ) isSubject = false;
                else continue;

                String pred = tuple.getPredicate();
                Span targ = isSubject ? tuple.getObject() : tuple.getSubject();
                String value = targ.getType().getShortName()
                    + TypeSystem.FEATURE_SEPARATOR + targ.getId();
                String predKey = isSubject ? pred + SUB_SUFFIX : pred + OBJ_SUFFIX;
                tgt.put( new DataField( predKey, DataFieldType.STRING ), value );
            }
        }

        if( ann instanceof Document ) {
            Document doc = (Document) ann;
            tgt.put( COL_FIELD, doc.getCollection() );
            tgt.put( URI_FIELD, doc.getUri() );
            tgt.put( XPT_FIELD, doc.getXpath() );
            tgt.put( IND_FIELD, doc.getIndex() );
            if( addProc ) {
                tgt.put( LAST_FIELD, doc.getProc_isLast() );
                tgt.put( SKIP_FIELD, doc.getProc_skip() );
            }
        }
    }

    @Override
    public void configure( CAS conf ) throws ResourceConfigurationException {
    }

}
