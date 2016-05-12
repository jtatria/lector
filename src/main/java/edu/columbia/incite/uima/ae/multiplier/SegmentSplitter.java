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
package edu.columbia.incite.uima.ae.multiplier;

import java.util.Collection;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.CasMultiplier_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.util.CasCopier;

import edu.columbia.incite.uima.util.Types;
import edu.columbia.incite.util.reflex.annotations.Resource;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public final class SegmentSplitter extends CasMultiplier_ImplBase { // URGENT

    public static final String PARAM_SEGMENT_TYPE = "segmentTypeName";
    @ConfigurationParameter(
        name = PARAM_SEGMENT_TYPE, mandatory = true,
        description = "F. Q. Name of the section type for splitting. Defaults to Section"
    )
    private String segTypename;

    public static final String PARAM_COPY_ANNOTATIONS = "copyAnnotations";
    @ConfigurationParameter(
        name = PARAM_COPY_ANNOTATIONS, mandatory = false, defaultValue = "true",
        description = "Copy annotations covered by sections to new CASes"
    )
    private Boolean copyAnnotations;

    @Resource
    private CAS srcCas;
    
    @Resource
    private FSIterator<AnnotationFS> zoneIt;
    
    @Resource
    private Map<AnnotationFS, Collection<AnnotationFS>> annIndex;

    @Override
    public void process( CAS cas ) throws AnalysisEngineProcessException {
        Type divType = Types.checkType( cas.getTypeSystem(), segTypename );

        srcCas = cas;
        zoneIt = cas.getAnnotationIndex( divType ).iterator();
        if( copyAnnotations ) {
            annIndex = CasUtil.indexCovered( cas, divType, cas.getTypeSystem().getType( "uima.tcas.Annotation" ) );
        }
    }

    @Override
    public boolean hasNext() throws AnalysisEngineProcessException {
        return zoneIt.hasNext();
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        // This is naive, as divs can contain each other.
        
        AnnotationFS zone = zoneIt.next();

        CAS tgtCas = getEmptyCAS();
        tgtCas.setDocumentText(zone.getCoveredText() );

        if( copyAnnotations ) {
            int offset = zone.getBegin();

            CasCopier copier = new CasCopier( srcCas, tgtCas );

            Collection<AnnotationFS> srcAnns = annIndex.get( zone );

            for( AnnotationFS src : srcAnns ) {
                AnnotationFS dstAnn = (AnnotationFS) copier.copyFs( src );
                Feature begin = dstAnn.getType().getFeatureByBaseName( "begin" );
                Feature end = dstAnn.getType().getFeatureByBaseName( "end" );
                dstAnn.setIntValue( begin, src.getBegin() - offset );
                dstAnn.setIntValue( end, src.getEnd() - offset );
                tgtCas.addFsToIndexes( dstAnn );
            }
        }

        return tgtCas;
    }
}
