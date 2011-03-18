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

package org.switchyard.bus.hornetq;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.Message;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.MessageHandler;
import org.switchyard.Exchange;
import org.switchyard.ExchangePattern;
import org.switchyard.ExchangePhase;
import org.switchyard.ServiceReference;
import org.switchyard.handlers.HandlerChain;
import org.switchyard.metadata.ServiceOperation;
import org.switchyard.spi.Dispatcher;

public class HornetQDispatcher implements Dispatcher {

    private ServiceReference _service;
    private DispatchQueue _inQueue;
    private DispatchQueue _outQueue;
    private ClientSession _session;
    private HandlerChain _handlers;
    
    public HornetQDispatcher(ServiceReference service, 
            ClientSession session, 
            HandlerChain handlers) {
        _service = service;
        _session = session;
        
        // Create a queue for receiving input messages
        _inQueue = new DispatchQueue(session, 
                service.getName().toString() + ExchangePhase.IN,
                new InputHandler(_handlers));
        // Check to see if a queue is required for output messages based on operation MEPs
        for (ServiceOperation op : service.getInterface().getOperations()) {
            if (op.getExchangePattern().equals(ExchangePattern.IN_OUT)) {
                // Found at least one InOut, so we need a reply queue
                _outQueue = new DispatchQueue(session, 
                        service.getName().toString() + ExchangePhase.OUT,
                        new OutputHandler());
            }
        }
    }
    
    @Override
    public ServiceReference getService() {
        return _service;
    }

    @Override
    public void dispatch(Exchange exchange) {
        Message msg = _session.createMessage(false);  // NOT PERSISTENT
        msg.getBodyBuffer().writeString(exchange.getId());
        try {
            switch (exchange.getPhase()) {
            case IN:
                _inQueue.getProducer().send(msg);
                break;
            case OUT:
                _outQueue.getProducer().send(msg);
                break;
            }
        } catch (HornetQException hqEx) {
            throw new RuntimeException("Send to HornetQ endpoint failed", hqEx);
        }
    }
    
    @Override
    public void stop() {
        try { 
            if (_inQueue != null) {
                _inQueue.destroy();
            }
            if (_outQueue != null) {
                _outQueue.destroy();
            }
            _session.stop();
        } catch (HornetQException ex) {
            throw new RuntimeException("Failed to stop HornetQ endpoint " + _service.getName(), ex);
        }
    }
    
    @Override
    public void start() {
        try {
            if (_inQueue != null) {
                _inQueue.init();
            }
            if (_outQueue != null) {
                _outQueue.init();
            }
            _session.start();
        } catch (HornetQException ex) {
            throw new RuntimeException("Failed to start HornetQ endpoint " + _service.getName(), ex);
        }
    }
    
}

class InputHandler implements MessageHandler {

    private HandlerChain _handler;
    
    InputHandler(HandlerChain handler) {
        _handler = handler;
    }
    
    @Override
    public void onMessage(ClientMessage message) {
        System.out.println("Received exchange input " + message.getBodyBuffer().readString());
        // deserialize and hand off to handler chain
        //_handler.handle(exchange);
    }
     
}

class OutputHandler implements MessageHandler {

    @Override
    public void onMessage(ClientMessage message) {
        System.out.println("Received exchange output " + message.getBodyBuffer().readString());
    }
    
}

class DispatchQueue {
    
    private ClientSession _session;
    private String _name;
    private ClientConsumer _consumer;
    private ClientProducer _producer;
    private MessageHandler _handler;
    
    DispatchQueue(ClientSession session, String name, MessageHandler handler) {
        _session = session;
        _name = name;
        _handler = handler;
    }
    
    void init() throws HornetQException {
        _session.createQueue(_name, _name);
        _producer = _session.createProducer(_name);
        if (_handler != null) {
            _consumer = _session.createConsumer(_name);
            _consumer.setMessageHandler(_handler);
        }
    }
    
    void destroy() throws HornetQException {
        _consumer.close();
        _producer.close();
        _session.deleteQueue(_name);
    }
    
    String getName() {
        return _name;
    }
    
    ClientConsumer getConsumer() {
        return _consumer;
    }
    
    ClientProducer getProducer() {
        return _producer;
    }
}