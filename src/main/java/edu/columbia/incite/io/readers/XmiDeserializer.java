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
package edu.columbia.incite.io.readers;

//import edu.columbia.incite.uima.api.SerializationData;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiSerializationSharedData;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.xml.sax.SAXException;

/**
 * Collection reader that de-serializes CASes from a XMI files.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class XmiDeserializer extends AbstractFileReader {
    /**
     * Ignore unknown types instead of causing an exception when found.
     */
    public final static String PARAM_LENIENT = "lenient";
    @ConfigurationParameter( name = PARAM_LENIENT, mandatory = false, defaultValue = "true" )
    private Boolean lenient;
    
    @Override
    protected void addDataFromFile( JCas jcas, FileInputStream fis ) throws CollectionException {
        XmiSerializationSharedData data = serData.getXmiSerializationData( jcas ) == null ?
            new XmiSerializationSharedData() :
            serData.getXmiSerializationData( jcas );
        
        BufferedInputStream bis = new BufferedInputStream( fis );
        try {
            XmiCasDeserializer.deserialize( bis, jcas.getCas(), lenient, data, data.getMaxXmiId() );
        } catch( SAXException | IOException ex ) {
            throw new CollectionException( ex );
        }
        
        serData.saveXmiSerializationData( jcas, data );
    }
}
