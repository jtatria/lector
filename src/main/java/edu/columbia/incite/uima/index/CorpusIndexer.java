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
package edu.columbia.incite.uima.index;

import edu.columbia.incite.uima.SegmentedProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.tools.FeatureBroker;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import edu.columbia.incite.uima.SimpleResource;

/**
 * CAS indexer processor.
 * TODO: remove dependency on Lucene.
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class CorpusIndexer extends SegmentedProcessor {
    // Field options
    public static final String PARAM_ADD_DOC_FIELDS = "addDocFields";
    @ConfigurationParameter( name = PARAM_ADD_DOC_FIELDS, mandatory = false, defaultValue = "true" )
    protected Boolean addDocFields;

    public static final String RES_LUCENEFB = "fieldBroker";
    @ExternalResource( key = RES_LUCENEFB, mandatory = false )
    private FeatureBroker<Document> fieldBroker;
    
    public static final String PARAM_ADD_TOKENSTREAMS = "addTokens";
    @ConfigurationParameter( name = PARAM_ADD_TOKENSTREAMS, mandatory = false, defaultValue = "true" )
    protected Boolean addTokens;
    
    public static final String RES_STREAMS = "streams";
    @ExternalResource( key = RES_STREAMS, mandatory = false )
    protected SimpleResource<Map<String,TokenSpec>> streamMap;
    
    // Index writer options
    public static final String PARAM_DRY_RUN = "dryRun";
    @ConfigurationParameter( name = PARAM_DRY_RUN, mandatory = false, defaultValue = "false" )
    protected Boolean dryRun;
    
    public static final String RES_INDEX_WRITER = "indexWriter";
    @ExternalResource( key = RES_INDEX_WRITER, mandatory = false )
    private LuceneIndexWriter indexWriter;    
    
    private final Table<String,IndexableField,UIMATokenStream> streams = HashBasedTable.create();
    private Document docInstance;
    private Long wrtrSssn;
    
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        // Document instance
        this.docInstance = new Document();
        
        // Document metadata
        if( addDocFields ) {
            this.fieldBroker = fieldBroker == null ? new InciteLuceneBroker() : fieldBroker;
        }
        
        // Stream fields
        if( addTokens ) {
            if( streamMap == null || streamMap.get() == null || streamMap.get().isEmpty() ) {
                getLogger().log( Level.WARNING, "No streams given for token indexing" );
            }
            for( String field : streamMap.get().keySet() ) {
                TokenSpec stream = streamMap.get().get( field );
                String type = stream.typeName;
                UIMATokenStream uts = new UIMATokenStream( stream.tn );
                IndexableField f = new Field( field, uts, stream.ft );
                streams.put( type, f, uts );
            }
            List<String> memberTmp = new ArrayList<>();
            memberTmp.addAll( streams.rowKeySet() );
            this.memberTypeNames = memberTmp.toArray( new String[ memberTmp.size() ] );
        }
        
        // Index writer
        if( !dryRun ) {
            if( indexWriter == null ) {
                throw new ResourceInitializationException( 
                    ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS,
                    new Object[]{ CorpusIndexer.RES_INDEX_WRITER }
                );  
            }
            this.wrtrSssn = indexWriter.openSession();
        }
    }
        
    @Override
    protected void processSegment( AnnotationFS seg ) throws AnalysisEngineProcessException {
        try {
            // clear old values
            docInstance.getFields().clear();
            
            // update segment metadata
            if( addDocFields ) {
                fieldBroker.values( seg, docInstance );
                for( List<AnnotationFS> covers : this.covers( seg ).values() ) {
                    for( AnnotationFS cover : covers ) {
                        fieldBroker.values( cover, docInstance );
                    }
                }
            }
            
            // update tokenstreams
            if( addTokens ) {
                Map<Type,List<AnnotationFS>> members = this.members( seg );
                for( Type type : members.keySet() ) {
                    List<AnnotationFS> tokens = members.get( type );
                    Map<IndexableField,UIMATokenStream> ts = streams.row( type.getName() );
                    for( IndexableField xf : ts.keySet() ) {
                        UIMATokenStream uts = ts.get( xf );
                        uts.setInput( tokens, seg.getBegin() );
                        docInstance.add( xf );
                    }
                }
            }
            
            // write to index
            if( !dryRun ) {
                this.indexWriter.index( docInstance );
            }
            
        } catch ( IOException | CASException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }
    
    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        if( !dryRun ) {
            indexWriter.closeSession( wrtrSssn );
        }
    }
    
    public static class TokenSpec {
        private final String typeName;
        private final FieldType ft;
        private final Tokenizer tn;
        
        public TokenSpec( String typeName, FieldType ft, Tokenizer tn ) {
            this.typeName  = typeName;
            this.ft    = ft;
            this.tn    = tn;
        }
    }
    
    private class UIMATokenStream extends TokenStream {
        // Input data
        private Collection<AnnotationFS> src;
        private Integer offset;

        // State data
        private Iterator<AnnotationFS> annIt;
        private AnnotationFS cur;
        private Integer last = 0;

        private final Tokenizer tokenizer;

        private final OffsetAttribute   osAttr;
        private final CharTermAttribute ctAttr;
        private final PayloadAttribute  plAttr;
        private final TypeAttribute     tyAttr;

        /**
         * Create a new UIMATokenStream with the given Tokenizer.
         * 
         * Instances of this class can be reused across documents.
         * 
         * @param tn 
         */
        UIMATokenStream( Tokenizer tn ) {
            this.tokenizer = tn;
            this.osAttr = addAttribute( OffsetAttribute.class );
            this.ctAttr = addAttribute( CharTermAttribute.class );
            this.plAttr = addAttribute( PayloadAttribute.class );
            this.tyAttr = addAttribute( TypeAttribute.class );
        }

        /**
         * This method should be called before passing a document containing a field containing this
         * TokenStream to a consumer in order to prepare it for consumption.
         * 
         * Failure to do so will result in an IOException when consumers attempt to call this 
         * TokenStream's @link{TokenStream.reset} method.
         * 
         * @param anns      A @link{Collection} with the UIMA annotations for this field in the current 
         * document.
         * @param offset    The current document's offset into the source CAS's SOFA string.
         * 
         * @return This UIMATokenStream, ready to be added to a document's field before passing the 
         *         containing document to a consumer.
         */
        UIMATokenStream setInput( Collection<AnnotationFS> anns, int offset ) {
            this.src = anns;
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

            String term = Tokenizer.NOTERM;
            while( annIt.hasNext() && term.equals( "" ) ) {
                cur = annIt.next();
                term = tokenizer.charterm( cur );
                if( !term.equals( Tokenizer.NOTERM ) ) {

                     // TODO This is naive and needs refactoring
                    last = last > cur.getEnd() ? last : cur.getEnd();
                    if( last > cur.getEnd() ) {
                        addAttribute( PositionIncrementAttribute.class ).setPositionIncrement( 0 );
                    }

                    osAttr.setOffset( cur.getBegin() - offset, cur.getEnd() - offset );

                    ctAttr.append( term );

                    // TODO: reuse bytesref
                    plAttr.setPayload( new BytesRef( tokenizer.payload( cur ) ) );

                    tyAttr.setType( tokenizer.type( cur ) );

                    return true;
                }
            }
            return false;
        }
    }
}
