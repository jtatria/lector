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

import org.apache.uima.resource.Resource;

/**
 * A ConfigurableResource encapsulates a shared object that can modify its 
 * behaviour to respond to processing events.
 * This resource can be used to e.g. configure access to a CAS's type system, 
 * set up pathnames for data output, etc.
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 * @param <C> Type for configuration data.
 */
public interface ConfigurableResource<C> extends Resource {

    /**
     * Passes configuration data to this resource.
     *
     * @param conf An instance of C containing new configuration data.
     *
     * @throws Exception If configuration fails for any reason.
     */
    void configure( C conf ) throws Exception;
}
