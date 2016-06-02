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

    public static final String ID_FIELD = "id";
    public static final String COL_FIELD = "doc:Collection";
    public static final String URI_FIELD = "doc:URI";
    public static final String XPT_FIELD = "doc:XPath";
    public static final String IND_FIELD = "doc:Index";
    public static final String LAST_FIELD = "proc:IsLast";
    public static final String SKIP_FIELD = "proc:Skip";
    public static final String SUB_SUFFIX = "_subject";
    public static final String OBJ_SUFFIX = "_object";

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
            tgt.put( new DataField( ID_FIELD, DataFieldType.STRING ), span.getId() );
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
            tgt.put( new DataField( COL_FIELD, DataFieldType.STRING ), doc.getCollection() );
            tgt.put( new DataField( URI_FIELD, DataFieldType.STRING ), doc.getUri() );
            tgt.put( new DataField( XPT_FIELD, DataFieldType.STRING ), doc.getXpath() );
            tgt.put( new DataField( IND_FIELD, DataFieldType.INTEGER ), doc.getIndex() );
            if( addProc ) {
                tgt.put( new DataField( LAST_FIELD, DataFieldType.BOOLEAN ), doc.getProc_isLast() );
                tgt.put( new DataField( SKIP_FIELD, DataFieldType.BOOLEAN ), doc.getProc_skip() );
            }
        }
    }

    @Override
    public void configure( CAS conf ) throws ResourceConfigurationException {
    }

}
