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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class InciteLuceneFB extends InciteFB_Base<Document> {
    
    private static final byte[] DEAD_BEEF = new byte[] { 0xd, 0xe, 0xa, 0xd, 0xb, 0xe, 0xe, 0xf };
    
    public static final String  DFLT_STRING   = new String( DEAD_BEEF, StandardCharsets.UTF_8 );
    public static final Long    DFLT_INTEGER  = ByteBuffer.wrap( DEAD_BEEF ).getLong();
    public static final Double  DFLT_REAL     = ByteBuffer.wrap( DEAD_BEEF ).getDouble();
    public static final Integer DFLT_BOOLEAN  = -1;
        
    private final ThreadLocal<Map<String,Field>> fCache = ThreadLocal.withInitial(
        () -> new HashMap<>() 
    );
    
    protected Field getField( String key, final Types dfType ) {
        return fCache.get().computeIfAbsent( key, s -> createField( s, dfType ) );
    }

    @Override
    protected void addData( String key, Object v, Types dfType, Document tgt ) {
        Field lf = getField( key, dfType );
        if( lf == null ) throw new IllegalStateException();
        update( lf, v, dfType );
        tgt.add( lf );
    }

    @Override
    protected Supplier<Document> supplier() {
        return () -> {   
            return new Document();
        };
    }
    

    
    public static Field createField( String name, Types dfType ) {
        switch( dfType ) {
            case STRING:  return new StringField( name, DFLT_STRING,   Field.Store.YES );
            case INTEGER: return new LongField(   name, DFLT_INTEGER,  Field.Store.YES );
            case REAL:    return new DoubleField( name, DFLT_REAL,     Field.Store.YES );
            case BOOLEAN: return new IntField(    name, DFLT_BOOLEAN,  Field.Store.YES );
            default: throw new AssertionError( dfType.name() );
        }
    }

    public static void update( Field lf, Object v, Types dfType ) {
        switch( dfType ) {
            case STRING:  lf.setStringValue(   (String) v );                 break;
            case INTEGER: lf.setLongValue( (   (Number) v ).longValue() );   break;
            case REAL:    lf.setDoubleValue( ( (Number) v ).doubleValue() ); break;
            case BOOLEAN: lf.setIntValue(   ( (Boolean) v ) ? 1 : 0 );       break;
            default: throw new AssertionError( dfType.name() );
        }
    }
}
