/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.uima.api.corpus.Indexer;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.api.corpus.Tokens.LexClass;
import edu.columbia.incite.uima.res.casio.FeatureExtractor;
import edu.columbia.incite.uima.res.corpus.TermNormal;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.Datum;

import static edu.columbia.incite.uima.api.corpus.Tokens.LexClass.*;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class LuceneIndexer extends Resource_ImplBase implements Indexer<Document> {

    public static final String RES_DOCUMENT_BROKER = "docBroker";
    @ExternalResource( key = RES_DOCUMENT_BROKER, api = FeatureBroker.class, mandatory = false )
    private FeatureBroker<Datum> docBroker;

    public static final String RES_COVERS_BROKER = "coverBroker";
    @ExternalResource( key = RES_COVERS_BROKER, api = FeatureBroker.class, mandatory = false )
    private FeatureBroker<Datum> coverBroker;

    public static final String RES_WRITER_PROVIDER = "writerProvider";
    @ExternalResource( key = RES_WRITER_PROVIDER, api = WriterProvider.class, mandatory = false )
    private WriterProvider writerProvider;

    public static final String PARAM_FAIL_ON_EMPTY_MD = "failOnEmptyMD";
    @ConfigurationParameter( name = PARAM_FAIL_ON_EMPTY_MD, mandatory = false, defaultValue = "false" )
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

    private final FieldFactory fieldFactory        = new FieldFactory();
    private final AtomicLong successes             = new AtomicLong();
    private final AtomicLong failures              = new AtomicLong();

    private Map<String,ThreadLocal<UIMATokenStream>> tlStreams;
    
    private Set<Long> sessions = new HashSet<>();
    private final AtomicLong sessionTokenProvider  = new AtomicLong();
    
    public LuceneIndexer( Map<String,TermNormal> fieldNormals ) {
        Map<String,ThreadLocal<UIMATokenStream>> tmp = new HashMap<>();
        for( Entry<String,TermNormal> e : fieldNormals.entrySet() ) {
            tmp.put( e.getKey(), ThreadLocal.withInitial( () -> new UIMATokenStream( e.getValue() ) ) );
        }
        this.tlStreams = Collections.unmodifiableMap( tmp );
    }

    public LuceneIndexer() {
        // stub for UIMAfit instantiation.
    }
    
    public static final String POS_ALL_FIELD   = "pos_all";
    public static final String NPOS_ALL_FIELD  = "npos_all";
    public static final String POS_LEX_FIELD   = "pos_lex";
    public static final String NPOS_LEX_FIELD  = "npos_lex";
    
    public static final LexClass[] ALL_CLASSES = new LexClass[]{
            ADJ,
            ADV,
            ART,
            CARD,
            CONJ,
            NN,
            NP,
            O,
            PP,
            PR,
//            PUNC,
            V
        };
    
    public static final LexClass[] LEX_CLASSES = new LexClass[]{
        ADJ,
        ADV,
//        ART,
//        CARD,
//        CONJ,
        NN,
        NP,
//        O,
//        PP,
//        PR,
//        PUNC,
        V 
    };
    
    @Override
    public boolean initialize( ResourceSpecifier aSpecifier, Map<String,Object> aAdditionalParams )
    throws ResourceInitializationException {
        boolean ret = super.initialize( aSpecifier, aAdditionalParams );
        
        Map<String,ThreadLocal<UIMATokenStream>> tmp = new HashMap<>();
        
        // All POS, POS in term.
        TermNormal.Conf posAll = new TermNormal.Conf();
        posAll.setLexAction( Tokens.LexAction.POST );
        posAll.setLexClasses( ALL_CLASSES );
        tmp.put( POS_ALL_FIELD, ThreadLocal.withInitial( () -> new UIMATokenStream( posAll ) ) );
        
        // All POS, No POS in term.
        TermNormal.Conf nPosAll = new TermNormal.Conf();
        nPosAll.setLexAction( Tokens.LexAction.LEMMA );
        nPosAll.setLexClasses( ALL_CLASSES );
        tmp.put( NPOS_ALL_FIELD, ThreadLocal.withInitial( () -> new UIMATokenStream( nPosAll ) ) );
        
        // Lex POS only, POS in term.
        TermNormal.Conf posLex = new TermNormal.Conf();
        posLex.setLexAction( Tokens.LexAction.POST );
        posLex.setLexClasses( LEX_CLASSES );
        tmp.put( POS_LEX_FIELD, ThreadLocal.withInitial( () -> new UIMATokenStream( posLex ) ) );
        
        // Lex POS only, No POS in term.
        TermNormal.Conf nPosLex = new TermNormal.Conf();
        nPosLex.setLexAction( Tokens.LexAction.LEMMA );
        nPosLex.setLexClasses( LEX_CLASSES );
        tmp.put( NPOS_LEX_FIELD, ThreadLocal.withInitial( () -> new UIMATokenStream( nPosLex ) ) );
                
        tlStreams = Collections.unmodifiableMap( tmp );
        return ret;
    }
    
    @Override
    public void afterResourcesInitialized() throws ResourceInitializationException {
        super.afterResourcesInitialized();
        if( docBroker == null ) docBroker = new FeatureExtractor();
        if( coverBroker == null ) coverBroker = docBroker;
        if( writerProvider == null ) {
            writerProvider = new WriterProvider();
            writerProvider.initialize();
        }
    }

    @Override
    public Long openSession() {
        Long token = sessionTokenProvider.getAndIncrement();
        sessions.add( token );
        return token;
    }

    @Override
    public void closeSession( Long token ) {
        sessions.remove( token );
        if( sessions.isEmpty() ) {
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
    public void configure( CAS conf ) throws ResourceConfigurationException {
        docBroker.configure( conf );
        coverBroker.configure( conf );
    }

    @Override
    public Document initDoc( AnnotationFS ann ) throws DocumentCreationException {
        // TODO: document instances should be reused.
        Document doc = new Document();
        Datum d = new Datum();
        try {
            docBroker.values( ann, d );
        } catch( CASException ex ) {
            failures.incrementAndGet();
            throw new DocumentCreationException( ex );
        }
        d.fields().stream().forEach( ( t ) -> doc.add( fieldFactory.makeField( t, d ) ) );
        return doc;
    }

    @Override
    public Document covers( Document doc, Collection<AnnotationFS> data )
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
    public Document text( Document doc, String text, int offset ) {
        // Override this method in order to allow native lucene analyzers.
        return doc;
    }

    
    @Override
    public Document tokens( Document doc, Collection<AnnotationFS> tokens, int offset ) throws TokenStreamException {
        for( String field : tlStreams.keySet() ) {
            UIMATokenStream uts = tlStreams.get( field ).get();
            uts.setInput( tokens, offset );
            // TODO: if docs reused, fields should be reset instead of recreated.
            doc.add( new Field( field, uts, TEXT_FT ) );
        }
        return doc;
    }
    
//    public Document tokens( Document doc, Map<String,List<AnnotationFS>> tokens, int offset )
//    throws TokenStreamException {
//        for( Map.Entry<String,List<AnnotationFS>> e : tokens.entrySet() ) {
//            String field = e.getKey();
//            List<AnnotationFS> anns = e.getValue();
//
//            if( anns == null || anns.isEmpty() ) continue;
//
//            TokenStream ts;
//            try {
//                ts = tsFactory.makeTokens( field, anns, offset );
//            } catch( CASException ex ) {
//                failures.incrementAndGet();
//                throw new TokenStreamException( ex );
//            }
//            doc.add( new Field( field, ts, TEXT_FT ) );
//        }
//        return doc;
//    }

    @Override
    public void writeToIndex( Document doc ) throws IOException {
        writerProvider.index( doc );
        successes.incrementAndGet();
    }

    protected void validateMetadata( Datum d ) {
    }
    
    protected static class FieldFactory {

        private BiMap<DataField,Field> cache =
            Maps.synchronizedBiMap( HashBiMap.<DataField,Field>create() );

        public Set<Field> getFields() {
            return cache.inverse().keySet();
        }

        public IndexableField makeField( DataField f, Datum d ) {
            Field field = cache.computeIfAbsent( f, ( DataField fld ) -> buildField( fld ) );

            switch( f.type() ) {
                case STRING: case CHAR: case BOOLEAN: {
                    String v = (String) f.get( d );
                    field.setStringValue( v );
                    break;
                }
                case BYTE: case INTEGER: {
                    Integer v = (Integer) f.get( d );
                    field.setIntValue( v );
                    break;
                }
                case LONG: {
                    Long v = (Long) f.get( d );
                    field.setLongValue( v );
                    break;
                }
                case FLOAT: {
                    Float v = (Float) f.get( d );
                    field.setFloatValue( v );
                    break;
                }
                case DOUBLE: {
                    Double v = (Double) f.get( d );
                    field.setDoubleValue( v );
                    break;
                }
                default: throw new AssertionError( f.type().name() );
            }
            return field;
        }

        private Field buildField( DataField f ) {
            Field field = null;
            switch( f.type() ) {
                case STRING: case CHAR: case BOOLEAN:
                    field = new StringField( f.name(), "", Field.Store.YES );
                    break;
                case BYTE: case INTEGER:
                    field = new IntField( f.name(), 0, Field.Store.YES );
                    break;
                case LONG:
                    field = new LongField( f.name(), 0l, Field.Store.YES );
                    break;
                case FLOAT:
                    field = new FloatField( f.name(), 0f, Field.Store.YES );
                    break;
                case DOUBLE:
                    field = new DoubleField( f.name(), 0d, Field.Store.YES );
                    break;
                default:
                    throw new AssertionError( f.type().name() );
            }
            return field;
        }
    }
        
    protected static class UIMATokenStream extends TokenStream {
        // Input data
        private Collection<AnnotationFS> src;
        private Integer offset;

        // State data
        private Iterator<AnnotationFS> annIt;
        private AnnotationFS cur;
        private Integer last = 0;

        private final TermNormal termNormal;
        
        public UIMATokenStream( TermNormal.Conf conf ) {
            this( new TermNormal( conf ) );
        }
        
        public UIMATokenStream( TermNormal tn ) {
            this.termNormal = tn;
            addAttribute( OffsetAttribute.class );
            addAttribute( CharTermAttribute.class );
            addAttribute( PayloadAttribute.class );
            addAttribute( TypeAttribute.class );
        }

        public UIMATokenStream setInput( Collection<AnnotationFS> tokens, int offset ) {
            this.src = tokens;
            this.offset = offset;
            return this;
        }

        @Override
        public void close() throws IOException {
            super.close();
            // Clear input.
            this.src = null;
            this.offset = null;
        }

        @Override
        public void end() throws IOException {
            super.end();
            addAttribute( OffsetAttribute.class ).setOffset( last, last );
            // Clear state.
            this.annIt = null;
            this.cur = null;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            if( src == null ) {
                throw new IllegalStateException( "Input not set" );
            }
            clearAttributes();
            annIt = src.iterator();
            last = 0;
        }

        @Override
        public boolean incrementToken() throws IOException {
            clearAttributes();
            if( annIt.hasNext() ) {
                cur = annIt.next();
                last = last > cur.getEnd() ? last : cur.getEnd();
                if( last > cur.getEnd() ) { // TODO This is naive and needs refactoring
                    addAttribute( PositionIncrementAttribute.class ).setPositionIncrement( 0 );
                }
                int b = cur.getBegin() - offset;
                int e = cur.getEnd() - offset;
                getAttribute( OffsetAttribute.class ).setOffset( b, e );
//                getAttribute( CharTermAttribute.class ).append( dump( cur ) );
                getAttribute( CharTermAttribute.class ).append( termNormal.term( cur ) );
//                getAttribute( PayloadAttribute.class ).setPayload( new BytesRef( getPayload( cur ) ) );
                getAttribute( PayloadAttribute.class ).setPayload( new BytesRef( termNormal.data( cur ) ) );
//                getAttribute( TypeAttribute.class ).setType( cur.getType().getName() );
                getAttribute( TypeAttribute.class ).setType( termNormal.type( cur ) );
                return true;
            } else return false;
        }
    }
}
