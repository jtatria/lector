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
package edu.columbia.incite.uima.io.readers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.impl.BinaryCasSerDes6;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.columbia.incite.uima.io.BinaryFormat;
import edu.columbia.incite.uima.io.util.SerializationData;

import static org.apache.uima.cas.impl.Serialization.deserializeCAS;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class BinaryReader extends AbstractFileReader {
    
    public static final String PARAM_TYPE_SYSTEM_PATH = "tsPath";
    @ConfigurationParameter( name = PARAM_TYPE_SYSTEM_PATH, mandatory = false )
    private String tsPath;

    private SerializationData serData = SerializationData.getInstance();
    private CASMgrSerializer casMgr;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        if( tsPath != null ) try {
            loadCasManager();
        } catch( IOException | ClassNotFoundException ex ) {
            throw new ResourceInitializationException( ex );
        }
    }
    
    @Override
    protected void addDataFromFile( JCas jcas, FileInputStream fis ) throws CollectionException {
        try { 
            BufferedInputStream bis = new BufferedInputStream( fis );
            BinaryFormat format = BinaryFormat.forStream( bis );
            switch( format ) {
                case JAVA_S: case JAVA_Sp: {
                    ObjectInputStream ois = new ObjectInputStream( bis );
                    Object o = ois.readObject();
                    CASCompleteSerializer serializer = null;
                    
                    if( o instanceof CASCompleteSerializer ) {
                        serializer = (CASCompleteSerializer) o;
                    } else if( o instanceof CASSerializer ) {
                        if( tsPath != null ) {
                            serializer = new CASCompleteSerializer();
                            serializer.setCasMgrSerializer( loadCasManager() );
                        } else {
                            serializer = serializeCASComplete( jcas.getCasImpl() );
                        }
                        serializer.setCasSerializer( (CASSerializer) o );   
                    }
                    
                    if( serializer != null ) jcas.getCasImpl().reinit( serializer );
                    else throw new CollectionException(
                        CollectionException.INCORRECT_INPUT_TO_CAS_INITIALIZER,
                        new Object[]{ "CASSerializer or CASCompleteSerializer", o.getClass().getSimpleName() }
                    );
                    
                    break;
                }
                
                case UIMA_0: case UIMA_4: {
                    jcas.getCasImpl().reinit( bis );
                    break;
                }
                
                case UIMA_6: {
                    TypeSystemImpl ts = loadCasManager() != null ? loadCasManager().getTypeSystem() : null;
                    BinaryCasSerDes6 bcs6 = deserializeCAS( jcas.getCas(), bis, ts, serData.getReuseInfo( jcas ) );
                    serData.saveReuseInfo( jcas, bcs6.getReuseInfo() );
                    break;
                }
                
                case DKPRO1: {
                    // TODO: copy DKPro binary io code.
                    throw new UnsupportedOperationException( "DKPro 6+ format not supported yet." );
                }
                
                default: throw new AssertionError( format.name() );
            }
        } catch( ResourceInitializationException | ClassNotFoundException | IOException ex ) {
            throw new CollectionException( ex );
        }
    }
    
    private CASMgrSerializer loadCasManager() throws IOException, ClassNotFoundException {
        if( casMgr == null &&  tsPath != null ) {
            try ( FileInputStream fis = new FileInputStream( tsPath ) ) {
                ObjectInputStream ois = new ObjectInputStream( fis );
                casMgr = (CASMgrSerializer) ois.readObject();
            }
        }
        return casMgr;
    }
}
