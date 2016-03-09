/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.uima.api.corpus.Indexer;
import edu.columbia.incite.uima.api.corpus.TokenFactory;
import edu.columbia.incite.uima.res.casio.FeatureExtractor;
import edu.columbia.incite.uima.res.corpus.token.LuceneTSFactory;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.Datum;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class LuceneIndexer extends Resource_ImplBase implements Indexer<Document> {

    public static final String PARAM_FIELD_NAME = "fieldName";
    @ConfigurationParameter( name = PARAM_FIELD_NAME, mandatory = false, defaultValue = "" )
    private String fieldName;

    public static final String RES_DOCUMENT_BROKER = "docBroker";
    @ExternalResource( key = RES_DOCUMENT_BROKER, api = FeatureBroker.class, mandatory = false )
    private FeatureBroker<Datum> docBroker;

    public static final String RES_COVERS_BROKER = "coverBroker";
    @ExternalResource( key = RES_COVERS_BROKER, api = FeatureBroker.class, mandatory = false )
    private FeatureBroker<Datum> coverBroker;

    public static final String RES_WRITER_PROVIDER = "writerProvider";
    @ExternalResource( key = RES_WRITER_PROVIDER, api = WriterProvider.class, mandatory = false )
    private WriterProvider writerProvider;

    public static final String RES_TOKEN_STREAM_FACTORY = "tsFactory";
    @ExternalResource( key = RES_TOKEN_STREAM_FACTORY, api = LuceneTSFactory.class, mandatory = false )
    private TokenFactory<TokenStream> tsFactory;

    public static final String PARAM_FAIL_OIN_EMPTY_MD = "failOnEmptyMD";
    private Boolean failOnEmptyMD = false;

    public static final FieldType TEXT_FT = new FieldType();
    static {
        TEXT_FT.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS );
        TEXT_FT.setStoreTermVectors( true );
        TEXT_FT.setStoreTermVectorOffsets( true );
        TEXT_FT.setStoreTermVectorPayloads( true );
        TEXT_FT.setStoreTermVectorPositions( true );
        TEXT_FT.setTokenized( true );
    }

    private FieldFactory fieldFactory = new FieldFactory();
    private Set<Long> users = new HashSet<>();
    private final AtomicLong tokenProvider = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();

    @Override
    public void afterResourcesInitialized() throws ResourceInitializationException {
        super.afterResourcesInitialized();

        if( docBroker == null ) docBroker = new FeatureExtractor();
        if( coverBroker == null ) coverBroker = docBroker;
        if( tsFactory == null ) tsFactory = new LuceneTSFactory();
        if( writerProvider == null ) {
            writerProvider = new WriterProvider();
            writerProvider.initialize();
        }

        getLogger().log( Level.INFO,
            "LuceneIndexer: Indexing CAS data to field {0}.", fieldName
        );
    }

    @Override
    public Long openSession() {
        Long token = tokenProvider.getAndIncrement();
        users.add( token );
        return token;
    }

    @Override
    public void closeSession( Long token ) {
        users.remove( token );
        if( users.isEmpty() ) {
            try {
                writerProvider.close();
                String msg = String.format(
                    "Indexing complete. %d contexts successfuly indexed, %d failed.",
                    successes.get(), failures.get()
                );
                getLogger().log( Level.INFO, msg );
            } catch( IOException ex ) {
                throw new RuntimeException( ex );
            }
        }
    }

    @Override
    public void configure( CAS conf ) throws Exception {
        docBroker.configure( conf );
        coverBroker.configure( conf );
        tsFactory.configure( conf );
    }

    @Override
    public Document createDocument( AnnotationFS ann ) throws DocumentCreationException {
        // TODO: document instances should be reused.
        Document doc = new Document();
        Datum d = new Datum();
        try {
            docBroker.values( ann, d );
        } catch( CASException ex ) {
            failures.incrementAndGet();
            throw new DocumentCreationException( ex );
        }
        d.fields().stream().forEach( ( DataField t ) -> doc.add( fieldFactory.makeField( t, d ) ) );
        return doc;
    }

    @Override
    public Document addMetadata( Document doc, Collection<AnnotationFS> data )
    throws DocumentMetadataException {
        if( data != null && data.size() > 0 ) {
            Datum d = new Datum();
            for( AnnotationFS ann : data ) {
                try {
                    coverBroker.values( ann, d );
                } catch( CASException ex ) {
                    failures.incrementAndGet();
                    throw new DocumentMetadataException( ex );
                }
            }
            validateMetadata( d );
            d.fields().stream().forEach( ( f ) -> doc.add( fieldFactory.makeField( f, d ) ) );
        } else {
            if( failOnEmptyMD ) {
                failures.incrementAndGet();
                throw new DocumentMetadataException( "No metadata annotations found." );
            }
        }
        return doc;
    }

    @Override
    public Document makeTokens( Document doc, Map<String,List<AnnotationFS>> tokens, int offset )
    throws TokenStreamException {
        for( Map.Entry<String,List<AnnotationFS>> e : tokens.entrySet() ) {
            String field = e.getKey();
            List<AnnotationFS> anns = e.getValue();

            if( anns == null || anns.isEmpty() ) continue;

            TokenStream ts;
            try {
                ts = tsFactory.makeTokens( field, anns, offset );
            } catch( CASException ex ) {
                failures.incrementAndGet();
                throw new TokenStreamException( ex );
            }
            doc.add( new Field( field, ts, TEXT_FT ) );
        }
        return doc;
    }

    @Override
    public void index( Document doc ) throws IOException {
        successes.incrementAndGet();
        writerProvider.index( doc );
    }

    protected void validateMetadata( Datum d ) {}
}
