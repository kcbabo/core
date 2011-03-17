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

public class HornetQDispatcher implements Dispatcher, MessageHandler {

    private ServiceReference _service;
    private String _inQueueName;
    private String _outQueueName;
    private ClientSession _session;
    private ClientConsumer _consumer;
    private ClientProducer _producer;
    private HandlerChain _handlers;
    
    public HornetQDispatcher(ServiceReference service, 
            ClientSession session, 
            HandlerChain handlers) {
        _service = service;
        _session = session;
        _handlers = handlers;
        
        // Create a queue for receiving input messages
        _inQueueName = service.getName().toString() + ExchangePhase.IN;
        // Check to see if a queue is required for output messages based on operation MEPs
        for (ServiceOperation op : service.getInterface().getOperations()) {
            if (op.getExchangePattern().equals(ExchangePattern.IN_OUT)) {
                // Found at least one InOut, so we need a reply queue
                _outQueueName = service.getName().toString() + ExchangePhase.OUT;
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
            _producer.send(msg);
        } catch (HornetQException hqEx) {
            throw new RuntimeException("Send to HornetQ endpoint failed", hqEx);
        }
    }
    
    public void stop() {
        try {
            _consumer.close();
            _producer.close();
            _session.stop();
            _session.deleteQueue(_queueName);
        } catch (HornetQException ex) {
            throw new RuntimeException("Failed to stop HornetQ endpoint " + _service.getName(), ex);
        }
    }
    
    public void start() {
        try {
            _session.createQueue(_queueName, _queueName);
            _producer = _session.createProducer(_queueName);
            _consumer = _session.createConsumer(_queueName);
            _consumer.setMessageHandler(this);
            _session.start();
        } catch (HornetQException ex) {
            throw new RuntimeException("Failed to start HornetQ endpoint " + _service.getName(), ex);
        }
    }
    
    @Override
    public void onMessage(ClientMessage msg) {
        System.out.println("Received exchange " + msg.getBodyBuffer().readString());
    }

}

class DispatchQueue {
    
    DispatchQueue(ClientSession session, String name) {
        
    }
}