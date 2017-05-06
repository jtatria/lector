/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.corpus;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import edu.columbia.incite.util.data.Datum;
import edu.columbia.incite.util.io.FileUtils;

/**
 *
 * @author gorgonzola
 */
public class CorpusDumper extends CorpusProcessor {
    
    public static final String DOC_SEP   = "\n";
    public static final String TOKEN_SEP = " ";
    public static final String SEC_SEP   = DOC_SEP;
    public static final String EOF       = "[EOF]" + DOC_SEP;
    public static final String EXT       = ".dump";
        
    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter( name = PARAM_OUTPUT_DIR, mandatory = false, defaultValue = "data/corpusDump",
        description = "Output Directory"
    )
    private String outputDir;
    
    public static final String PARAM_DUMP_RAW = "dumpRaw";
    @ConfigurationParameter( name = PARAM_DUMP_RAW, mandatory = false, defaultValue = "false",
        description = "Ignore all parameters and dump raw covered text (will produce duplications "
            + "if token annotations are not a segmentation type or more than one type is included)" 
    )
    private Boolean dumpRaw;
    
    private Writer out;
    
    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );
        try {
            this.out = FileUtils.getWriter( outputDir, getDocumentId() + EXT, true, true );
        } catch ( IOException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }
    
    @Override
    protected void postProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.postProcess( jcas );
        try {
            this.out.append( EOF );
            this.out.close();
        } catch ( IOException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }

    @Override
    protected void processSegment( AnnotationFS segment, List<AnnotationFS> covers, List<AnnotationFS> members ) throws AnalysisEngineProcessException {
        Datum md = getMetadata();
        try {
            if( dumpRaw ) out.append( segment.getCoveredText() );
            else {
                for( AnnotationFS ann : members ) {
                    String txt = termNormal.term( ann );
                    if( txt.length() <= 0 ) continue;
                    out.append( txt );
                    out.append( TOKEN_SEP );
                    updateCounts( ann, txt, md );;
                }
            }
        } catch ( IOException ex ) {
            Logger.getLogger(CorpusDumper.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }
    
}
