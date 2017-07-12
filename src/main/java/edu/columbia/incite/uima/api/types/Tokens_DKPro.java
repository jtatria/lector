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
package edu.columbia.incite.uima.api.types;

import java.util.Locale;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.cas.text.AnnotationFS;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Tokens_DKPro {

    public static String[] parse( AnnotationFS token ) {
        if( !isToken( token ) ) throw new IllegalArgumentException();
        Token t = (Token) token;
        String posg  = t.getPos().getType().getShortName();
        String port  = t.getPos().getPosValue();
        String lemma = t.getLemma().getValue().toLowerCase( Locale.ROOT );
        String raw   = t.getCoveredText().toLowerCase( Locale.ROOT );
        return new String[]{ posg, port, lemma, raw };
    }
    
    public static boolean isToken( AnnotationFS token ) {
        return token.getClass().isAssignableFrom( Token.class );
    }
    
}
