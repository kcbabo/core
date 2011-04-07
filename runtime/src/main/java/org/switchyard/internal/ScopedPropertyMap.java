/* 
 * JBoss, Home of Professional Open Source 
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved. 
 * See the copyright.txt in the distribution for a 
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use, 
 * modify, copy, or redistribute it subject to the terms and conditions 
 * of the GNU Lesser General Public License, v. 2.1. 
 * This program is distributed in the hope that it will be useful, but WITHOUT A 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details. 
 * You should have received a copy of the GNU Lesser General Public License, 
 * v.2.1 along with this distribution; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA  02110-1301, USA.
 */

package org.switchyard.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.switchyard.Property;
import org.switchyard.Scope;
import org.switchyard.io.Serialization.AccessType;
import org.switchyard.io.Serialization.Strategy;


@Strategy(access=AccessType.FIELD)
public class ScopedPropertyMap {
    private Map<Scope, Map<String, Property>> _props = 
        new HashMap<Scope, Map<String, Property>>();
    
    public ScopedPropertyMap() {
        for (Scope scope : Scope.values()) {
            _props.put(scope, new HashMap<String, Property>());
        }
    }
    
    public void put(Property property) {
        _props.get(property.getScope()).put(property.getName(), property);
    }
    
    public Property get(Scope scope, String name) {
        return _props.get(scope).get(name);
    }
    
    public Set<Property> get(Scope scope) {
        return new HashSet<Property>(_props.get(scope).values());
    }
    
    public Set<Property> get() {
        HashSet<Property> allProps = new HashSet<Property>();
        for (Scope scope : _props.keySet()) {
            allProps.addAll(get(scope));
        }
        return allProps;
    }
    
    public void remove(Property property) {
        _props.get(property.getScope()).remove(property.getName());
    }
    
    public void clear(Scope scope) {
        _props.get(scope).clear();
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        for (Map map : _props.values()) {
            map.clear();
        }
        _props.clear();
    }
}
