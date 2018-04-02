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
package edu.columbia.incite.io.writers;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.util.Level;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.impl.XmiSerializationSharedData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import edu.columbia.incite.io.SerializationData;

import static edu.columbia.incite.util.io.FileUtils.getOutputStream;

/**
 * An analysis component that serializes CASes to XMI files.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class XmiSerializer extends AbstractFileWriter {
    
    public final static String PARAM_USE_DATA = "useData";
    @ConfigurationParameter( name = PARAM_USE_DATA, mandatory = false, defaultValue = "false",
        description = "If true, save serialization data for id consistency across serializations"
    )
    private Boolean useData;

    /**
     * Write CASes as deltas from the last saved marker.
     */
    public final static String PARAM_DELTA_CAS = "delta";
    @ConfigurationParameter(
         name = PARAM_DELTA_CAS, mandatory = false, defaultValue = "false",
        description = "If true, CAS will be serialized as delta since the last CAS marker. "
        + "Requires a shared SerializationData resource."
    )
    private Boolean delta;

    /**
     * Add newlines and tabs to the generated XMI file.
     */
    public final static String PARAM_PRETTY_PRINT = "pretty";
    @ConfigurationParameter(
         name = PARAM_PRETTY_PRINT, mandatory = false, defaultValue = "true",
        description = "Serialize XMI with newlines and indentation."
    )
    private Boolean pretty;

    private SerializationData serData;
    
    XmiSerializationSharedData curData;
    Marker curMarker;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );

        if( delta && !getPattern().matches( "%i" ) ) {
            setPattern( "%i" + File.separator + getPattern() );
        }
        
        if( delta ) useData = true;
        if( useData ) serData = SerializationData.getInstance();
        
        // Set parent's configured extension.
        setExtension( ".xmi" );
        
    }

    @Override
    public void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        String baseName = getFileNameForCas( jcas.getCas() );
        
        int i = 0;
        do {
            baseName = baseName.replaceAll( "%i", Integer.toString( i++ ) );
        } while ( Files.exists( Paths.get( baseName ) ) );
        
        if( useData ) {
            curData = serData.getXmiSerializationData( jcas );
            if( delta ) curMarker = serData.getMarker( jcas );
            serData.deleteData( jcas );
        }
        
        getLogger().log( Level.INFO, "Writing {0} CAS to {1}{2}{3}"
            , new Object[]{ delta ? "delta" : "full", getDir(), File.separator, baseName }
        );
        
        try ( OutputStream os = getOutputStream( getDir(), baseName, getMakeDirs(), getOverwrite() ) ) {
            XmiCasSerializer.serialize( jcas.getCas(), jcas.getTypeSystem(), os, pretty, curData, curMarker );
        } catch( IOException | SAXException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
        
        curData = null;
        curMarker = null;
    }
}
