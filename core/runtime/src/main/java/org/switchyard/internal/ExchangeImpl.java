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

package org.switchyard.internal;

import java.util.HashMap;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.switchyard.Context;
import org.switchyard.Exchange;
import org.switchyard.ExchangePattern;
import org.switchyard.HandlerChain;
import org.switchyard.Message;
import org.switchyard.Scope;
import org.switchyard.Service;
import org.switchyard.spi.Endpoint;

/**
 * Implementation of Exchange.
 */
public class ExchangeImpl implements Exchange {

    /**
     * In message.
     */
    public static final String IN_MSG       = "in";
    /**
     * Out message.
     */
    public static final String OUT_MSG      = "out";
    /**
     * Fault message.
     */
    public static final String FAULT_MSG    = "fault";

    private String          _exchangeId;
    private ExchangePattern _pattern;
    private Service         _service;
    private Message         _message;
    private final HandlerChain    _handlers;
    private Endpoint        _source;
    private Endpoint        _target;
    private final HashMap<Scope, Context> _context =
        new HashMap<Scope, Context>();

    /**
     * Constructor.
     * @param service service
     * @param pattern exchange pattern
     * @param handlers handlers
     */
    ExchangeImpl(Service service, ExchangePattern pattern, HandlerChain handlers) {
        _service = service;
        _pattern = pattern;
        _handlers = handlers;
        _exchangeId = UUID.randomUUID().toString();
        initContext();
    }

    @Override
    public Context getContext() {
        return _context.get(Scope.EXCHANGE);
    }

    @Override
    public Context createContext() {
        return new BaseContext();
    }

    @Override
    public Context getContext(Scope scope) {
        return _context.get(scope);
    }

    @Override
    public ExchangePattern getPattern() {
        return _pattern;
    }

    @Override
    public Service getService() {
        return _service;
    }

    @Override
    public String getId() {
        return _exchangeId;
    }

    @Override
    public Message getMessage() {
        return _message;
    }

    @Override
    public void send(Message message, Context messageContext) {
        setContext(Scope.MESSAGE, messageContext);

        _message = message;
        _handlers.handle(this);
    }

    @Override
    public void send(Message message) {
        send(message, new BaseContext());
    }

    /**
     * Get source endpoint.
     * @return source
     */
    public Endpoint getSource() {
        return _source;
    }

    /**
     * Get target endpoint.
     * @return target
     */
    public Endpoint getTarget() {
        return _target;
    }

    /**
     * Set the target endpoint.
     * @param target target endpoint
     */
    public void setTarget(Endpoint target) {
        _target = target;
    }

    /**
     * Set the source endpoint.
     * @param source source endpoint
     */
    public void setSource(Endpoint source) {
        _source = source;
    }

    /**
     * Set the context for a particular scope.
     * @param scope scope
     * @param context context
     */
    private void setContext(Scope scope, Context context) {
        _context.put(scope, context);
    }

    /**
     * Builds the context layers for this exchange
     */
    private void initContext() {
        for (Scope scope : Scope.values()) {
            _context.put(scope, new BaseContext());
        }
    }
}
