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
package edu.columbia.incite.uima.io;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.uima.cas.Marker;
import org.apache.uima.cas.impl.BinaryCasSerDes6.ReuseInfo;
import org.apache.uima.cas.impl.XmiSerializationSharedData;
import org.apache.uima.jcas.JCas;

import edu.columbia.incite.uima.types.Document;

/**
 * Simple container for serialization data.
 * Data should be saved by a Collection Reader, and should be used (and cleared!) by
 * implementations of AbstractFileWriter.
 * 
 * WARNING: Not clearing saved data will eventually cause OutOfMemory errors.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class SerializationData {

    private static SerializationData instance = null;

    private SerializationData() {
    }

    public static SerializationData getInstance() {
        if( instance == null ) {
            instance = new SerializationData();
        }
        return instance;
    }

    private Map<String,XmiSerializationSharedData> serData = new ConcurrentHashMap<>();
    private Map<String,Marker> casMarkers = new ConcurrentHashMap<>();
    private Map<String,ReuseInfo> reuseInfo = new ConcurrentHashMap<>();

    /**
     * Save CAS Marker for delta serialization.
     *
     * @param jcas      A JCas.
     * @param casMarker A valid CAS marker.
     */
    public void saveMarker( JCas jcas, Marker casMarker ) {
        String key = getCasId( jcas );
        casMarkers.put( key, casMarker );
    }

    /**
     * Save XMI serialization data for xmi:id consistency across serialization.
     *
     * @param jcas       A JCas
     * @param serialData Serialization data used for de-serialization of the given JCas.
     */
    public void saveXmiSerializationData( JCas jcas, XmiSerializationSharedData serialData ) {
        String key = getCasId( jcas );
        serData.put( key, serialData );
    }

    /**
     * Save binary serialization ReuseInfo to enable delta serialization in format 6/6+.
     *
     * @param jcas A JCas.
     * @param info ReuseInfo created when deserializing the given JCas.
     */
    public void saveReuseInfo( JCas jcas, ReuseInfo info ) {
        String key = getCasId( jcas );
        reuseInfo.put( key, info );
    }

    /**
     * Get a saved CAS marker for the given CAS, if any.
     *
     * @param jcas A JCas.
     *
     * @return A valid CAS Marker, if one was stored.
     */
    public Marker getMarker( JCas jcas ) {
        return casMarkers.get( getCasId( jcas ) );
    }

    /**
     * Get saved XMI serialization data for the given CAS, if any.
     *
     * @param jcas A JCas.
     *
     * @return XMI Serialization data, if any was stored.
     */
    public XmiSerializationSharedData getXmiSerializationData( JCas jcas ) {
        return serData.get( getCasId( jcas ) );
    }

    /**
     * Get form 6 reuse info for the given CAS, if any.
     *
     * @param jcas A JCas.
     *
     * @return Form 6/6+ ReuseInfo, if any was stored.
     */
    public ReuseInfo getReuseInfo( JCas jcas ) {
        return reuseInfo.get( getCasId( jcas ) );
    }

    /**
     * Clear all data for the given JCas.
     * This method must be called explicitly by users of serialization data, or saved data will
     * cause an OutOfMemory error.
     *
     * @param jcas A JCas to delete all serialization data for.
     */
    public void deleteData( JCas jcas ) {
        serData.remove( getCasId( jcas ) );
        casMarkers.remove( getCasId( jcas ) );
    }

    private String getCasId( JCas jcas ) {
        Document dmd = jcas.getAnnotationIndex( Document.class ).iterator().next();
        if( dmd != null ) {
            return dmd.getId();
        } else {
            return null;
        }
    }

}
