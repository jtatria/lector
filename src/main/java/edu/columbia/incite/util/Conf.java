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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class Conf implements Serializable {
    private static final long serialVersionUID = 2596815141983031282L;

    public static final String SEP = ".";

    protected transient final Map<Class<?>,Function<String,?>> factories = new HashMap<>();

    private final Map<String,String> values = new HashMap<>();

    private final Properties props = new Properties();
    
    protected Conf( Map<String,String> values ) {
        load( values );
    }

    protected Conf( String ns, Properties props ) {
        load( ns, props );
    }
    
    public String get( String name ) {
        if( __debug__ ) System.out.println( "conf get " + name );
        return values.get( name );
    }
    
    public String set( String param, String value ) {
        if( __debug__ ) System.out.println( "conf set " + param + " " + value );
        return values.put( param, value );
    }
    
    public final void load( String ns, Properties props ) {
        if( __debug__ ) System.out.print( "loading " + ns + " from:\n" + props.toString().replaceAll( ",", "," ) );
        for( String s : props.stringPropertyNames() ) {
            int index = s.lastIndexOf( SEP );
            String para = index > 0 ? s.substring( index + 1, s.length() ) : s;
            String pref = index > 0 ? s.substring( 0, index ) : "";
            if( pref.startsWith( ns ) && !para.isEmpty() ) {
                values.put( para, props.getProperty( s ) );
            }
        }
        this.props.clear();
        this.props.putAll( props );
    }
    
    public final void load( Map<String,String> params ) {
        if( __debug__ ) params.entrySet().stream().forEach( ( e ) -> System.out.println( e.getKey() + "\t" + e.getValue() ) );
        values.putAll( params );
    }

    protected Integer getInteger( String name, Integer def ) {
        String value = get( name );
        return value != null ? Integer.decode( value ) : def;
    }

    protected Long getLong( String name, Long def ) {
        String value = get( name );
        return value != null ? Long.decode( value ) : def;
    }

    protected Float getFloat( String name, Float def ) {
        String value = get( name );
        return value != null ? Float.parseFloat( value ) : def;
    }

    protected Double getDouble( String name, Double def ) {
        String value = get( name );
        return value != null ? Double.parseDouble( value ) : def;
    }

    protected Boolean getBoolean( String name, Boolean def ) {
        String value = get( name );
        return value != null ? Boolean.parseBoolean( value ) : def;
    }

    protected String getString( String name, String def ) {
        String value = get( name );
        return value != null ? value : def;
    }

    protected String[] getStringArray( String name, String[] def ) {
        String value = get( name );
        if( value != null ) {
            String[] split = value.split( "," );
            if( split.length > 0 ) return split;
        }
        return def;
    }

    protected int[] getIntegerArray( String name, int[] def ) {
        String value = get( name );
        if( value != null ) {
            String[] split = value.split( "," );
            if( split.length > 0 ) {
                int[] ret = new int[split.length];
                for( int i = 0; i < ret.length; i++ ) {
                    ret[i] = Integer.decode( split[i] );
                }
                return ret;
            }
        }
        return def;
    }

    protected <T extends Enum<T>> T getEnum( String name, Class<T> enumClass, T def ) {
        String value = get( name );
        return value != null ? Enum.valueOf( enumClass, value ) : def;
    }

    protected Path getPath( String name, Path dir, Path def ) {
        if( __debug__ ) {
            System.out.printf( 
                "%s\t%s\n%s\t%s\n%s\t%s\n",
                "name", name,
                "dir", dir != null ? dir.toString() : "",
                "default", def != null ? def.toString() : ""
            );
        }
        String value = get( name );
        Path tgt = value != null ? Paths.get(  value ) : def;
        return dir != null ? dir.resolve( tgt ) : tgt;
    }

    protected Path getPath( String name, Path def ) {
        String value = get( name );
        return value != null ? Paths.get( value ) : def;
    }

    protected Class getClass( String name, String pkg, Class def ) {
        String value = get( name );
        if( value == null ) return def;
        try {
            return Class.forName( pkg + "." + value );
        } catch( ClassNotFoundException ex ) {
            throw new RuntimeException( ex ); // TODO rte
        }
    }
    
    protected Class getClass( String name, Class def ) {
        String value = get( name );
        if( value == null ) return def;
        try {
            return Class.forName( value );
        }
        catch( ClassNotFoundException ex ) {
            throw new RuntimeException( ex ); // TODO rte
        }
    }

    protected <V> V getOther( String name, Class<V> clz, V def ) {
        String value = get( name );
        V ret = def;
        if( value != null ) {
            Function factory = factories.get( clz );
            if( factory == null ) throw new IllegalStateException(
                String.format( "No factory defined for class %s", clz.getSimpleName() )
            );
            else ret = (V) factory.apply( value );
        }
        return ret;
    }

    protected <T> List<T> getList( String name, Class<T> clz, List<T> def ) {
        String[] value = getStringArray( name, null );
        if( value != null ) {
            List<T> ret = new ArrayList<>();
            Function factory = factories.get( clz );
            if( factory == null ) {
                throw new IllegalStateException(
                    String.format( "No factory defined for class %s", clz.getSimpleName() )
                );
            } else {
                for( String s : value ) {
                    ret.add( (T) factory.apply( s ) );
                }
            }
            return ret;
        }
        return def;
    }

    protected <T> Map<String,T> getMap( String name, Class<T> clz, Map<String,T> def ) {
        String[] value = getStringArray( name, null );
        if( value != null ) {
            if( value.length % 2 != 0 ) throw new IllegalArgumentException( "length of value for map is not even" );
            Map<String,T> map = new HashMap<>();
            Function factory = factories.get( clz );
            if( factory == null ) {
                throw new IllegalStateException(
                    String.format( "No factory defined for class %s", clz.getSimpleName() )
                );
            }
            for( int i = 0; i < value.length; i += 2 ) {
                String k = value[i];
                T v = (T) factory.apply( value[i+1] );
                map.put( k, v );
            }
            return map;
        }
        return def;
    }

    public Properties getProps() {
        return this.props;
    }

    public class InvalidParameterException extends RuntimeException {
        private static final long serialVersionUID = 5785110877313596491L;

        public InvalidParameterException( String name, String value, String msg ) {
            super(
                String.format( "Invalid value %s for %s: %s", value, name, msg )
            );
        }
    }

    public boolean validate() {
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<String> keys = new ArrayList<>( values.keySet() );
        Collections.sort( keys );
        for( String k : keys ) {
            sb.append( String.format( "%s=%s\n", k, values.get( k ) ) );
        }
        return sb.toString();
    }
    
    protected static boolean __debug__ = false;
    
    public static void debug__() {
        __debug__ = !__debug__;
    }
}
