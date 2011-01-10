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

import org.switchyard.ExchangePattern;

public class InOnlyOperation implements ServiceOperation {
    
    public static final String INPUT_MESSAGE = "in";
    
    private String _methodName;
    private String _inputName;
    
    public InOnlyOperation(String methodName) {
        this(methodName, INPUT_MESSAGE);
    }
    
    public InOnlyOperation(String methodName, String inputName) {
        _methodName = methodName;
        _inputName = inputName;
    }

    @Override
    public ExchangePattern getExchangePattern() {
        return ExchangePattern.IN_ONLY;
    }

    @Override
    public String getInputMessage() {
        return _inputName;
    }

    @Override
    public String getName() {
        return _methodName; 
    }

    @Override
    public String getOutputMessage() {
        return null;
    }

}
