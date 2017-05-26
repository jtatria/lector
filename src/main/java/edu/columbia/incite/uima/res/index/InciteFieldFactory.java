/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.index;

import edu.columbia.incite.uima.api.index.FieldFactory;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.Datum;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.uima.fit.component.Resource_ImplBase;

/**
 *
 * @author jta
 */
public class InciteFieldFactory extends Resource_ImplBase implements FieldFactory {
    
    private BiMap<DataField, Field> cache = Maps.synchronizedBiMap( 
        HashBiMap.<DataField, Field>create()
    );

    @Override
    public void updateFields( Document docInstance, Datum metadata ) {
        return;
    }
    
    public Set<Field> getFields() {
        return cache.inverse().keySet();
    }
    
    public Collection<IndexableField> getfields( final Datum d ) {
        return d.fields().stream().map( df -> getField( df, d ) ).collect( Collectors.toList() );
    }
    
    public IndexableField getField( DataField f, Datum d ) {
        Field field = cache.computeIfAbsent( f, ( DataField fld ) -> make( fld ) );
        
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

    private Field make( DataField f ) {
        Field field = null;
        switch( f.type() ) {
            case STRING: case CHAR: case BOOLEAN:
                field = new StringField( f.name(), "", Field.Store.YES );
                break;
            case BYTE: case INTEGER:
                field = new IntField( f.name(), 0, Field.Store.YES );
                break;
            case LONG:
                field = new LongField( f.name(), 0l, Field.Store.YES );
                break;
            case FLOAT:
                field = new FloatField( f.name(), 0f, Field.Store.YES );
                break;
            case DOUBLE:
                field = new DoubleField( f.name(), 0d, Field.Store.YES );
                break;
            default:
                throw new AssertionError( f.type().name() );
        }
        return field;
    }


}
