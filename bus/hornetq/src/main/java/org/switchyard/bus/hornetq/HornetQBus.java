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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQServers;
import org.hornetq.core.server.JournalType;
import org.switchyard.ServiceReference;
import org.switchyard.handlers.HandlerChain;
import org.switchyard.spi.Dispatcher;
import org.switchyard.spi.ExchangeBus;

public class HornetQBus implements ExchangeBus {
    
    public static final String WORK_DIR = "org.switchyard.bus.hornetq.WorkDir";

    private HornetQServer _server;
    private ClientSessionFactory _clientFactory;
    private HashMap<QName, HornetQDispatcher> _dispatchers = 
        new HashMap<QName, HornetQDispatcher>();
    
    public HornetQBus() {
    }
    
    public void init(Map<String, Object> busConfig) {
        // Create the server
        _server = HornetQServers.newHornetQServer(getHornetQConfig(busConfig));
    }
    
    public synchronized void start() {
        try {
            _server.start();
            _clientFactory = HornetQClient.createClientSessionFactory(
                    new TransportConfiguration(InVMConnectorFactory.class.getName()));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to start HornetQProvider", ex);
        }
    }
    
    public synchronized void stop() {
        try {
            for (HornetQDispatcher ep : _dispatchers.values()) {
                ep.stop();
            }
            _server.stop();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to stop HornetQProvider", ex);
        }
    }

    @Override
    public Dispatcher getDispatcher(ServiceReference service) {
        return _dispatchers.get(service.getName());
    }

    @Override
    public synchronized Dispatcher createDispatcher(
            ServiceReference service, HandlerChain handlerChain) {
        
        try {
            ClientSession session = _clientFactory.createSession();
            HornetQDispatcher endpoint = new HornetQDispatcher(service, session, handlerChain);
            _dispatchers.put(service.getName(), endpoint);
            endpoint.start();
            return endpoint;
        } catch (HornetQException hqEx) {
            throw new RuntimeException("Failed to create HornetQ endpoint", hqEx);
        }
    }
    
    HornetQServer getHornetQServer() {
        return _server;
    }
    
    Configuration getHornetQConfig(Map<String, Object> providerConfig) {
        // Generate the HornetQ configuration
        Configuration config = new ConfigurationImpl();
        config.setSecurityEnabled(false);
        config.setJournalType(JournalType.NIO);
        config.getAcceptorConfigurations().add(
                new TransportConfiguration(InVMAcceptorFactory.class.getName()));
        
        // Journal directory comes from provider configuration
        String workDir = (String)providerConfig.get(WORK_DIR);
        if (workDir != null) {
            config.setJournalDirectory(workDir);
            config.setBindingsDirectory(workDir);
            config.setLargeMessagesDirectory(workDir);
        }
        return config;
    }


}
