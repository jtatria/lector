/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus;

import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;

import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.Datum;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
class FieldFactory {
    
    private BiMap<DataField,Field> cache =
        Maps.synchronizedBiMap( HashBiMap.<DataField,Field>create() );
    
    public Set<Field> getFields() {
        return cache.inverse().keySet();
    }
    
    public IndexableField makeField( DataField f, Datum d ) {
        
        if( !cache.containsKey( f ) ) {
            cache.put( f, buildField( f ) );
        }
        Field field = cache.get( f );
                
        switch( f.type() ) {
            case STRING: case CHAR: case BOOLEAN: {
                String v = (String) f.get( d );
                field.setStringValue( v );
                break;
            }
            case BYTE: case INTEGER: {
                Integer v = (Integer) f.get( d );
                field.setIntValue( v );
                break;
            }
            case LONG: {
                Long v = (Long) f.get( d );
                field.setLongValue( v );
                break;
            }
            case FLOAT: {
                Float v = (Float) f.get( d );
                field.setFloatValue( v );
                break;
            }
            case DOUBLE: {
                Double v = (Double) f.get( d );
                field.setDoubleValue( v );
                break;
            }
            default: throw new AssertionError( f.type().name() );
        }
        return field;
    }

    private Field buildField( DataField f ) {
        Field field = null;
        switch( f.type() ) {
            case STRING: case CHAR: case BOOLEAN:
                field = new StringField( f.name(), "", Store.YES );
                break;
            case BYTE: case INTEGER:
                field = new IntField( f.name(), 0, Store.YES );
                break;
            case LONG:
                field = new LongField( f.name(), 0l, Store.YES );
                break;
            case FLOAT:
                field = new FloatField( f.name(), 0f, Store.YES );
                break;
            case DOUBLE:
                field = new DoubleField( f.name(), 0d, Store.YES );
                break;
            default:
                throw new AssertionError( f.type().name() );
        }
        return field;
    }
}
