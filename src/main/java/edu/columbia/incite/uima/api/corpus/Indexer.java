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
package edu.columbia.incite.uima.api.corpus;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceProcessException;

import edu.columbia.incite.uima.api.ConfigurableResource;
import edu.columbia.incite.uima.api.SessionResource;

/**
 * A shared resource that provides an interface for indexing data from a CAS.
 *
 * This interface's design assumes that document's will be created from a UIMA annotation
 * corresponding to some SOFA segment, and that (1) document metadata will be added to the created
 * document object from a collection of UIMA annotations <em>covering</em> the source document
 * annotation and (2) indexing tokens for the created document will be created from a collection of
 * UIMA annotations <em>covered</em> by the source document annotation. These assumptions are not
 * enforced by this interface, allowing for different indexing strategies depending on subclass
 * implementations.
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <D> A type for index documents.
 */
public interface Indexer<D> extends ConfigurableResource<CAS>, SessionResource<Long> {

    public static final String DEFAULT_FIELD = "text";

    /**
     * Creates an instance of type D from the given UIMA annotation.
     * The {@code doc} parameter can be {@code null} in order to allow the mapping of entire
     * CASes to index documents for type systems that do not have a document-wide annotation.
     *
     * @param doc A UIMA annotation
     *
     * @return An instance of D corresponding to an index document.
     *
     * @throws edu.columbia.incite.uima.api.corpus.Indexer.DocumentCreationException
     */
    D initDoc( AnnotationFS doc ) throws DocumentCreationException;

    /**
     * Adds additional metadata to the given instance of D from a collection of UIMA
     * {@link org.apache.uima.cas.text.AnnotationFS annotations}.
     *
     * This is useful if the indexed document should inherit features from an arbitrary collection
     * of annotations e.g. covering annotations, metadata annotations, etc.
     *
     * @param doc  An instance of D returned by {@link #initDoc(AnnotationFS)}.
     * @param data A collection of {@link org.apache.uima.cas.text.AnnotationFS annotations}
     *             containing index document metadata.
     *
     * @return The given instance of D with metadata.
     *
     * @throws edu.columbia.incite.uima.api.corpus.Indexer.DocumentMetadataException
     */
    D covers( D doc, Collection<AnnotationFS> data ) throws DocumentMetadataException;

    D text( D doc, String text, int offset );
    
    /**
     * Produce token streams for the given index document from the given collection of UIMA
     * {@link org.apache.uima.cas.text.AnnotationFS annotations}.
     * The {@code offset} parameter controls the alignment of SOFA text offsets in the input
     * annotations to the correct indexed document's offsets. This is necessary if indexed
     * documents are created from annotations with a begin offset not equal to 0 (e.g. when using
     * within-CAS segments).
     *
     * @param doc    An instance of D returned by {@link #initDoc(AnnotationFS)}.
     * @param tokens A collection of {@link org.apache.uima.cas.text.AnnotationFS annotations}
     *               containing token stream data.
     * @param offset Optional offset value that will be substracted from each annotation's begin
     *               and end offsets.
     *
     * @return The given instance of D with added token streams.
     *
     * @throws edu.columbia.incite.uima.api.corpus.Indexer.TokenStreamException
     */
    D tokens( D doc, Map<String,List<AnnotationFS>> tokens, int offset ) throws TokenStreamException;

    /**
    * Adds the given document to a capable index.
    *
    * @param doc An instance of D created by {@link #initDoc(AnnotationFS)}
     * @throws edu.columbia.incite.uima.api.corpus.Indexer.IndexingException
    *
    * @throws IOException                                       if the underlying indexing process
    *                                                           failed.
    */
    void writeToIndex( D doc ) throws IndexingException, IOException;

    public class DocumentCreationException extends ResourceProcessException {

        private static final long serialVersionUID = 187922973083055968L;

        public DocumentCreationException( String msg ) {
            super( new Throwable( msg ) );
        }

        public DocumentCreationException( Exception ex ) {
            super( ex );
        }
    }

    public class DocumentMetadataException extends ResourceProcessException {

        private static final long serialVersionUID = 7365667007617879873L;

        public DocumentMetadataException( String msg ) {
            super( new Throwable( msg ) );
        }

        public DocumentMetadataException( Exception ex ) {
            super( ex );
        }
    }

    public class TokenStreamException extends ResourceProcessException {

        private static final long serialVersionUID = -6361947742124850156L;

        public TokenStreamException( String msg ) {
            super( new Throwable( msg ) );
        }

        public TokenStreamException( Exception ex ) {
            super( ex );
        }
    }

    public class IndexingException extends ResourceProcessException {

        private static final long serialVersionUID = -2949291830020324154L;

        public IndexingException( String msg ) {
            super( new Throwable( msg ) );
        }

        public IndexingException( Exception ex ) {
            super( ex );
        }
    }
}
