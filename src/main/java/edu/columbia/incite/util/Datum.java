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
package edu.columbia.incite.util;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * An non-optimized, thread-safe implementation of a multi-type map.
 * This implementation is designed to be used a basis for an implementation-agnostic record in a
 * data set.
 * Instances of this class can control how they will be compared for equality with other instances
 * by indicating which fields should be considered in equality comparisons or not. See
 * {@link DataField}.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Datum implements Serializable {
    private static final long serialVersionUID = -6934315215675815909L;

    private final Map<DataField,Object> data = new ConcurrentHashMap<>();

    private Integer cachedHash;

    public Object put( DataField f, Object v ) {
        Object old = data.put( f, v );
        if( !v.equals( old ) ) {
            cachedHash = null;
        }
        return old;
    }
    
    public <T> T get( DataField<T> f ) {
        return (T) data.get( f );
    }

    public Set<DataField> fields() {
        return Collections.unmodifiableSet( data.keySet() );
    }

    public boolean hasField( DataField f ) {
        return data.keySet().contains( f );
    }

    public Set<String> fieldNames() {
        return data.keySet().stream()
            .map( ( f ) -> f.name() )
            .collect( Collectors.toSet() );
    }

    public Set<Map.Entry<DataField,Object>> data() {
        return Collections.unmodifiableSet( data.entrySet() );
//        return ImmutableSet.<Map.Entry<DataField,Object>>copyOf( data.entrySet() );
    }

    public void clear() {
        data.clear();
        cachedHash = null;
    }

    @Override
    public boolean equals( Object o ) {
        if( o == null ) return false;
        if( o == this ) return true;
        if( o.getClass() != this.getClass() ) return false;
        return data.equals( ( (Datum) o ).data );
    }

    @Override
    public int hashCode() {
        if( cachedHash == null ) cachedHash = data.hashCode();
        return cachedHash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append( "{" );
        for( Entry<DataField,Object> e : this.data() ) {
            String k = e.getKey().toString();
            String v = e.getValue().toString();
            sb.append( String.format( "'%s' = '%s',\n", k, v ) );
        }
        sb.append( "}" );
        return sb.toString();
    }
    
    public static class DataField<T> implements Comparable<DataField>, Serializable {
        private static final long serialVersionUID = -8657350678121624695L;

        public final String name;
        public final DataFieldType type;
    //    private final Key key;

        public DataField( String name, Class<T> cls ) {
            this( name, DataFieldType.forClass( cls ) );
        }

        public DataField( String name, DataFieldType type ) {
            this.name = name;
            this.type = type;
    //        this.key = new Key( new Key( name ), new Key( type.name()) );
        }

        public String name() {
            return this.name;
        }

        public DataFieldType type() {
            return this.type;
        }

        public T get( Datum d ) {
            return (T) type.cast( d.get( this ) );
        }

        @Override
        public boolean equals( Object obj ) {
            if( obj == null ) return false;
            if( obj == this ) return true;
            if( !DataField.class.isAssignableFrom( obj.getClass() ) ) return false;
            final DataField<?> other = (DataField<?>) obj;
    //        return this.type == other.type && this.key.equals( other.key );
            return this.type == other.type && this.name.equals( other.name );
        }

        @Override
        public int hashCode() {
    //        return this.key.hashCode();
            return Objects.hash( name, type );
        }

        @Override
        public int compareTo( DataField o ) {
            if( this == o ) return 0;
            int c = this.name.compareTo( o.name );
            return c == 0 ? this.type.compareTo( o.type ) : c;
        }

        @Override
        public String toString() {
            return this.name + ":" + this.type().toString();
        }

    }
    
    /**
    * Enumeration of primitive wrapper types that can be referenced from a field definition and used
    * to combine and parse objects believed to be instances of the corresponding wrapper.
    *
    * @author José Tomás Atria <jtatria@gmail.com>
    */
   public static enum DataFieldType {
       // TODO: precision should be abstracted away from this enum: remove double/float and replace
       // by real, etc.
       STRING(  String.class,    ( String s ) -> s ),
       BYTE(    Byte.class,      ( String s ) -> Byte.parseByte( s ) ),
       INTEGER( Integer.class,   ( String s ) -> Integer.decode( s ) ),
       LONG(    Long.class,      ( String s ) -> Long.decode( s ) ),
       FLOAT(   Float.class,     ( String s ) -> Float.valueOf( s ) ),
       DOUBLE(  Double.class,    ( String s ) -> Double.parseDouble( s ) ),
       BOOLEAN( Boolean.class,   ( String s ) -> Boolean.parseBoolean( s ) ),
       CHAR(    Character.class, ( String s ) -> s.charAt( 0 ) ),
       ;

       private static final Map<String,DataFieldType> FOR_CLASS = new HashMap<>();
       static {
           for ( DataFieldType t : DataFieldType.values() ) {
               FOR_CLASS.put( t.clz.getName(), t );
           }
       }

       private final Class clz;
       private final Decoder decoder;

       private <T> DataFieldType( Class<T> clz, Decoder<T> d ) {
           this.clz = clz;
           this.decoder = d;
       }

       @Override
       public String toString() {
           return this.name();
       }

       public <T> T cast( Object value ) throws ClassCastException {
           return (T) clz.cast( value );
       }

       public <T> T decode( String s ) throws ClassCastException {
           return (T) decoder.decode( s );
       }

       public static DataFieldType forClass( Class<?> claz ) {
           return FOR_CLASS.get( claz.getName() );
       }

       @FunctionalInterface
       private interface Decoder<T> {
           abstract T decode( String s ) throws ClassCastException;
       }

   }


}
