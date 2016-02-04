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

import java.util.Collection;
import java.util.Map;

import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.resource.Resource;

import edu.columbia.incite.uima.api.OLDSessionResource;

/**
 * A DataCollector resource encapsulates an object that is capable of collecting structured records 
 * from an analysis engine.
 * <br>
 * This interface is designed to work in parallel to a processing pipeline, and could be used to 
 * replace functionality normally offered by a CASConsumer, but without the restriction of running 
 * only at the end of a processing pipeline in a single consumer thread.
 * <br>
 * The interface allows for the collector to maintain per-component data using the user tracking 
 * facilities offered by {@link edu.columbia.incite.uima.api.OLDSessionResource ReactiveResource}.
 * 
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <T>   Type for data objects.
 */
@Deprecated
public interface DataCollector<T> extends OLDSessionResource<AnalysisComponent,Map<String,String>> {
    
    static final String PARAM_MERGE_DATA = "Merge data";
        
    /**
     * Add a record of type T from a given 
     * {@link org.apache.uima.analysis_component.AnalysisComponent}.
     * 
     * @param caller  The analysis component that is calling this resource. Analysis components 
     *      should be registered by calling {@link #register(AnalysisComponent)} before calling 
     *      this method.
     * @param record    The record to be added.
     * @return  Returns true if the operation succeeded. Calls to 
     *      {@link #hasRecord(AnalysisComponent, Object)} must return true after calling this 
     *      method if this method returned true.
     */
    Object addRecord( AnalysisComponent caller, T record );

    /**
     * Check whether this DataCollector already contains a given record associated to the given 
     * {@link org.apache.uima.analysis_component.AnalysisComponent}.
     * 
     * @param caller  The analysis component that is calling this resource. Analysis components 
     *      should be registered by calling {@link #register(AnalysisComponent)} before calling 
     *      this method.
     * @param record    The record to be added.
     * @return  Returns true if this resource contains the given record for the given user.
     */
    boolean hasRecord( AnalysisComponent caller, T record );

}
