/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.run;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

import edu.columbia.incite.uima.util.ComponentFactory;
import edu.columbia.incite.util.collection.CollectionTools;
import edu.columbia.incite.util.io.FileUtils;

/**
 *
 * @author gorgonzola
 */
public class CPERunner implements Callable<Integer> {
    
    public static final String TSD_FILENAME = "TypeSystem.xml";
    public static final String CPS_FILENAME = "settings.properties";

    private final CollectionReaderDescription crd;
    private final List<AnalysisEngineDescription> aes;
    private final AnalysisEngineDescription cons;
    private final AnalysisEngineDescription full;
    
    private final CPEConf conf;
    
    private CollectionProcessingEngine cpe;
    
    public CPERunner( CPEConf conf ) {
        try {
            this.crd  = makeReader( conf );
            this.aes  = makeAes( conf );
            this.cons = makeCons( conf );
            this.full = buildAe( aes, cons );
            this.conf = conf;
        } catch ( ResourceInitializationException ex ) {
            throw new RuntimeException( ex );
        }
    }
    
    public CPERunner build() 
    throws IOException, SAXException, CpeDescriptorException, InvalidXMLException, 
           ResourceInitializationException {

        if( conf.dumpMetaData() ) {
            dumpSettings();
            dumpTypeSystem();
        }

        CpeBuilder bldr = new CpeBuilder();
        bldr.setMaxProcessingUnitThreadCount( conf.maxThreads() );
        bldr.setReader( crd );
        bldr.setAnalysisEngine( full );
        applyErrorActions( bldr );
        this.cpe = bldr.createCpe( null );
        
        return this;
    }
    
    @Override
    public Integer call() throws Exception {
        if( cpe == null ) throw new IllegalStateException( "Runner not built!" );
        
        CallbackListener listen = new CallbackListener();
        cpe.addStatusCallbackListener( listen );
        
        try {
            cpe.process();
            synchronized( listen ) {
                while( listen.isRunning() ) {
                    // TODO: query status, progress etc.
                    listen.wait();
                }
            }
        } catch( ResourceInitializationException | InterruptedException ex ) {
            Logger.getLogger( CPERunner.class.getName() ).log( Level.SEVERE, null, ex );
            return 1;
        }
        
        return 0;
    }

    private CollectionReaderDescription makeReader( CPEConf conf ) 
    throws ResourceInitializationException {
        Class readerClass = conf.readerClass();
        return ComponentFactory.makeReaderDescription( readerClass, conf.getProps() );
    }

    private List<AnalysisEngineDescription> makeAes( CPEConf conf ) 
    throws ResourceInitializationException {
        List<AnalysisEngineDescription> aesL = new ArrayList<>();
        for( Class aeClass : conf.aeClasses() ) {
            aesL.add(
                ComponentFactory.makeEngineDescription( aeClass, conf.getProps() )
            );
        }
        return aesL;
    }

    private AnalysisEngineDescription makeCons( CPEConf conf )
    throws ResourceInitializationException {
        return conf.consumer() != null ?
            ComponentFactory.makeEngineDescription( conf.consumer(), conf.getProps() ) :
            null;
    }

    private AnalysisEngineDescription buildAe(
        List<AnalysisEngineDescription> aes, AnalysisEngineDescription cons
    ) throws ResourceInitializationException {
        List<AnalysisEngineDescription> aeL = new ArrayList<>( aes );
        if( cons != null ) aeL.add( cons );
        if( aeL.isEmpty() ) throw new IllegalArgumentException( "Empty analysis pipeline!" );
        return ComponentFactory.makeAggregateDescription( aeL );
    }

    private void applyErrorActions( CpeBuilder bldr ) throws CpeDescriptorException {
        CpeCasProcessors ccp = bldr.getCpeDescription().getCpeCasProcessors();
        if( ccp == null ) return;
        for( CpeCasProcessor proc : ccp.getAllCpeCasProcessors() ) {
            proc.setActionOnMaxError( conf.actionOnError() );
        }
    }

    private void dumpSettings() {
        List<ResourceMetaData> rmds = new ArrayList<>();
        rmds.add( crd.getMetaData() );
        aes.stream().forEach( ( ae ) -> rmds.add( ae.getMetaData() ) );
        if( cons != null ) rmds.add( cons.getMetaData() );

        try( Writer w = FileUtils.getWriter( conf.metaDir(), CPS_FILENAME, true, true ) ) {
            for( ResourceMetaData rmd : rmds ) {
                String rName = rmd.getName();
                try( 
                    Writer xmlW = FileUtils.getWriter( conf.metaDir(), rName + ".xml", true, true ) 
                ) {
                    rmd.toXML( w );
                } catch ( SAXException ex ) {
                    Logger.getLogger( CPERunner.class.getName() ).log(
                        Level.SEVERE, "Can't write descriptor for " + rName, ex
                    );
                }
                ConfigurationParameterSettings cps = rmd.getConfigurationParameterSettings();
                for( NameValuePair nvp : cps.getParameterSettings() ) {
                    String value;
                    if( CollectionTools.isArray( nvp.getValue() ) ) {
                        value = Arrays.toString( (String[]) nvp.getValue() );
                    } else {
                        value = nvp.getValue().toString();
                    }
                    String msg = String.format( "%s.%s=%s\n", rName, nvp.getName(), value );
                    w.write( msg );
                }
            }
        } catch ( IOException ex ) {
            Logger.getLogger( CPERunner.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    private void dumpTypeSystem() {
        try( Writer w = FileUtils.getWriter( conf.metaDir(), TSD_FILENAME, true, true ) ) {
            TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
            tsd.toXML( w );
        } catch ( SAXException | IOException ex ) {
            Logger.getLogger( CPERunner.class.getName() ).log(
                Level.SEVERE, "Can't write type system!", ex
            );
        } catch ( ResourceInitializationException ex ) {
            throw new RuntimeException( ex );
        }
    }
}
