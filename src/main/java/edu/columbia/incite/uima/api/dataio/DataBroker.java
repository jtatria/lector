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
import org.apache.uima.analysis_component.AnalysisComponent;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <R>
 */
@Deprecated
public interface DataBroker<R> extends DataCollector<R>, DataExporter<Collection<R>> {
    
    static final String PARAM_MERGE_RECORDS = "Merge Records";
    
    R getRecord( AnalysisComponent caller, Object key );
    
    Collection<R> getRecords( AnalysisComponent caller );
    
    void mergeRecords( AnalysisComponent caller );
    
}
