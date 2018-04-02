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
package edu.columbia.incite.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class that implements an object that is capable of determining its fully qualified,
 * unambiguous XPath expression starting from a root node.
 * <br>
 * This class is designed to be used in SAX ContenHandlers to determine the origin and location of
 * parsing events in an agnostic manner.
 * <br>
 * The normal use of this class is to construct one root node and then add subsequent nodes to this
 * root or its descendants. See the source for
 * {@link edu.columbia.incite.util.xml.AbstractSaxHandler} for an example.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class XPathNode {

    private final String name;

    private final XPathNode parent;

    private final Multimap<String,XPathNode> childIndex;

    /**
     * Create a new XPath node with the given local name.
     *
     * @param name
     */
    public XPathNode( String name ) {
        this( name, null );
    }

    /**
     * Create a new XPath node with the given local name as child of the given parent XPath node.
     *
     * @param name
     * @param parent
     */
    public XPathNode( String name, XPathNode parent ) {
        this.name = name;
        this.parent = parent;
        this.childIndex = HashMultimap.create();
    }

    /**
     * Get the parent node of this node.
     *
     * @return The parent node for this node. {@code null} if this is a root node.
     */
    public XPathNode parent() {
        return this.parent;
    }

    /**
     * Get all children nodes of this node, mapped to their local names.
     *
     * @return A map containing localnames as keys, and all child nodes with a given name as values.
     */
    public Map<String,Collection<XPathNode>> getChildren() {
        return this.childIndex.asMap();
    }

    /**
     * Add a child node to this node with the given name.
     *
     * @param tag The local name of the child to be added to this node.
     *
     * @return A new child node with this node as parent.
     */
    public XPathNode addChild( String tag ) {
        int ct = 1;
        if( this.childIndex.containsKey( tag ) ) {
            ct += this.childIndex.get( tag ).size();
        }
        XPathNode child = new XPathNode( String.format( "%s[%d]", tag, ct ), this );
        this.childIndex.get( tag ).add( child );
        return child;
    }

    /**
     * Get this node's local name.
     *
     * @return The local name for this node.
     */
    public String getName() {
        return ( this.name );
    }

    /**
     * Get a valid XPath expression that will resolve to this node from the root node in this node's
     * ancestry chain.
     *
     * @return A string containing a valid XPath expression for this node.
     */
    public String getPath() {
        StringBuilder path = new StringBuilder();
        List<XPathNode> chain = new ArrayList<>();
        XPathNode cur = this;

        while( cur != null ) {
            chain.add( cur );
            cur = cur.parent();
        }

        Collections.reverse( chain );
        for( XPathNode i : chain ) {
            path.append( "/" );
            path.append( i.getName() );
        }

        return path.toString();
    }

}
