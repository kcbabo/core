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

package org.switchyard.internal.handlers;

import java.util.List;

import org.switchyard.Exchange;
import org.switchyard.ExchangeHandler;
import org.switchyard.HandlerException;
import org.switchyard.Service;
import org.switchyard.internal.ExchangeImpl;
import org.switchyard.internal.ServiceRegistration;
import org.switchyard.spi.ServiceRegistry;

/**
 * AddressingHandler is a handler which determines the
 * ServiceRegistration and the target endpoint to be used.
 */
public class AddressingHandler implements ExchangeHandler {

    private final ServiceRegistry _registry;

    /**
     * Constructor.
     * @param registry registry
     */
    public AddressingHandler(ServiceRegistry registry) {
        _registry = registry;
    }

    @Override
    public void handleFault(Exchange exchange) {
        // Nothing to do here
    }

    @Override
    public void handleMessage(Exchange exchange) throws HandlerException {
        // we need to mess with some internal exchange details - boo!
        ExchangeImpl ei = (ExchangeImpl) exchange;

        if (ei.getTarget() == null) {
            ei.setTarget(((ServiceRegistration)exchange.getService()).getEndpoint());
        }
    }

}
