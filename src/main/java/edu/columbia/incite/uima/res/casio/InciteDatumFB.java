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

import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.data.DataFieldType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.apache.uima.cas.Type;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
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
