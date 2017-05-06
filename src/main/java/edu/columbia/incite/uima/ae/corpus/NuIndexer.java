/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.ae.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.corpus.LemmaSet;
import edu.columbia.incite.uima.api.corpus.POSClass;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.res.corpus.TermNormal;
import edu.columbia.incite.uima.res.corpus.lucene.WriterProvider;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.Datum;

/**
 *
 * @author gorgonzola
 */
public class NuIndexer extends SegmentedEngine {
    
    public static final String RES_INDEX_WRITER = "indexWriter";
    @ExternalResource( key = RES_INDEX_WRITER, mandatory = true )
    private WriterProvider indexWriter;
    
    public static final String RES_FIELD_NORMALS = "fieldNormals";
//    @ExternalResource( key = RES_FIELD_NORMALS, mandatory = true )
    @ExternalResource( key = RES_FIELD_NORMALS, mandatory = false )
    protected FieldNormals fieldNormals;
    
    public static final String RES_FIELD_TYPES = "fieldTypes";
//    @ExternalResource( key = RES_FIELD_TYPES, mandatory = true )
    @ExternalResource( key = RES_FIELD_TYPES, mandatory = false )
    protected FieldTypes fieldTypes;
    
    private final Map<IndexableField,UIMATokenStream> tokenFields = new HashMap<>();
    private Document docInstance;
    private Long wrtrSssn;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        if( !fieldNormals.normals.keySet().equals( fieldTypes.types.keySet() ) ) {
            throw new ResourceInitializationException( new IllegalArgumentException(
                "Inconsistent specifications in field maps"
            ) );
        }
        
        this.docInstance = new Document();
        
        for( String key : fieldNormals.normals.keySet() ) {
            TermNormal tn = fieldNormals.normals.get( key );
            FieldType ft = fieldTypes.types.get( key );
            
            UIMATokenStream uts = new UIMATokenStream( tn );
            
            IndexableField field = new Field( key, uts, ft );
            tokenFields.put( field, uts );
            docInstance.add( field );
        }
        
        
        getLogger().log( Level.CONFIG, fieldTypes.toString() );
                
        this.wrtrSssn = indexWriter.openSession();
    }
    
    @Override
    protected void processSegment( 
        AnnotationFS seg, List<AnnotationFS> covers, List<AnnotationFS> t 
    ) throws AnalysisEngineProcessException {
        Datum md = getMetadata();
        
        for( UIMATokenStream uts : tokenFields.values() ) uts.setInput( t, seg.getBegin() );
        
        try {
            this.indexWriter.index( docInstance );
        } catch ( IOException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }
    
    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        indexWriter.closeSession( wrtrSssn );
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
                default: throw new AssertionError( f.type().name() );
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
                throw new IllegalStateException( "No input for token stream!" );
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
                // TODO: resuse bytesref.
                getAttribute( PayloadAttribute.class ).setPayload( new BytesRef( termNormal.data( cur ) ) );
//                getAttribute( TypeAttribute.class ).setType( cur.getType().getName() );
                getAttribute( TypeAttribute.class ).setType( termNormal.type( cur ) );
                return true;
            } else return false;
        }
    }
    
    public static class FieldNormals extends Resource_ImplBase {

        final Map<String,TermNormal> normals = new HashMap<>();
        
        @Override
        public boolean initialize( ResourceSpecifier spec, Map<String, Object> params )
        throws ResourceInitializationException {
            boolean ret = super.initialize( spec, params );
            getLogger().log( Level.CONFIG, this.toString() );
            return ret;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            List<String> keys = new ArrayList<>( normals.keySet() );
            Collections.sort( keys );
            for( String key : keys ) {
                sb.append( String.format( "========== %s ==========\n", key ) );
                sb.append( normals.get( key ).toString() );
            }
            return sb.toString();
        }
    
        public void add( 
            String field, boolean incPunc, boolean excNlex, boolean lemmatize, boolean addPos, 
            LemmaSet[] replace, LemmaSet[] delete 
        ) {
            TermNormal.Conf c = new TermNormal.Conf();

            List<POSClass> pos = new ArrayList<>();
        
            if( incPunc ) pos.add( POSClass.PUNC );
            pos.addAll( Arrays.asList( excNlex ? POSClass.LEX_CLASSES : POSClass.ALL_CLASSES ) );        
            c.setLexClasses(pos.toArray( new POSClass[pos.size()] ) );

            Tokens.LexAction action;
            if( addPos ) action = Tokens.LexAction.ADD_POS_TAG;
            else if( lemmatize ) action = Tokens.LexAction.LEMMATIZE;
            else action = Tokens.LexAction.KEEP_AS_IS;
            c.setLexicalAction( action );

            c.setNonLexicalAction( Tokens.NonLexAction.DELETE );

            if( delete != null && delete.length > 0 ) {
                c.setLemmaDeletions( delete );
            }

            if( replace != null && replace.length > 0 ) {
                c.setLemmaSubstitutions( replace );
            }

            this.normals.put( field, new TermNormal( c.commit() ) );
        }
        
    }
    
    public static class FieldTypes extends Resource_ImplBase {
        
        final Map<String,FieldType> types = new HashMap<>();
     
        @Override
        public boolean initialize( ResourceSpecifier spec, Map<String, Object> params )
        throws ResourceInitializationException {
            boolean ret = super.initialize( spec, params );
            getLogger().log( Level.CONFIG, this.toString() );
            return ret;
        }
        
        public void add( 
            String field, IndexOptions opts, boolean tvs, boolean tvOffsets, 
            boolean tvPayloads, boolean tvPositions 
        ) {
            FieldType ft = new FieldType();
            ft.setIndexOptions( opts );
            ft.setStoreTermVectors( tvs);
            ft.setStoreTermVectorOffsets( tvOffsets );
            ft.setStoreTermVectorPayloads( tvPayloads );
            ft.setStoreTermVectorPositions( tvPositions );
            ft.setTokenized( true );
            
            this.types.put( field, ft );
        }
        
        @Override
        public String toString() {
            
            StringBuilder sb = new StringBuilder();
            
            List<String> keys = new ArrayList<>( types.keySet() );
            Collections.sort( keys );
            
            for( String key : keys ) {
                sb.append( String.format( "========== %s ==========\n", key ) );
                sb.append( types.get( key ).toString() );
            }
            return sb.toString();
        }
    }
    
}
