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
package edu.columbia.incite.uima.io.resources;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceProcessException;
import org.xml.sax.ContentHandler;

/**
 * UIMA resource wrapper for a SAX handler to be used in Incite's XML collection readers.
 * This resource should receive a reference to an empty CAS from the {@link #configure(JCas)} method
 * in order to allow the handler to access and write CAS data from XML data.
 * This interface assumes the handler will maintain exclusive control of the CAS while processing
 * XMl data.
 * This reference <em>must</em> be released once XML data for the given CAS has finished and before
 * processing the next CAS by calling {@link #reset() } on this resource either from within this
 * resource's {@link org.xml.sax.ContentHandler#endDocument() endDocument()} method or from
 * the reader's {@link org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
 * getNext(CAS)}.
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public interface SaxHandler extends Resource, ContentHandler {

    /**
     * Pass a reference to an empty CAS to this SAX handler. This is typically done at the beginning of
     * a reader's
     * {@link org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
     * getNext(CAS)} method, before any SAX events are written to the handler.
     * <br>
     * If an implementation retains this reference after calls to this method, users of this
     * resource must release it after processing a new CAS by calling {@link #reset()} on this
     * resource.
     *
     * @param jcas
     *
     * @throws org.apache.uima.resource.ResourceProcessException
     */
    void configure( JCas jcas ) throws ResourceProcessException;

    /**
     * Release all references to the CAS.
     * This method <em>must</em> be called after all XML data for a given CAS has been processed,
     * and before strating a new one in order to ensure that all references to the finished CAS are
     * released.
     * This is typically done at the end of a reader's
     * {@link org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS) getNext(CAS)}
     * method, but may also be called internally from this handler's
     * {@link org.xml.sax.ContentHandler#endDocument() endDocument()} method.
     */
    void reset();
}
