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
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import edu.columbia.incite.uima.api.types.Document;
import edu.columbia.incite.uima.api.types.Paragraph;
import edu.columbia.incite.util.reflex.Resources;
import edu.columbia.incite.util.xml.XPathNode;
import edu.columbia.incite.util.reflex.annotations.Resource;

/**
 * Default implementation of Incite's SAX handler for reading CAS data from XML data.
 * This resource is capable of extracting SOFA data from an XML file's character data, as well as
 * populating the CAS with annotations created from XML elements and attributes.
 * Text normalization requires the instantiation and configuration of a {@link TextFilter}.
 * Annotation and feature extraction require the instantiation and configuration of a
 * {@link MappingProvider}.
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class InciteSaxHandler extends Resource_ImplBase implements SaxHandler {
    
    // TODO move this somewhere.
    public static final String NO_ID = "";
    public static final String EOL = "\n";

    /**
     * Filter text from XML character data before writing it to the CAS' SOFA string. Requires a {@link TextFilter}.
     */
    public static final String PARAM_FILTER_TEXT = "filterText";
    @ConfigurationParameter( name = PARAM_FILTER_TEXT, mandatory = false,
        defaultValue = "true"
    )
    private Boolean filterText;

    /**
     * Create annotations from XML elements and attributes. Requires a {@link MappingProvider}.
     */
    public static final String PARAM_ANNOTATE = "annotate";
    @ConfigurationParameter( name = PARAM_ANNOTATE, mandatory = false,
        defaultValue = "true"
    )
    private Boolean annotate;

    /**
     * Create paragraph segment annotations over SOFA data. Requires a {@link MappingProvider}.
     */
    public static final String PARAM_MAKE_PARAGRPAHS = "makeParagraphs";
    @ConfigurationParameter( name = PARAM_MAKE_PARAGRPAHS, mandatory = false,
        defaultValue = "true"
    )
    private Boolean makeParagraphs;
    
    /**
     * Break paragraph segment annotations over SOFA data by adding new lines. This is independent of formatting done by a text filter.
     */
    public static final String PARAM_BREAK_PARAGRPAHS = "breakParagraphs";
    @ConfigurationParameter( name = PARAM_BREAK_PARAGRPAHS, mandatory = false,
        defaultValue = "true"
    )
    private Boolean breakParagraphs;
    
    // TODO: add line break options. This is tricky, because the specific behaviour that needs to 
    // be implemented will vary according to the values of makeParagraphs and filterText. For now, 
    // we are asuming that there is a MappingProvider, and follow the mapping provider's 
    // instructions with regards to what is a line break. i.e. we only break lines on endElement() 
    // if the mapping provider returns true for isLineBreak(). However, this is not flexible enough 
    // and makes a mapping provider necessary even if we are not creating annotations or paragraphs. 
    // Furthermore, the actual procedure for breaking lines will be different if text is being 
    // processed by a TextFilter. Now, if we are filtering, line breaks are appended to the output 
    // character stream by calling the filter's appendBreak() method, which performs various sanity
    // checks before breaking. If we are not filtering, the HARDCODED VALUE \n is simply inserted 
    // into the output character stream without any ceremony. This causes a whole deal of trouble 
    // if the the input character stream is not prefectly consistent in marking newlines with XML 
    // tags, i.e. always.
    
    /**
     * Shared resource providing a mapping from XML data to a UIMA type system.
     */
    public static final String RES_MAPPING_PROVIDER = "mappingProvider";
    @ExternalResource( key = RES_MAPPING_PROVIDER, api = MappingProvider.class, mandatory = false )
    private MappingProvider mappingProvider;

    /**
     * Shared resource implementing a text filter.
     */
    public static final String RES_TEXT_FILTER = "textFilter";
    @ExternalResource( key = RES_TEXT_FILTER, api = TextFilter.class, mandatory = false )
    private TextFilter textFilter;

    // Created internally
    @Resource protected StringBuffer inBuffer;
    @Resource protected StringBuffer outBuffer;
    @Resource protected Stack<Annotation> annStack;
    @Resource protected XPathNode curNode;
    @Resource protected Integer curPara;
    @Resource protected Paragraph paraAnn;

    // Set on configure
    @Resource private String docId;
    @Resource private JCas jcas;

    @Override
    public boolean initialize( ResourceSpecifier spec, Map<String, Object> params )
        throws ResourceInitializationException {
        boolean out = super.initialize( spec, params );

        if( filterText && textFilter == null ) {
            textFilter = new InciteTextFilter();
        }
        
        if( ( annotate || makeParagraphs ) && mappingProvider == null ) {
            throw new ResourceInitializationException(
                ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS,
                new Object[] { RES_MAPPING_PROVIDER }
            );
        }
        
        if( mappingProvider == null ) {
            getLogger().log( Level.WARNING, 
                "Processing XML data without providing a MappingProvider is not entirely supported "
                + "yet, as text processing with or without a textfilter relies on MappingProvider's "
                + "'isLineBreak()' method. This notice will be removed as soon as this problem is "
                + "fixed"
            );
        }

        return out;
    }
    
    //============================= SAX events ===============================//

    @Override
    public void startDocument() throws SAXException {
        inBuffer = new StringBuffer();
        outBuffer = new StringBuffer();
        
        if( annotate ) {
            annStack = new Stack<>();
        }
        
        if( makeParagraphs ) {
            curPara = 0;
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if( paraAnn != null ) {
            finishAnnotation( paraAnn );
            paraAnn = null;
        }

        this.jcas.setDocumentText( outBuffer.toString() );
        reset();
    }

    @Override
    public void characters( char[] ch, int start, int length ) {
        this.inBuffer.append( ch, start, length );
    }

    @Override
    public void startElement( String uri, String lName, String qName, Attributes attrs )
        throws SAXException {
        this.curNode = curNode == null ? new XPathNode( docId ) : this.curNode.addChild( qName );
        updateCharBuffers();
        
        if( makeParagraphs ) {
            if( paraAnn == null ) {
                paraAnn = makePara( curPara++ );
            } else if( mappingProvider.isParaBreak( qName ) ) {
                finishAnnotation( paraAnn );
                paraAnn = makePara( curPara++ );
            }
        }
        
        if( annotate && mappingProvider.isData( qName ) ) {
            processData( uri, lName, qName, attrs );
        }
    }

    @Override
    public void endElement( String uri, String lName, String qName ) throws SAXException {
        this.curNode = this.curNode.parent();
        updateCharBuffers();
        
        if( annotate && mappingProvider.isAnnotation( qName ) ) {
            if( !annStack.empty() ) {
                finishAnnotation( annStack.pop() );
            }
        }

        if( makeParagraphs && paraAnn != null && mappingProvider.isParaBreak( qName ) ) {
            finishAnnotation( paraAnn );
            paraAnn = null;
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
        this.docId = doc != null ? doc.getId() : NO_ID;
    }

    @Override
    public void reset() {
        Resources.destroyFor( this );
    }

    //========================================================================//
    
    private void updateCharBuffers() {
        if( inBuffer.length() <= 0 ) {
            return;
        }
        String data = inBuffer.toString();
        inBuffer.delete( 0, inBuffer.length() );
        if( data.length() > 0 ) {
            if( filterText ) {
                textFilter.appendToBuffer( outBuffer, data );
            } else {
                outBuffer.append( data );
            }
        }
    }
    
    private Paragraph makePara( int id ) {
        int obl = outBuffer.length();
        if( obl > 0 ) {
            int pbl = EOL.length();
            if( obl - pbl > 0 && !outBuffer.substring( obl - pbl, obl ).equals( EOL ) ) {
                outBuffer.append(EOL );
            }
        }
        Paragraph p = new Paragraph( jcas, outBuffer.length(), outBuffer.length() );
        p.setId( Integer.toString( id ) );
        return p;
    }

    private Map<String, String> extractAttributes( Attributes attrs ) {
        Map<String, String> data = new HashMap();
        for( int i = 0; i < attrs.getLength(); i++ ) {
            data.put( attrs.getLocalName( i ), attrs.getValue( i ) );
        }
        return data;
    }

    /**
     * Process non-annotation data from starting XML element. This method will be called from
     * {@link #startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes) startElement}
     * if this handler is configured to create annotations and the configured
     * {@link MappingProvider} indicates that the XML element name of the currently processing
     * element corresponds to relevant data that does not map directly to a UIMA annotation.
     * This method does not perform any operation in the default implementation. This method should
     * be overriden by subclasses that know how to handle the encountered data. Overriding methods
     * are <em>not</em> required to call this method.
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
     * Create annotation from starting XML element. This method will be called from
     * {@link #startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes) startElement}
     * if this handler is configured to create annotations and the configured
     * {@link MappingProvider} indicates that the XML element name of the currently processing
     * element corresponds to a UIMA annotation.
     *
     * The returned annotation's begin and end offsets will both be set to the current
     * length of the SOFA data, taking into consideration any modifications to the character stream
     * performed by a {@link TextFilter}, if configured. The end offset will be adjusted by the
     * handler at the source element's
     * {@link #endElement(java.lang.String, java.lang.String, java.lang.String) endElement} call.
     *
     * This method should be overriden by subclasses that require additional capabilities for their
     * processing tasks. Overriding methods are required to call this method in order not
     * to interfere with this handler's processing of UIMA annotations.
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
            getLogger().log( Level.WARNING, "Can\'t create annotation: No type found for qName {0}", new Object[] { qName }
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
     * Finish annotation corresponding to the ending XML element. This method will be called from
     * {@link #endElement(java.lang.String, java.lang.String, java.lang.String) endElement} if this
     * handler is configured to create annotations and the mapping
     * provider indicates that the XML element name of the currently processing element corresponds
     * to a UIMA annotation.
     *
     * This method adjusts the end offset of the annotation corresponding to the currently
     * processing XMl element to the current length of the SOFA data, taking into consideration ant
     * modifications to the character stream performed by a {@link TextFilter}.
     *
     * Subclasses should override this method if they need to add additional finalization logic.
     * Overriding methods are required to call this method in order to produce correct annotation
     * offsets.
     *
     * @param ann
     */
    protected void finishAnnotation( Annotation ann ) {
        ann.setEnd( outBuffer.length() );
        jcas.addFsToIndexes( ann );
    }

    private void processData( String uri, String lName, String qName, Attributes attrs ) {
        int start = outBuffer.length();
        String path = curNode.getPath();
        Map<String, String> data = extractAttributes( attrs );

        if( mappingProvider.isAnnotation( qName ) ) {
            Annotation ann = createSpanAnnotation( start, qName, path, data );
            if( ann != null ) {
                annStack.push( ann );
            }
        } else if( mappingProvider.isData( qName ) ) {
            processOtherData( start, qName, path, data );
        }
    }
    
    //================= Convenience methods for child classes ================//

    /**
     * Get whether this handler is currently configured to create UIMA annotations from XML data.
     *
     * @return {@code true} if this handler is configured to create UIMA annotations from XML data.
     */
    protected boolean getAnnotate() {
        return annotate;
    }

    /**
     * Get whether this handler is currently configured to process text with a {@link TextFilter}.
     *
     * @return {@code true} if this handler is currently configured to process text with a
     *         {@link TextFilter}.
     */
    protected boolean getFilterText() {
        return filterText;
    }

    /**
     * Get the JCas that is currently being populated by this handler.
     *
     * @return The JCas currently being populated.
     */
    protected JCas getJCas() {
        return jcas;
    }

    /**
     * Set this handler's XML-UIMA mapping provider.
     * This method is provided as a convenience for subclasses, as an alternative to the
     * configuration of an additional UIMA resource.
     *
     * @param provider A reference to an implementation of {@link MappingProvider}
     */
    protected void setMappingProvider( MappingProvider provider ) {
        this.mappingProvider = provider;
    }

    /**
     * Set this handler's text filter.
     * This method is provided as a convenience for subclasses, as an alternative to the
     * configuration of an additional UIMA resource.
     *
     * @param filter A reference to an implementation of {@link TextFilter}
     */
    protected void setTextFilter( TextFilter filter ) {
        this.textFilter = filter;
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
