/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.switchyard.metadata;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class JavaService extends BaseService {
    
    private Class<?> _serviceInterface;

    private JavaService(Set<ServiceOperation> operations, Class<?> serviceInterface) {
        super(operations);
        _serviceInterface = serviceInterface;
    }
    
    public static JavaService fromClass(Class<?> serviceInterface) {
        HashSet<ServiceOperation> ops = new HashSet<ServiceOperation>();
        for (Method m : serviceInterface.getDeclaredMethods()) {
            // We only consider public methods
            if (Modifier.isPublic(m.getModifiers())) {
                // At this point, we only accept methods with a single 
                // parameter which maps to the input message
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 1) {
                    throw new RuntimeException(
                            "Service operations on a Java interface must have exactly one parameter!");
                }
                // Create the appropriate service operation and add it to the list
                String inputName = serviceInterface.getCanonicalName() + 
                    m.getName() + params[0].getCanonicalName();
                if (m.getReturnType().equals(Void.TYPE)) {
                    ops.add(new InOnlyOperation(m.getName(), inputName));
                }
                else {
                    String outputName = serviceInterface.getCanonicalName() +
                        m.getName() + m.getReturnType().getCanonicalName();
                    ops.add(new InOutOperation(m.getName(), inputName, outputName));
                }
            }
        }
        
        return new JavaService(ops, serviceInterface);
    }
    
    public Class<?> getJavaInterface() {
        return _serviceInterface;
    }
}
