package edu.columbia.incite.uima.ae.corpus;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.corpus.Indexer;

/**
 *
 * @author gorgonzola
 * @param <D>
 */
public class NuIndexer<D> extends StructuredReader {

    public static final String RES_INDEXER = "indexer";
    @ExternalResource( key = RES_INDEXER, mandatory = true,
        description = "Index writing interface object" )
    private Indexer<D> indexer;
    
    private final AtomicLong docCounter = new AtomicLong();
    
    private long token;
    
    @Override
    public void initialize( UimaContext uCtx ) throws ResourceInitializationException {
        super.initialize( uCtx );
        token = indexer.openSession();
    }
    
    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );
        try {
            indexer.configure( jcas.getCas() );
        } catch ( Exception ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }
    
    @Override
    protected void read( Annotation ctx, Collection<AnnotationFS> covers, Collection<AnnotationFS> tokens ) {
        try {
            D doc = indexer.initDoc( ctx );
            indexer.covers( doc, covers);
            indexer.text( doc, ctx.getCoveredText(), ctx.getBegin() );
            indexer.tokens( doc, tokens, ctx.getBegin() );
            indexer.writeToIndex( doc );
        } catch( Indexer.DocumentCreationException ex ) {
            String msg = String.format( "Document creation failed for context %s in CAS %s: %s",
                docCounter.get(), getDocumentId(), ex.getMessage()
            );
            getLogger().log( Level.WARNING, msg );
        } catch( Indexer.DocumentMetadataException ex ) {
            String msg = String.format( "Metadata extraction failed for context %s in CAS %s: %s",
                docCounter.get(), getDocumentId(), ex.getMessage()
            );
            getLogger().log( Level.WARNING, msg );
        } catch( Indexer.TokenStreamException ex ) {
            String msg = String.format( "Token stream creation failed for context %s in CAS %s: %s",
                docCounter.get(), getDocumentId(), ex.getMessage()
            );
            getLogger().log( Level.WARNING, msg );
        } catch( Indexer.IndexingException ex ) {
            String msg = String.format( "Indexing failed for context %s in CAS %s: %s",
                docCounter.get(), getDocumentId(), ex.getMessage()
            );
            getLogger().log( Level.WARNING, msg );
        } catch( IOException ex ) {
            getLogger().error(
                String.format( "Can't write to %s: %s", indexer.getClass().getSimpleName(), ex.getMessage() ), ex
            );
        }
    }
    
    @Override
    public void collectionProcessComplete() {
        indexer.closeSession( token );
    }    
}
