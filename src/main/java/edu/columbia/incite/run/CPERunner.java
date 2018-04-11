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
package edu.columbia.incite.run;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import edu.columbia.incite.Conf;
import edu.columbia.incite.uima.util.ComponentFactory;
import edu.columbia.incite.util.CollectionTools;
import edu.columbia.incite.util.FileUtils;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class CPERunner implements Callable<Integer> {
    
    public static final Path TSD_FILENAME = Paths.get( "TypeSystem.xml" );
    public static final Path CPS_FILENAME = Paths.get( "settings.properties" );

    private final CollectionReaderDescription crd;
    private final List<AnalysisEngineDescription> aes;
    private final AnalysisEngineDescription cons;
    private final AnalysisEngineDescription full;
    
    private final Conf conf;
    
    private CollectionProcessingEngine cpe;
    
    public CPERunner( Conf conf ) {
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

        if( conf.dumpConf() ) {
            dumpSettings();
            dumpTypeSystem();
        }

        CpeBuilder bldr = new CpeBuilder();
        bldr.setMaxProcessingUnitThreadCount( conf.threads() );
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

    private CollectionReaderDescription makeReader( Conf conf ) 
    throws ResourceInitializationException {
        Class readerClass = conf.uimaReader();
        return ComponentFactory.makeReaderDescription( readerClass, conf.getProps() );
    }

    private List<AnalysisEngineDescription> makeAes( Conf conf ) 
    throws ResourceInitializationException {
        List<AnalysisEngineDescription> aesL = new ArrayList<>();
        for( Class aeClass : conf.uimaAes() ) {
            aesL.add(
                ComponentFactory.makeEngineDescription( aeClass, conf.getProps() )
            );
        }
        return aesL;
    }

    private AnalysisEngineDescription makeCons( Conf conf )
    throws ResourceInitializationException {
        return conf.uimaConsumer() != null ?
            ComponentFactory.makeEngineDescription( conf.uimaConsumer(), conf.getProps() ) :
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
            proc.setActionOnMaxError( conf.uimaOnerr() );
        }
    }

    private void dumpSettings() {
        List<ResourceMetaData> rmds = new ArrayList<>();
        rmds.add( crd.getMetaData() );
        aes.stream().forEach( ( ae ) -> rmds.add( ae.getMetaData() ) );
        if( cons != null ) rmds.add( cons.getMetaData() );

        try( Writer w = FileUtils.getWriter( conf.homeDir().resolve( CPS_FILENAME ), true, true ) ) {
            for( ResourceMetaData rmd : rmds ) {
                String rName = rmd.getName();
                try( 
                    Writer xmlW = FileUtils.getWriter( conf.homeDir().resolve( rName + ".xml" ), true, true ) 
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
        try( Writer w = FileUtils.getWriter( conf.homeDir().resolve( TSD_FILENAME ), true, true ) ) {
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
