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
package edu.columbia.incite.resources.casio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import edu.columbia.incite.util.Datum;
import edu.columbia.incite.util.Datum.DataField;
import edu.columbia.incite.util.Datum.DataFieldType;

/**
 * An InciteBroker that creates and populates instances of @link{Datum} multi-type maps.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class InciteDatumBroker extends InciteBroker<Datum> {

    private final Map<String,DataField> dfCache = new ConcurrentHashMap<>();
    
    @Override
    protected void addData( String name, Object v, Types dfType, Datum tgt ) {
        DataField df = dfCache.computeIfAbsent(
            name, n -> new DataField( n, getDataFieldType( dfType ) ) 
        );
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
