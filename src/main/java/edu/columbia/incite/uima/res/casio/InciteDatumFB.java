/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.casio;

import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.data.DataFieldType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.apache.uima.cas.Type;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class InciteDatumFB extends InciteFB_Base<Datum> {

    private Map<String,DataField> dfCache = new ConcurrentHashMap<>();
    
    @Override
    protected void addData( String name, Object v, Types dfType, Datum tgt ) {
        DataField df;
        if( dfCache.containsKey( name ) ) {
            df = dfCache.get( name );
        } else {
            df = new DataField( name, getDataFieldType( dfType ) );
            dfCache.put( name, df );
        }
        tgt.put( df, v );
    }

    @Override
    protected Supplier<Datum> supplier() {
        return () -> new Datum();
    }

    private DataFieldType getDataFieldType( Types type ) {
        switch( type ) {
            case STRING:  return DataFieldType.STRING;
            case INTEGER: return DataFieldType.LONG;
            case REAL:    return DataFieldType.DOUBLE;
            case BOOLEAN: return DataFieldType.BOOLEAN;
            default: throw new AssertionError( type.name() );
        }
    }
}
