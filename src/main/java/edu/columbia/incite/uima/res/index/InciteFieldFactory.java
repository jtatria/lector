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
 * @author José Tomás Atria <jtatria@gmail.com>
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
