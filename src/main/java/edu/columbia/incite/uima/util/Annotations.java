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
package edu.columbia.incite.uima.util;

import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Utility methods for accessing UIMA annotation feature data.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public abstract class Annotations {

    /**
     * Populate the given map with the feature values found in the given annotation, following the 
     * inclusion/exclusion rules passed in the given regular expression pattern.
     * 
     * @param ann An {@link AnnotationFS}
     * @param target The target {@link Map}.
     */
    public static void extractFeatureValues( AnnotationFS ann, Map target ) {
        extractFeatureValues( ann, target, null, null );
    }

    /**
     * Populate the given map with the feature values found in the given annotation, following the 
     * inclusion/exclusion rules passed in the given regular expression pattern.
     * 
     * @param ann An {@link AnnotationFS}
     * @param target The target {@link Map}.
     * @param pattern An inclusion/exclusion pattern.
     */
    public static void extractFeatureValues( AnnotationFS ann, Map target, String pattern ) {
        extractFeatureValues( ann, target, pattern, null );
    }

    /**
     * Populate the given map with the feature values found in the given annotation, following the 
     * inclusion/exclusion rules passed in the given regular expression pattern.
     * 
     * @param ann An {@link AnnotationFS}
     * @param target The target {@link Map}.
     * @param pattern An inclusion/exclusion pattern.
     * @param prefix An optional feature name prefix.
     */
    public static void extractFeatureValues( AnnotationFS ann, Map target, String pattern, String prefix ) {
        for( Feature feature : ann.getType().getFeatures() ) {
            if( !checkPattern( feature.getShortName(), pattern ) ) {
                continue;
            }
            if( feature.getRange().isPrimitive() ) {
                String key;
                if( prefix != null && !"".equals( prefix ) ) {
                    key = prefix + "_" + feature.getShortName();
                } else {
                    key = feature.getShortName();
                }
                target.put( key, ann.getFeatureValueAsString( feature ) );
            }
        }
    }

    private static boolean checkPattern( String check, String pattern ) {
        String inPrefix = "+|";
        String exPrefix = "-|";
        if( pattern.startsWith( inPrefix ) ) {
            return Pattern.compile( pattern.substring( inPrefix.length() ) ).matcher( check ).matches();
        } else {
            return !Pattern.compile( pattern.substring( exPrefix.length() ) ).matcher( check ).matches();
        }
    }
    
    /**
     * Comparator for UIMA annotations. Implements the standard UIMA sorting order based on 
     * beginning and ending offsets such that annotations are sorted by their location in the SOFA, 
     * with enclosing annotations first, i.e. if annotations have the same beginning offset, the 
     * one with the further ending offset will be sorted first.
     * 
     * This sorting order is consistent with the default iteration order in 
     * {@link org.apache.uima.cas.text.AnnotationIndex}
     * 
     * @return A {@link Comparator} that follows default UIMA sorting order.
     */
    public static Comparator<AnnotationFS> uimaSort() {
        return ( AnnotationFS o1, AnnotationFS o2 ) -> {
            int ret = Integer.compare( o1.getBegin(), o2.getBegin() );
            if( ret == 0 ) ret = Integer.compare( o2.getEnd(), o1.getEnd() );
            return ret;
        };
    }
}
