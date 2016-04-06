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
package edu.columbia.incite.uima.ae.corpus;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.ae.AbstractEngine;
import edu.columbia.incite.uima.res.dataio.SimpleDataResource;
import edu.columbia.incite.util.collection.Collections;
import edu.columbia.incite.util.data.DataSet;
import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.DataFieldType;
import edu.columbia.incite.util.data.jaxb.JAXBDataSet;
import edu.columbia.incite.util.io.FileUtils;
import edu.columbia.incite.util.reflex.annotations.NullOnRelease;
/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class NaiveNgram extends AbstractEngine {

    public static final String PARAM_FOCUS_REGEX = "focusRegex";
    @ConfigurationParameter( name = PARAM_FOCUS_REGEX, mandatory = true )
    private String focusRegex;

    public static final String PARAM_MARKER_STRING = "marker";
    @ConfigurationParameter( name = PARAM_MARKER_STRING, mandatory = false, defaultValue = "|%t|" )
    private String marker;

    public static final String PARAM_WINDOW_SIZE = "windowSize";
    @ConfigurationParameter( name = PARAM_WINDOW_SIZE, mandatory = false, defaultValue = "3" )
    private Integer w;

    public static final String PARAM_OUTDIR = "outDir";
    @ConfigurationParameter( name = PARAM_OUTDIR, mandatory = false )
    private String outDir;

    public static final String PARAM_OUTFILE = "outfile";
    @ConfigurationParameter( name = PARAM_OUTFILE, mandatory = false )
    private String outFile;

    public static final String RES_DATA = "dataResource";
    @ExternalResource( key = RES_DATA, mandatory = false )
    private SimpleDataResource<DataSet<Datum>> data;

    private Pattern focus;

    private Map<List<String>,Integer> seen = new HashMap<>();

    private DataField<String>[] fields;
    private DataField<Long> ct;

    @NullOnRelease private List<Token> tokens;
    @NullOnRelease private Integer n;
    @NullOnRelease private Boolean stop;

    private Long sessionId;

    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );

        focus = Pattern.compile( "^" + focusRegex + "$" );

        outDir = outDir == null ? System.getProperty( "user.dir" ) : outDir;
        outFile = outFile == null ? "ngrams.xml" : outFile;

        fields = makeFields();

        if( data == null ) data = new SimpleDataResource<>();

//        sessionId = data.openSession();
//        data.configure( new DataSet<>() );

        data.setMerger( ( DataSet<Datum> t, DataSet<Datum> u ) -> {
            for( Datum r : u.getRecords() ) {
                t.addRecord( r );
            }
            return t;
        } );

        data.setConsumer( ( DataSet<Datum> t ) -> {
            try {
                OutputStream os = FileUtils.getOutputStream( outDir, outFile, true, true );
                ( new JAXBDataSet( t, true ) ).export( os );
            } catch( IOException | JAXBException ex ) {
                getLogger().log( Level.SEVERE, ex.toString() );
            }
        } );
    }

    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        tokens = new ArrayList<>( JCasUtil.select( jcas, Token.class ) );
        n = tokens.size();
        stop = n <= 0;
    }

    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        if( stop ) return;
        for( int i = 0; i < n; i++ ) {
            if( focus.matcher( tokens.get( i ).getCoveredText() ).find() )
                updateDataPoint( Collections.<Token>window( tokens, i, w ) );
        }
    }

    @Override
    public void collectionProcessComplete() {
//        data.closeSession( sessionId );
    }

    private DataField[] makeFields() {
        DataField[] f = new DataField[ w * 2 + 1 ];
        for( int i = -w; i <= w; i++ ) {
            String key = i < 0 ? "pre" : i > 0 ? "pos" : "FOCUS";
            f[w + i] = new DataField<>(
                key != "FOCUS" ? key + Integer.toString( Math.abs( i ) ) : key,
                DataFieldType.STRING
            );
        }
        ct = new DataField<>( "ct", DataFieldType.LONG );
        return f;
    }

    private void updateDataPoint( List<Token> window ) {
        Datum d = new Datum();
        for( int i = 0; i < window.size(); i++ ) {
            String value = window.get( i ) != null ? window.get( i ).getCoveredText() : "NA";
            d.set( fields[i], value );
        }
        d.set( ct, 1 );
//        data.getData().addRecord( d );
    }
}
