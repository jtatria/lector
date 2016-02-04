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
package edu.columbia.incite.uima.ae.translator;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

/**
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public class FeatureMap {
    
    private Map<String,String> map = new HashMap<>();
    
    public FeatureMap() {
        map.put( "id", "documentId" );
        map.put( "uri", "documentUri" );
        map.put( "collection", "collectionId" );
        map.put( "proc_isLast", "isLastSegment" );
    }

    public Feature getTargetFeature( Type tgtType, Feature srcFeat ) {
        String tgtFeatName = getTargetFeatureName( srcFeat.getShortName() );
        Feature tgt = tgtType.getFeatureByBaseName( tgtFeatName );
        if( tgt != null ) return tgt;
        return null;
    }

    private String getTargetFeatureName( String name ) {
        if( map.containsKey( name ) ) return map.get( name );
        else return name;
    }
    
}
