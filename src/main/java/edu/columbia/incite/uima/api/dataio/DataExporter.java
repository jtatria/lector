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
package edu.columbia.incite.uima.api.dataio;

import java.util.Map;

import org.apache.uima.analysis_component.AnalysisComponent;

import edu.columbia.incite.uima.api.OLDSessionResource;

/**
 * A DataExporter encapsulates an object that exports data outside of a UIMA runtime environment.
 * <br>
 * This interface is designed to work in parallel to a processing pipeline, and could be used to
 * replace functionality normally offered by a CASConsumer, but without the restriction of running
 * only at the end of a processing pipeline in a single consumer thread.
 * <br>
 * The interface allows for the resource to export per-component data using the user tracking
 * facilities offered by {@link edu.columbia.incite.uima.api.OLDSessionResource ReactiveResource}.
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <D>
 */
@Deprecated
public interface DataExporter<D> extends OLDSessionResource<AnalysisComponent,Map<String,String>> {
    
    /**
     * Export the given data object of type D for the given
     * {@link org.apache.uima.analysis_component.AnalysisComponent user}.
     *
     * @param caller The analysis component that is calling this resource. Analysis components
     *             should be registered by calling {@link #register(AnalysisComponent)} before calling
     *             this method.
     * @param data Data to be exported.
     *
     * @throws Exception
     */
    void export( AnalysisComponent caller, D data ) throws Exception;
   
}
