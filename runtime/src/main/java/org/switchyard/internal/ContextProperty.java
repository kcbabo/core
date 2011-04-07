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

import java.io.IOException;

import org.switchyard.Property;
import org.switchyard.Scope;
import org.switchyard.internal.ContextProperty.ContextPropertyFactory;
import org.switchyard.io.Serialization.AccessType;
import org.switchyard.io.Serialization.Factory;
import org.switchyard.io.Serialization.Strategy;

@Strategy(access=AccessType.FIELD, factory=ContextPropertyFactory.class)
public class ContextProperty implements Property {
    
    private String _name;
    private Scope _scope;
    private Object _value;
    
    // Private ctor used for internal serialization only
    private ContextProperty() {
        
    }
    
    ContextProperty(String name, Scope scope, Object value) {
        if (name == null || scope == null) {
            throw new IllegalArgumentException("Property name and scope must not be null!");
        }
        
        _name = name;
        _scope = scope;
        _value = value;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Scope getScope() {
        return _scope;
    }

    @Override
    public Object getValue() {
        return _value;
    }
    
    public void setValue(Object value) {
        _value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ContextProperty)) {
            return false;
        }
        
        ContextProperty comp = (ContextProperty)obj;
        return _name.equals(comp.getName()) 
                && _scope.equals(comp.getScope())
                && (_value == null ? comp.getValue() == null : _value.equals(comp.getValue()));
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + _name.hashCode();
        hash = hash * 31 + _scope.hashCode();
        hash = hash * 31 + (_value != null ? _value.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return ("[name=" + _name + ", scope=" + _scope + ", value=" + _value + "]");
    }
    
    /**
     * The serialization factory for ContextProperty.
     */
    public static final class ContextPropertyFactory implements Factory<ContextProperty> {
        @Override
        public ContextProperty create(Class<ContextProperty> type) throws IOException {
            return new ContextProperty();
        }
    }
}
