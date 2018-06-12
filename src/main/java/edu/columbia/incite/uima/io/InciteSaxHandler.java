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
package edu.columbia.incite.uima.io;

import edu.columbia.incite.uima.tools.MappingProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import edu.columbia.incite.uima.api.types.Document;
import edu.columbia.incite.uima.api.types.Paragraph;
import edu.columbia.incite.uima.api.types.Mark;
import edu.columbia.incite.util.XPathNode;
import edu.columbia.incite.uima.tools.TextFilter;

/**
 * Default implementation of Incite's SAX handler for reading CAS data from XML data.
 * 
 * This resource is capable of extracting SOFA data from an XML file's character data, as well as
 * populating the CAS with annotations created from XML elements and attributes.
 * 
 * Text normalization requires the instantiation and configuration of a {@link TextFilter}. 
 * If no filter is configured, SOFA data is written as-is (i.e. trust the source's encoding 
 * choices).
 * 
 * Annotation and feature extraction require the instantiation and configuration of a
 * {@link MappingProvider}. If no mapping provider is configured, the resulting CAS will contain 
 * only SOFA data and no annotations. 
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class InciteSaxHandler extends Resource_ImplBase implements SaxHandler {
    
    // TODO move this somewhere else.
    public static final String NO_ID = "";

    /**
     * Create annotations from XML elements and attributes. Requires a {@link MappingProvider} for 
     * {@link #RES_MAPPING_PROVIDER}.
     */
    public static final String PARAM_ANNOTATE = "annotate";
    @ConfigurationParameter( name = PARAM_ANNOTATE, mandatory = false,
        defaultValue = "true"
    )
    protected Boolean annotate;
    
    /**
     * Mark char stream processing warnings in the CAS with 0-length annotations. Only valid if 
     * {@link #PARAM_ANNOTATE} is true. Requires a {@link MappingProvider} for 
     * {@link #RES_MAPPING_PROVIDER}
     */
    public static final String PARAM_MARK_WARNINGS = "markWarnings"; // TODO: check default
    @ConfigurationParameter( name = PARAM_MARK_WARNINGS, mandatory = false, defaultValue = "true" )
    protected Boolean markWarnings;

    /**
     * Create paragraph segment annotations over SOFA data. Requires a {@link MappingProvider} for 
     * {@link #RES_MAPPING_PROVIDER}.
     */
    public static final String PARAM_MAKE_PARAGRPAHS = "makeParagraphs";
    @ConfigurationParameter( name = PARAM_MAKE_PARAGRPAHS, mandatory = false,
        defaultValue = "true"
    )
    protected Boolean makeParagraphs;
    
    /**
     * Shared resource providing a mapping from XML data to a UIMA type system.
     */
    public static final String RES_MAPPING_PROVIDER = "mappingProvider";
    @ExternalResource( key = RES_MAPPING_PROVIDER, api = MappingProvider.class, mandatory = false )
    protected MappingProvider mappingProvider;

    /**
     * Shared resource implementing a character stream processor for text normalization.
     */
    public static final String RES_CHAR_PROCESSOR = "charProcessor";
    @ExternalResource( key = RES_CHAR_PROCESSOR, api = TextFilter.class, mandatory = false )
    protected TextFilter charProcessor;
    
    // State values, null on reset
    private Stack<Annotation> annStack;
    private XPathNode curNode;
    private Paragraph paraAnn;
    private int curPara = 0;

    // Configured values, set on configure( Jcas )
    private String docId;
    private JCas jcas;
    
    @Override
    public void afterResourcesInitialized() throws ResourceInitializationException {
        super.afterResourcesInitialized();
        
        if( ( annotate || makeParagraphs ) && mappingProvider == null ) {
            throw new ResourceInitializationException( 
                ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS, 
                new Object[] { RES_MAPPING_PROVIDER }
            );
        }
        
        // TODO check BufferFilter's default configuration; we may be leaking unsuitable defaults
        if( charProcessor == null ) {
            this.charProcessor = new TextFilter.Noop();
        }
    }    
    
    //============================= SAX events ===============================//

    @Override
    public void startDocument() throws SAXException {
        // If we are making annotations, init annotation stack.
        // TODO: reuse stack
        if( annotate ) {
            annStack = new Stack<>();
        }
        
        // If making paragraphs, init paragraph.
        if( makeParagraphs ) {
            breakPara();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        // If there is an open paragprah annotation, close it.
        if( paraAnn != null ) {
            finishAnnotation( paraAnn );
            paraAnn = null;
        }

        // Set SOFA string data.
        this.jcas.setDocumentText( charProcessor.result() );
        
        // Reset everything.
        reset();
    }

    @Override
    public void characters( char[] ch, int start, int length ) {
        this.charProcessor.consume( ch, start, length );
    }

    @Override
    public void startElement( String uri, String lName, String qName, Attributes attrs )
        throws SAXException {
        // Update XPath node.
        this.curNode = curNode == null ? new XPathNode( docId ) : this.curNode.addChild( qName );
        
        if( makeParagraphs && mappingProvider.isParaBreak( qName ) ) {
            breakPara();
        }
        
        if( annotate && mappingProvider.isData( qName ) ) {
            processData( uri, lName, qName, attrs );
        }
    }

    @Override
    public void endElement( String uri, String lName, String qName ) throws SAXException {
        // Update XPath node.
        this.curNode = this.curNode.parent();
        
        if( mappingProvider.isInlineMark( qName ) ) {
            processInline( qName );
        }
        
        if( makeParagraphs && mappingProvider.isParaBreak( qName ) ) {
            breakPara();
        }

        if( annotate && mappingProvider.isAnnotation( qName ) ) {
            if( !annStack.empty() ) {
                finishAnnotation( annStack.pop() );
            }
        }
    }
    
    //========================================================================//
    
    @Override
    public void configure( CAS conf ) throws ResourceConfigurationException {
        try {
            this.jcas = conf.getJCas();
            this.mappingProvider.configure( conf );
        } catch ( CASException ex ) {
            throw new ResourceConfigurationException( ex );
        }
        Document doc = this.jcas.getAnnotationIndex( Document.class ).iterator().next();
        this.docId = doc != null ? doc.getId() : "";
    }

    @Override
    public void reset() {
        // Configured values
        this.jcas     = null;
        this.docId    = null;
        
        // State values
        this.annStack = null;
        this.curNode  = null;
        this.curPara = 0;
        
        // Charprocessor
        this.charProcessor.reset();
    }

    //========================================================================//

    private void breakPara() {
        if( paraAnn != null ) {
            if( paraAnn.getBegin() == charProcessor.curLength() ) return;
            charProcessor.addBreak( TextFilter.EOL );
            finishAnnotation( paraAnn );
        }
        paraAnn = new Paragraph( jcas, charProcessor.curLength(), charProcessor.curLength() );
        paraAnn.setId( Integer.toString( curPara++ ) );
    }

    private Map<String,String> extractAttributes( Attributes attrs ) {
        Map<String,String> data = new HashMap();
        for( int i = 0; i < attrs.getLength(); i++ ) {
            data.put( attrs.getLocalName( i ), attrs.getValue( i ) );
        }
        return data;
    }

    /**
     * Process non-annotation data from starting XML element.
     * 
     * This method will be called from
     * {@link #startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes) startElement}
     * if this handler is configured to create annotations and the configured
     * {@link MappingProvider} indicates that the XML element name of the currently processing
     * element corresponds to relevant data that does <em>not</em> map directly to a UIMA annotation.
     * 
     * This method does not perform any operation in the default implementation. 
     * 
     * This method should be overriden by subclasses that know how to handle the encountered data.
     * 
     * Overriding methods are <em>not</em> required to call this method.
     *
     * @param start Position of the processing element over the SOFA data, including modification
     *              performed by a {@link TextFilter}, if any.
     * @param qName Source element's QName.
     * @param path  Source element's unambiguous XPath expression.
     * @param data  Map containing the source element's XML attribute data.
     */
    protected void processOtherData(
        int start, String qName, String path, Map<String, String> data
    ) {
    }

    /**
     * Create annotation from starting XML element. 
     * 
     * This method will be called from
     * {@link #startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes) startElement}
     * if this handler is configured to create annotations and the configured
     * {@link MappingProvider} indicates that the XML element name of the currently processing
     * element corresponds to a UIMA annotation.
     *
     * The returned annotation's begin and end offsets will both be set to the current
     * length of the SOFA data, taking into consideration any modifications to the character stream
     * performed by a {@link TextFilter}, if one is configured. The end offset will be adjusted 
     * by the handler at the source element's
     * {@link #endElement(java.lang.String, java.lang.String, java.lang.String) endElement} call.
     *
     * This method should be overriden by subclasses that require additional capabilities for their
     * processing tasks.
     * 
     * Overriding methods <em>are required</em> to call this method in order not to interfere with 
     * this handler's processing of UIMA annotations.
     *
     * @param start Begin offset for the new UIMA annotation
     * @param qName Source element's QName.
     * @param path  Source element's unambiguous XPath expression.
     * @param data  Map containing the source element's XML attribute data.
     *
     * @return A new UIMA annotation.
     */
    protected Annotation createSpanAnnotation(
        int start, String qName, String path, Map<String, String> data
    ) {
        CAS cas = jcas.getCas();
        // Get type
        Type annType = mappingProvider.getType( qName, data );
        if( annType == null ) {
            getLogger().log( Level.WARNING, "Can\'t create annotation: No type found for qName {0}",
                new Object[] { qName }
            );
            return null;
        }

        // Create annotation
        Annotation ann = (Annotation) cas.createAnnotation( annType, start, start );

        // Populate features
        for( String attr : data.keySet() ) {
            Feature feat = mappingProvider.getFeature( annType, attr );
            if( feat == null ) {
                getLogger().log( Level.WARNING, "Can't set feature value: No feature found in type "
                    + "{0} for attribute {1} in qName {2}",
                    new Object[] { annType.getShortName(), attr, qName }
                );
                continue;
            }
            ann.setFeatureValueFromString( feat, data.get( attr ) );
        }

        return ann;
    }

    /**
     * Finish the annotation corresponding to the ending XML element.
     * 
     * This method will be called from
     * {@link #endElement(java.lang.String, java.lang.String, java.lang.String) endElement} if this
     * handler is configured to create annotations and the configured 
     * {@link MappingProvider} indicates that the XML element name of the currently processing 
     * element corresponds to a UIMA annotation.
     *
     * This method adjusts the end offset of the annotation corresponding to the currently
     * processing XMl element to the current length of the SOFA data, taking into consideration any
     * modifications to the character stream performed by a {@link TextFilter}, if one is 
     * configured, and adds the finished annotation to the CAS index.
     *
     * Subclasses should override this method if they need to add additional finalization logic.
     * 
     * Overriding methods <em>are required</em> to call this method in order to ensure correct 
     * annotation offsets.
     *
     * @param ann
     */
    protected void finishAnnotation( Annotation ann ) {
        ann.setEnd( charProcessor.curLength() );
        jcas.addFsToIndexes( ann );
    }
    
    /**
     * Process inline XML elements that do not contain CAS-relevant data, but may or may not 
     * interfere with the extraction of SOFA data.
     * 
     * This is typically the case with layout marks like line or page breaks that appear in between 
     * character nodes. The primary motivation for this method is that corpora encoders are 
     * notoriously inconsistent in their dealing with whitespace: some times these marks should be 
     * considered as whitespace, sometimes they shouldn't.
     * 
     * This method allows downstream consumers the opportunity to deal with those inconsistencies 
     * correctly.
     * 
     * @param qName 
     */
    protected void processInline( String qName ) {
        this.charProcessor.addBreak( qName );
        if( markWarnings ) {
            Mark m = new Mark( jcas, charProcessor.curLength(), charProcessor.curLength() );
            m.setNotes( qName );
            jcas.addFsToIndexes( m );
        }
    }

    private void processData( String uri, String lName, String qName, Attributes attrs ) {
        int start = charProcessor.curLength();
        String path = curNode.getPath();
        Map<String,String> data = extractAttributes( attrs );

        if( mappingProvider.isAnnotation( qName ) ) {
            Annotation ann = createSpanAnnotation( start, qName, path, data );
            if( ann != null ) {
                annStack.push( ann );
            }
        } else if( mappingProvider.isData( qName ) ) {
            processOtherData( start, qName, path, data );
        }
    }
    
    //========================================================================//
    
    protected JCas getJCas() {
        return this.jcas;
    }
    
    //====================== ContentHandler's overrides ======================//
    
    /**
     * @inheritDoc
     */
    @Override
    public void setDocumentLocator( Locator locator ) {
    }

    /**
     * @inheritDoc
     */
    @Override
    public void startPrefixMapping( String prefix, String uri ) throws SAXException {
    }

    /**
     * @inheritDoc
     */
    @Override
    public void endPrefixMapping( String prefix ) throws SAXException {
    }

    /**
     * @inheritDoc
     */
    @Override
    public void ignorableWhitespace( char[] ch, int start, int length ) throws SAXException {
    }

    /**
     * @inheritDoc
     */
    @Override
    public void processingInstruction( String target, String data ) throws SAXException {
    }

    /**
     * @inheritDoc
     */
    @Override
    public void skippedEntity( String name ) throws SAXException {
    }

}