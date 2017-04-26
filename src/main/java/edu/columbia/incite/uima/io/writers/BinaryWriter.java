/*
 * Copyright (C) 2015 Jose Tomas Atria <jtatria@gmail.com>
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
package edu.columbia.incite.uima.io.writers;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.BinaryCasSerDes4;
import org.apache.uima.cas.impl.BinaryCasSerDes6;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.io.BinaryFormat;
import edu.columbia.incite.uima.io.util.SerializationData;
import edu.columbia.incite.util.io.FileUtils;

import static org.apache.uima.cas.impl.Serialization.serializeCASMgr;

/**
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public class BinaryWriter extends AbstractFileWriter {
      
    public static final String PARAM_FORMAT = "format";
    @ConfigurationParameter( name = PARAM_FORMAT, mandatory = false, defaultValue = "UIMA_4" )
    private BinaryFormat bf;
//    private String format;
    
    public static final String PARAM_DELTA = "delta";
    @ConfigurationParameter( name = PARAM_DELTA, mandatory = false, defaultValue = "false" )
    private Boolean delta;
    
    public static final String PARAM_TYPE_SYSTEM_FILE = "tsFile";
    @ConfigurationParameter( name = PARAM_TYPE_SYSTEM_FILE, mandatory = false, defaultValue = "TypeSystem.ser" )
    private String tsFile;
    
    private SerializationData serData = SerializationData.getInstance();

    private Boolean needsTsWritten = true;
    
//    private BinaryFormat bf;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        this.ext = ".bin";
//        this.bf = BinaryFormat.forString( format );
    }
    
    @Override
    public void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        OutputStream os = getOutputStreamForCas( jcas.getCas() );
        
        getLogger().log( Level.INFO, "Writing {0} CAS to {1}{2}{3}"
            , new Object[]{ delta ? "delta" : "full", 
                getDir(), File.separator, getFileNameForCas( jcas.getCas() ) 
            }
        );
        
        try {
            switch( bf ) {
                case JAVA_S: { // Simple Java serialization.
                    CASSerializer serializer = new CASSerializer();
                    serializer.addCAS( jcas.getCasImpl() );
                    ObjectOutputStream oos = new ObjectOutputStream( os );
                    oos.writeObject( serializer );
                    break;
                }
                case JAVA_Sp: { // Simple Java serialization with type system and indexes.
                    CASCompleteSerializer serializer = new CASCompleteSerializer();
                    CASMgrSerializer casMgrSerial = new CASMgrSerializer();
                    casMgrSerial.addTypeSystem( (TypeSystemImpl) jcas.getTypeSystem() );
                    casMgrSerial.addIndexRepository( (FSIndexRepositoryImpl) jcas.getIndexRepository() );
                    CASSerializer casSerial = new CASSerializer();
                    serializer.setCasMgrSerializer( casMgrSerial );
                    serializer.setCasSerializer( casSerial );
                    ObjectOutputStream oos = new ObjectOutputStream( os );
                    oos.writeObject( serializer );
                    needsTsWritten = false;
                    break;
                }
                case UIMA_0: { // Simple Java serialization with UIMA header.
                    CASSerializer serializer = new CASSerializer();
                    if( delta ) {
                        serializer.addCAS( jcas.getCasImpl(), os, serData.getMarker( jcas ) );
                    } else {
                        serializer.addCAS( jcas.getCasImpl(), os );
                    }
                    break;
                }
                case UIMA_4: { // Compressed serialization with UIMA header
                    BinaryCasSerDes4 serializer = new BinaryCasSerDes4( (TypeSystemImpl) jcas.getTypeSystem(), false );
                    if( delta ) {
                        serializer.serialize( jcas, os, serData.getMarker( jcas ) );
                    } else {
                        serializer.serialize( jcas, os );
                    }
                    break;
                }
                case UIMA_6: { // Compressed serialization with UIMA header
                    BinaryCasSerDes6 serializer = delta ? 
                        new BinaryCasSerDes6( jcas.getCas(), serData.getReuseInfo( jcas ) ):
                        new BinaryCasSerDes6( jcas.getCas(), (TypeSystemImpl) jcas.getTypeSystem());
                    serializer.serialize( os );
                    break;
                }
                case DKPRO1: { // Compressed serialization with DKPro header
                    writeHeader( os );
                    writeTypeSystem( jcas, os );
                    BinaryCasSerDes6 serializer = delta ? 
                        new BinaryCasSerDes6( jcas.getCas(), serData.getReuseInfo( jcas ) ):
                        new BinaryCasSerDes6( jcas.getCas(), (TypeSystemImpl) jcas.getTypeSystem());
                    serializer.serialize( os );
                    needsTsWritten = false;
                    break;
                }
                default: throw new AssertionError( bf.name() );
            }
            os.flush();
            os.close();
            
            writeTypeSystem( jcas );
            
        } catch ( IOException 
                | CASRuntimeException 
                | ResourceInitializationException 
                | IllegalArgumentException ex ) 
        {
            throw new AnalysisEngineProcessException( ex );
        } finally {
            serData.deleteData( jcas );
        }
        
    }

    private void writeHeader( OutputStream os ) throws IOException {
        DataOutputStream dos = new DataOutputStream( os );
        dos.write( getHeader() );
        dos.flush();
    }

    private void writeTypeSystem( JCas jcas, OutputStream os ) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream( os );
        CASMgrSerializer casMgrSerializer = serializeCASMgr( jcas.getCasImpl() );
        oos.writeObject( casMgrSerializer );
        oos.flush();
    }

    private byte[] getHeader() {
        return new byte[]{ 'D', 'K', 'P', 'r', 'o', '1' };
    }

    private void writeTypeSystem( JCas jcas ) throws IOException {
        if( !needsTsWritten ) return;
        OutputStream os = FileUtils.getOutputStream( outputDir, tsFile, mkdirs, overwrite );
        writeTypeSystem( jcas, os );
        needsTsWritten = false;
    }
}
