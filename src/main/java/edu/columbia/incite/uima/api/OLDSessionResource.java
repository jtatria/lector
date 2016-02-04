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
package edu.columbia.incite.uima.api;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.resource.Resource;

/**
 * A UserResource encapsulates an object that modifies its behaviour in reaction to processing
 * events. In order for implementations to keep track of analysis components using this resource,
 * components should call {@link #register(AnalysisComponent) } before calling any other method in
 * this resource, and should signal completion by calling {@link #unregister(AnalysisComponent)}.
 * <br>
 * The behaviour of this resource changes in response to calls to
 * {@link #configure(AnalysisComponent, Object...)} methods.
 * <br>
 * Typically, an analysis component would call {@link #register(AnalysisComponent)} during its
 * {@link org.apache.uima.analysis_component.AnalysisComponent#initialize(UimaContext) initialize}
 * method, then call {@link #reset(AnalysisComponent)} on entering its
 * {@link org.apache.uima.analysis_component.AnalysisComponent#process(AbstractCas) process} method,
 * modify this resource's configuration with calls to
 * {@link #configure(AnalysisComponent,Object...)}, invoke any finalization logic by calling
 * {@link #finish(AnalysisComponent,boolean)} when done with the current configuration and
 * unregister itself when completely done by calling {@link #unregister(AnalysisComponent)} from its
 * {@link org.apache.uima.analysis_component.AnalysisComponent#collectionProcessComplete() collectionProcessComplete}
 * method.
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <I>
 *            Type for session keys.
 *
 * @param <C>
 *            Type of configuration parameters that modify the behaviour of this resource. (e.g. a CAS,
 *            Annotations, a Map with parameters, etc.).
 */
@Deprecated
public interface OLDSessionResource<I,C> extends Resource {

    /**
     * Registers an analysis component with this resource. Registration is required if the resource
     * implementation needs to keep track of running components.
     *
     * @param userId
     *             The analysis component to be registered.
     */
    void register( I userId );
    
    /**
     * Sets additional parameters for the given Analysis Component in this resource. Parameter
     * checking and their effects are left to implementations.
     *
     * @param userId
     *              The analysis component that is calling this resource. Analysis components should
     *              be registered by calling {@link #register(AnalysisComponent)} before calling this
     *              method.
     * @param conf
     *              Additional parameter objects
     * @throws org.apache.uima.UIMAException
     */
    void configure( I userId, C conf ) throws UIMAException;

    /**
     * Signals to this resource that the given analysis component is done processing for the
     * current configuration.
     * <br>
     * If an implementation retained any references to a CAS after calls to {@link
     * #configure(AnalysisComponent,Object...)} (e.g. if using CAS members for configuration),
     * they must be released after calls to this method.
     *
     * @param userId
     *               The analysis component that is calling this resource. Analysis components should
     *               be registered by calling {@link #register(AnalysisComponent)} before calling this
     *               method.
     */
    void finish( I userId );

    /**
     * Clears configuration data and state in this resource for the given analysis component.
     * <br>
     * This method should be called by an analysis component that requires more than one
     * configuration during the processing of a single CAS.
     * <br>
     * This resource's state after a call to reset should be equivalent to its state after
     * calling {@link #register(AnalysisComponent) } but before any calls to
     * {@link #configure(AnalysisComponent,Object...) }.
     *
     * @param userId
     *             The analysis component that is requesting the reset of its configuration data.
     */
    void reset( I userId );

    /**
     * Signals to this resource that the analysis component is finished. No further method calls
     * should be allowed from this analysis component after calling this method, and this resource
     * is free to dispose of any resources held for use by the analysis component.
     * <br>
     * This method should be called from an analysis component's
     * {@link org.apache.uima.analysis_component.AnalysisComponent#collectionProcessComplete()
     * collectionProcessComplete} method.
     * method.
     *
     * @param userId
     *             The analysis component that will be unregistered from this resource.
     */
    void unregister( I userId );
}
