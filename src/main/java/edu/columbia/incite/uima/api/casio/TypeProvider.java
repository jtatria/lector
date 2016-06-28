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
package edu.columbia.incite.uima.api.casio;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.ConfigurableResource;

/**
 * A shared resource that provides abstracted access to a TypeSystem.
 *
 * This interface has been designed with particular attention to Document Metadata types, as these
 * are usually redefined by applications for different purposes.
 * <br>
 * It also supports abstracted access to three categories of types that are commonly used in
 * analysis engines for indexing purposes: A Base type, that is understood to be the root node in a
 * subtree within the type system (i.e. the common ancestor for all types in an application's
 * domain-specific type system); a Segment type, considered to be the root node for a
 * subtree of types that split the CAS SOFA into contiguous and non-overlapping segments
 * (also referred to as zones in older versions of Incite's UIMA API); an Entity type,
 * understood to be the root node in a subtree of types that identify entities that are associated
 * to arbitrary spans of the CAS's SOFA.
 * <br>
 * See {@link OLDSessionResource} for usage details for this resource.
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public interface TypeProvider extends ConfigurableResource<CAS> {

    /**
     * Get a UIMA annotation type that implements document metadata annotations.
     *
     * @return A UIMA type capable of holding document metadata.
     */
    Type getDocumentMetadataType();

    /**
     * Get the document id feature from the metadata annotation type.
     * This should normally contain an unambiguous identifier for the specific analysis unit 
     * corresponding to a CAS's SOFA data e.g. a source file's URL, etc.
     *
     * @return A UIMA Feature capable of storing a document identifier that is unique within a
     *         given document collection.
     */
    Feature getDocumentIdFeature();

    /**
     * Get the root node for this application's type system.
     * If this TypeProvider is providing access to a typesystem that lacks a unique root node, this
     * method should return the uima.cas.TOP type.
     *
     * @return The root of this application's domain-specific type system, if it exists.
     */
    Type getBaseType();

    /**
     * Get the root node for this application's segmentation types.
     * Segmentation types are understood to be those types that define contiguous and
     * non-overlapping annotations over the entire CAS SOFA.
     *
     * @return  The root node of the subtree of this application's segmentation types from the type 
     *          system.
     */
    Type getSegmentType();

    /**
     * Get the root node for this application's entity types.
     * Entity types are understood to be those types that define arbitrary spans of SOFA data that
     * is associated to a given analytically relevant entity (i.e. annotations that are not
     * CAS metadata containers or segments and are associated to non-contiguous and possibly
     * overlapping spans of SOFA data.).
     *
     * @return The root node of the subtree of this application's entity types from the type system.
     */
    Type getEntityType();

    /**
     * Convenience method to return the document id from the feature returned by
     * {@link #getDocumentIdFeature()} in the first annotation of the type returned by
     * {@link #getDocumentMetadataType()} in the given CAS.
     *
     * @param cas A CAS.
     *
     * @return  A collection-unique id for the given CAS, if it exists.
     */
    default String getCasId( CAS cas ) {
        AnnotationFS dmd = cas.getAnnotationIndex( getDocumentMetadataType() ).iterator().next();
        if( dmd != null ) {
            return dmd.getFeatureValueAsString( getDocumentIdFeature() );
        }
        return null;
    }

    /**
     * Get all available features in the type returned by {@link #getDocumentMetadataType()}.
     *
     * @return A list of features implementing document metadata.
     */
    default List<Feature> getDocumentMetadataFeatures() {
        return getDocumentMetadataType().getFeatures();
    }
}
