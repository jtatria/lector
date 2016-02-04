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

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.Resource;

/**
 * A shared resource that provides unique ids.
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <I> Type for provided ids, typically Long.
 */
public interface IdProvider<I> extends Resource {
    
    /**
     * Get a new id, guaranteed to be unique within the lifetime of this resource.
     * @param ann
     * @return
     */
    I getId( AnnotationFS ann );
}
