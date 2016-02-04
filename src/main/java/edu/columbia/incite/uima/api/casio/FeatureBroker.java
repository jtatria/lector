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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.ConfigurableResource;

/**
 * A {@link edu.columbia.incite.uima.api.ConfigurableResource} that provides abstracted access to
 * feature values in a {@link org.apache.uima.cas.TypeSystem}
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <D> Type of object that will hold extracted feature values; typically a
 *            {@link java.util.Map} of some sort.
 */
public interface FeatureBroker<D> extends ConfigurableResource<CAS> {

    /**
     * Create a new instance of D and populate it with values extracted from the given annotation
     *
     * @param ann A UIMA annotation to extract feature values from.
     *
     * @return An instance of D holding the values extracted from the given annotation.
     *
     * @throws CASException If annotation data can not be accessed.
     */
    D values( AnnotationFS ann ) throws CASException;

    /**
     * Add feature values from the given annotation to the given instance of D.
     * This interface makes no assumptions about how {@code tgt} will deal with the added data.
     *
     * @param ann A UIMA annotation to extract feature values from.
     * @param tgt An instance of D to which data from {@code ann} will be added.
     *
     * @throws CASException If annotation data can not be accessed.
     */
    void values( AnnotationFS ann, D tgt ) throws CASException;

    /**
     * Initializes this feature broker to access data from the given CAS.
     * This allows the signaling of type system changes, etc. in order to e.g. create and initialize
     * Feature Paths, etc.
     *
     * @param cas A CAS from which annotations will have their feature values extracted.
     *
     * @throws CASException If configuration fails.
     */
    @Override
    default void configure( CAS cas ) throws CASException {
        // Nothing by default.
    }
}
