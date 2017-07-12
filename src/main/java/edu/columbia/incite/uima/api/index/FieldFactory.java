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
package edu.columbia.incite.uima.api.index;

import edu.columbia.incite.util.data.Datum;
import org.apache.lucene.document.Document;
import org.apache.uima.resource.Resource;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public interface FieldFactory extends Resource {

    public void updateFields( Document docInstance, Datum metadata );
    
}
