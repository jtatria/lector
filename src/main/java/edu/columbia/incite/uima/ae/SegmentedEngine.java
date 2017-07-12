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
package edu.columbia.incite.uima.ae;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.uima.api.types.InciteTypes;
import edu.columbia.incite.uima.res.casio.FeaturePathBroker;
import edu.columbia.incite.uima.util.JCasTools;
import edu.columbia.incite.uima.util.Types;
import edu.columbia.incite.util.data.Datum;

/**
 * A SegmentedEngine carries out its analysis logic by splitting the contents of a CAS into
 * contigous, non-overlaping segments. These segments (e.g. paragraphs) include an arbitrary number
 * of "member" annotations (e.g. words or span annotations) and are covered by a number of "cover"
 * annotations (e.g. sections, chapters) from which they inherit metadata.
 *
 * This class provides the necessary infraestructure to facillitate analysis on a per-segment basis,
 * including options to include metadata from segment and cover features. Inheriting subclasses
 * should implement their analysis logic by overriding the 
 * {@link #processSegment(AnnotationFS,Collection,Collection)}
 * abstract method.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class SegmentedEngine extends AbstractEngine {

    // TypeSystem parameters
    public static final String PARAM_SEGMENT_TYPE = "segmentTypeName";
    @ConfigurationParameter(
        name = PARAM_SEGMENT_TYPE, mandatory = false,
        description = "Type name for segment annotations."
    )
    private String segmentTypeName;

    public static final String PARAM_MEMBER_TYPES = "memberTypeNames";
    @ConfigurationParameter(
        name = PARAM_MEMBER_TYPES, mandatory = false,
        description = "Type names for segment member annotations."
    )
    private String[] memberTypeNames;

    public static final String PARAM_COVER_TYPES = "coverTypeNames";
    @ConfigurationParameter(
        name = PARAM_COVER_TYPES, mandatory = false,
        description = "Type names for cover annotations."
    )
    private String[] coverTypeNames;

    // Metadata options
    public static final String PARAM_ADD_SEGMENT_METADATA = "addSegmentMetadata";
    @ConfigurationParameter(
        name = PARAM_ADD_SEGMENT_METADATA, mandatory = false, defaultValue = "true",
        description = "If true, segment feature values will be included in metadata requests."
    )
    private Boolean addSegmentMetadata;

    public static final String RES_SEGMENT_MD_BROKER = "segmentMDBroker";
    @ExternalResource( key = RES_SEGMENT_MD_BROKER, api = FeatureBroker.class, mandatory = false,
        description = "Segment feature broker for metadata extraction."
    )
    private FeatureBroker<Datum> segmentMDBroker;

    public static final String PARAM_ADD_COVER_METADATA = "addCoverMetadata";
    @ConfigurationParameter(
        name = PARAM_ADD_COVER_METADATA, mandatory = false, defaultValue = "true",
        description = "If true, cover feature values will be included in metadata requests."
    )
    private Boolean addCoverMetadata;

    public static final String RES_COVER_MD_BROKER = "coverMDBroker";
    @ExternalResource( key = RES_COVER_MD_BROKER, api = FeatureBroker.class, mandatory = false,
        description = "Cover feature broker for metadata extraction."
    )
    private FeatureBroker<Datum> coverMDBroker;

    private Type sType;

    // Annotation indexes
    private Map<AnnotationFS,List<AnnotationFS>> coverIndex;
    private Map<AnnotationFS,List<AnnotationFS>> memberIndex;

    private AnnotationFS segment;

    @Override
    public void initialize( UimaContext uCtx ) throws ResourceInitializationException {
        super.initialize( uCtx );

        if ( segmentTypeName == null ) {
            segmentTypeName = InciteTypes.PARAGRAPH_TYPE;
        }

        getLogger().info( String.format(
            "%s initialized. Processing %s annotations as corpus segments.",
            this.getClass().getSimpleName(), segmentTypeName
        ) );

        if ( coverTypeNames == null || coverTypeNames.length == 0 ) {
            getLogger().info( "No cover types defined: Including all covering annotations." );
            coverTypeNames = new String[]{ CAS.TYPE_NAME_ANNOTATION };
        }
        if ( memberTypeNames == null || memberTypeNames.length == 0 ) {
            getLogger().info( "No token types defined: Including all covered annotations." );
            memberTypeNames = new String[]{ CAS.TYPE_NAME_ANNOTATION };
        }

        if ( addSegmentMetadata ) {
            segmentMDBroker = segmentMDBroker == null ? new FeaturePathBroker() : segmentMDBroker;
        }
        
        if ( addCoverMetadata ) {
            coverMDBroker = coverMDBroker == null ? new FeaturePathBroker() : coverMDBroker;
        }
    }

    @Override
    protected void preProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.preProcess( jcas );

        // Save segment type
        this.sType = Types.checkType( jcas.getTypeSystem(), segmentTypeName );

        // Save cover and token types
        Set<Type> cTypes = new HashSet<>();
        Set<Type> tTypes = new HashSet<>();
        Collections.addAll( cTypes, Types.checkTypes( jcas.getTypeSystem(), coverTypeNames ) );
        Collections.addAll( tTypes, Types.checkTypes( jcas.getTypeSystem(), memberTypeNames ) );

        // Build annotation indexes
        this.coverIndex = JCasTools.multiTypeCoverIndex( jcas, sType, cTypes );
        this.memberIndex = JCasTools.multiTypeMemberIndex( jcas, sType, tTypes );
    }

    @Override
    protected void realProcess( JCas jcas ) throws AnalysisEngineProcessException {
        // Get canonical segment iterator
        FSIterator<Annotation> sIt = jcas.getAnnotationIndex( sType ).iterator();

        // Loop over docs, sequentially
        int ct = 0;
        while ( sIt.hasNext() ) {
            ct++;
            this.segment = sIt.next();
            processSegment( segment, covers( segment ), members( segment ) );
            this.segment = null;
        }

        // Log doc counts for CAS
        getLogger().log( Level.INFO, String.format( "%s read %d segments from CAS %s",
            this.getClass().getSimpleName(), ct, getDocumentId() )
        );
    }

    @Override
    protected void postProcess( JCas jcas ) throws AnalysisEngineProcessException {
        super.postProcess( jcas );
        coverIndex = null;
        memberIndex = null;
    }

    protected List<AnnotationFS> covers( AnnotationFS segment ) {
        return coverIndex.get( segment );
    }

    protected List<AnnotationFS> members( AnnotationFS segment ) {
        return memberIndex.get( segment );
    }

    @Override
    protected Datum getMetadata() throws AnalysisEngineProcessException {
        Datum md = super.getMetadata();

        if( this.segment != null ) {
            if ( addSegmentMetadata ) {
                try {
                    this.segmentMDBroker.values( segment, md );
                } catch ( CASException ex ) {
                    throw new AnalysisEngineProcessException( ex );
                }
            }

            if ( addCoverMetadata ) {
                try {
                    for ( AnnotationFS cover : coverIndex.get( segment ) ) {
                        coverMDBroker.values( cover, md );
                    }
                } catch ( CASException ex ) {
                    throw new AnalysisEngineProcessException( ex );
                }
            }
        }
        return md;
    }

    protected abstract void processSegment(
        AnnotationFS segment, List<AnnotationFS> covers, List<AnnotationFS> members
    ) throws AnalysisEngineProcessException;

}
