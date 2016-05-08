/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.casio;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.component.Resource_ImplBase;

import edu.columbia.incite.uima.api.casio.TypeProvider;
import edu.columbia.incite.uima.api.types.InciteTypes;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class InciteTypeProvider extends Resource_ImplBase implements TypeProvider {
    
    private TypeSystem ts;

    @Override
    public Feature getDocumentIdFeature() {
        return ts.getType(InciteTypes.DOCUMENT_TYPE ).getFeatureByBaseName(InciteTypes.DOC_ID_FEATURE );
    }
    
    @Override
    public List<Feature> getDocumentMetadataFeatures() {
        return ts.getType(InciteTypes.DOCUMENT_TYPE ).getFeatures();
    }

    @Override
    public Type getDocumentMetadataType() {
        return ts.getType(InciteTypes.DOCUMENT_TYPE );
    }
    
    @Override
    public Type getBaseType() {
        return ts.getType(InciteTypes.BASE_TYPE );
    }

    @Override
    public Type getSegmentType() {
        return ts.getType(InciteTypes.SEGMENT_TYPE );
    }

    @Override
    public Type getEntityType() {
        return ts.getType(InciteTypes.ENTITY_TYPE );
    }

    @Override
    public void configure( CAS cas ) {
        if( ts == null || !ts.equals( cas.getTypeSystem() ) ) this.ts = cas.getTypeSystem();
    }

}
