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
package edu.columbia.incite.uima.api;

import org.apache.uima.resource.Resource;

/**
 * A SessionResource provides means to control access to shared resources and maintain per-component
 * processing data.
 * @author José Tomás Atria <jtatria@gmail.com>
 * @param <S> Type for an object that provides a component with access to its session data.
 */
public interface SessionResource<S> extends Resource {

    /**
     * Open a new session and return an instance of S that provides access to session data.
     * This interface's contract requires this method to be called before a component can access
     * session resources.
     * @return An instance of S that provides a component with access to session data.
     */
    S openSession();

    /**
     * Close the given session and release all held resources.
     * This interface's contract expects no further uses of any resources associated to the given
     * session.
     * @param session An instance of S created by {@link openSession()}.
     */
    void closeSession( S session );
}
