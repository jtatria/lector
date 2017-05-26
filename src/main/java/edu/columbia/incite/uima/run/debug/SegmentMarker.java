/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.run.debug;

import edu.columbia.incite.uima.ae.SegmentedEngine;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.Datum;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;

/**
 *
 * @author jta
 */
public class SegmentMarker extends SegmentedEngine {

    Set<DataField> dfs = new HashSet<>();
    
    @Override
    protected void processSegment( AnnotationFS segment, List<AnnotationFS> covers, List<AnnotationFS> members ) throws AnalysisEngineProcessException {
        Datum md = getMetadata();
        for( DataField df : md.fields() ) {
            if( !dfs.contains( df ) ) {
                dfs.add( df );
            }
        }
    }
    
    @Override
    public void collectionProcessComplete() {
        for( DataField df : dfs ) {
            System.out.printf( "%s:%s\n", df.name, df.type.toString() );
        }
    }
}
