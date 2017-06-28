/* 
 * Copyright (C) 2015 José Tomás Atria <ja2612@columbia.edu>
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
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public abstract class Annotations {

    public static void extractFeatureValues( AnnotationFS ann, Map target ) {
        extractFeatureValues( ann, target, null, null );
    }

    public static void extractFeatureValues( AnnotationFS ann, Map target, String pattern ) {
        extractFeatureValues( ann, target, pattern, null );
    }

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
    
    public static Comparator<AnnotationFS> uimaSort() {
        return ( AnnotationFS o1, AnnotationFS o2 ) -> {
            int ret = Integer.compare( o1.getBegin(), o2.getBegin() );
            if( ret == 0 ) ret = Integer.compare( o2.getEnd(), o1.getEnd() );
            return ret;
        };
    }
}
