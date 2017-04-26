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
package edu.columbia.incite.uima.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Enumeration of supported binary formats used for serialization of UIMA data structures.
 * This class also includes some utility methods for obtaining an instance of BinaryFormat from a
 * binary header or a string key.
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public enum BinaryFormat {

    /**
     * Plain Java serialization using CASSerializer. *
     */
    JAVA_S( new byte[] { -84, -19, 0, 5 }, "S" ),
    /**
     * Plain Java serialization using CASCompleteSerializer. *
     */
    JAVA_Sp( new byte[] { -84, -19, 0, 5 }, "S+" ),
    /**
     * Uncompressed UIMA binary format. *
     */
    UIMA_0( new byte[] { 'U', 'I', 'M', 'A' }, "0" ),
    /**
     * Form 4 compressed UIMA binary format. *
     */
    UIMA_4( new byte[] { 'U', 'I', 'M', 'A' }, "4" ),
    /**
     * Form 6 compressed UIMA binary format. *
     */
    UIMA_6( new byte[] { 'U', 'I', 'M', 'A' }, "6" ),
    /**
     * DKPro1 compressed UIMA binary format (Form 6 plus meta data). *
     */
    DKPRO1( new byte[] { 'D', 'K', 'P', 'r' }, "6+" ),;

    private final byte[] header;
    private final String key;

    private BinaryFormat( byte[] header, String key ) {
        this.header = header;
        this.key = key;
    }

    /**
     * Return the string key used to refer to this format in configuration parameters.
     *
     * @return A String that can be used to represent this format in configuration parameter
     *         settings.
     */
//    public String key() {
//        return this.key;
//    }

    /**
     * Return the four byte header associated to this format.
     * This header is used internally to identify the correct format of data from an input stream.
     *
     * @return A byte array of size 4 holding the magic number for this format.
     */
    public byte[] header() {
        return this.header;
    }

    /**
     * Return the correct binary format for the given string key.
     * This methods operates as the inverse of {@link #key()}, s.t.
     * BinaryFormat.forString( "KEY" ).key() == "KEY".
     *
     * @param key A string key, like the ones returned by the {@link #key()} instance method.
     *
     * @return A BinaryFormat for the given key.
     *
     * @throws IllegalArgumentException
     */
//    public static BinaryFormat forString( String key ) throws IllegalArgumentException {
//        for( BinaryFormat bf : BinaryFormat.values() ) {
//            if( bf.key().equals( key ) ) {
//                return bf;
//            }
//        }
//        throw new IllegalArgumentException( "Unknown format requested: " + key );
//    }

    /**
     * Identify the binary format of data contained in the given stream.
     * WARNING: This method has side effects in that it reads data from the given stream. Users are
     * advised to validate the state of the given stream for further reading operations (i.e. fill
     * buffers, reset offsets, etc.) after calling this method.
     *
     * @param is An input stream.
     *
     * @return The BinaryFormat corresponding to the data contained in the given stream.
     *
     * @throws IOException If data could not be read or the format is unknown.
     */
    public static BinaryFormat forStream( InputStream is ) throws IOException {
        BufferedInputStream bis = is instanceof BufferedInputStream ? (BufferedInputStream) is
            : new BufferedInputStream( is );

        bis.mark( 32 );
        byte[] header = new byte[4];
        int read = bis.read( header );
        if( Arrays.equals( header, JAVA_S.header() ) ) {
            bis.reset();
            return JAVA_S;
        } else if( Arrays.equals( header, UIMA_0.header() ) ) {
            int v0 = ( new DataInputStream( bis ) ).readInt();
            int v1 = ( new DataInputStream( bis ) ).readInt();
            bis.reset();
            if( v0 == 1 ) {
                return UIMA_0;
            } else if( v1 == 0 ) {
                return UIMA_4;
            } else {
                return UIMA_6;
            }
        } else if( Arrays.equals( header, DKPRO1.header() ) ) {
            return DKPRO1;
        }
        throw new IOException( "Unknown format for input stream" );
    }

}
