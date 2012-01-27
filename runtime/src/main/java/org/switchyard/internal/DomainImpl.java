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

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.switchyard.BaseHandler;
import org.switchyard.Exchange;
import org.switchyard.ExchangeHandler;
import org.switchyard.HandlerChain;
import org.switchyard.Service;
import org.switchyard.ServiceDomain;
import org.switchyard.ServiceReference;
import org.switchyard.exception.SwitchYardException;
import org.switchyard.handlers.PolicyHandler;
import org.switchyard.handlers.TransactionHandler;
import org.switchyard.handlers.TransformHandler;
import org.switchyard.handlers.ValidateHandler;
import org.switchyard.metadata.BaseExchangeContract;
import org.switchyard.metadata.ExchangeContract;
import org.switchyard.metadata.InOutService;
import org.switchyard.metadata.ServiceInterface;
import org.switchyard.metadata.ServiceOperation;
import org.switchyard.policy.Policy;
import org.switchyard.spi.Dispatcher;
import org.switchyard.spi.ExchangeBus;
import org.switchyard.spi.ServiceRegistry;
import org.switchyard.transform.TransformerRegistry;
import org.switchyard.validate.ValidatorRegistry;

/**
 * Implementation of ServiceDomain.
 */
public class DomainImpl implements ServiceDomain {

    private static Logger _logger = Logger.getLogger(DomainImpl.class);

    private final QName _name;
    private final DefaultHandlerChain _defaultHandlers;
    private final ServiceRegistry _registry;
    private final ExchangeBus _exchangeBus;
    private final TransformerRegistry _transformerRegistry;
    private final ValidatorRegistry _validatorRegistry;

    /**
     * Constructor.
     * @param name name
     * @param registry registry
     * @param exchangeBus message exchange bus
     * @param transformerRegistry transformerRegistry
     * @param validatorRegistry validatorRegistry
     */
    public DomainImpl(QName name,
            ServiceRegistry registry,
            ExchangeBus exchangeBus,
            TransformerRegistry transformerRegistry,
            ValidatorRegistry validatorRegistry) {

        _name = name;
        _registry = registry;
        _exchangeBus  = exchangeBus;
        _transformerRegistry = transformerRegistry;
        _validatorRegistry = validatorRegistry;

        // Build out the system handlers chain.  A null "provider" handler
        // is inserted as a placeholder to establish the correct position of
        // the service provider within the chain.
        TransactionHandler transactionHandler = new TransactionHandler();
        ValidateHandler validateHandler = new ValidateHandler(_validatorRegistry);
        _defaultHandlers = new DefaultHandlerChain();
        _defaultHandlers.addLast("transaction-pre-invoke", transactionHandler);
        _defaultHandlers.addLast("generic-policy", new PolicyHandler());
        _defaultHandlers.addLast("validation-before-transform", validateHandler);
        _defaultHandlers.addLast("transformation", new TransformHandler(_transformerRegistry));
        _defaultHandlers.addLast("validation-after-transform", validateHandler);
        _defaultHandlers.addLast(HandlerChain.PROVIDER_HANDLER, new BaseHandler());
        _defaultHandlers.addLast("transaction-post-invoke", transactionHandler);

        if (_logger.isDebugEnabled()) {
            _logger.debug("Created SwitchYard ServiceDomain instance '" + name + "'.");
        }
    }
    
    @Override
    public Service registerService(QName serviceName, ServiceInterface metadata, 
            ExchangeHandler handler) {
        List<Policy> requires = Collections.emptyList();
        return registerService(serviceName, metadata, handler, requires);
    }

    @Override
    public Service registerService(QName serviceName,
            ServiceInterface metadata, ExchangeHandler handler,
            List<Policy> requires) {
        
        // If no service interface is provided, we default to InOutService
        if (metadata == null) {
            metadata = new InOutService();
        }
        // Create the service 
        Service service = new ServiceImpl(serviceName, metadata, requires, this);
        // Add a handler chain with the provider at the end
        HandlerChain handlers = _defaultHandlers.copy();
        handlers.replace(HandlerChain.PROVIDER_HANDLER, handler);
        Dispatcher ep = _exchangeBus.createDispatcher(service, handlers, _transformerRegistry);
        
        // register the service
        return _registry.registerService(service, ep, this);
    }

    /* (non-Javadoc)
     * @see org.switchyard.ServiceDomain#creaetServiceReference(javax.xml.namespace.QName, org.switchyard.metadata.ServiceInterface)
     */
    @Override
    public ServiceReference createServiceReference(QName serviceName,
            ServiceInterface metadata) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.switchyard.ServiceDomain#createServiceReference(javax.xml.namespace.QName, org.switchyard.metadata.ServiceInterface, org.switchyard.ExchangeHandler)
     */
    @Override
    public ServiceReference createServiceReference(QName serviceName,
            ServiceInterface metadata, ExchangeHandler handler) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.switchyard.ServiceDomain#wireReference(org.switchyard.ServiceReference, org.switchyard.Service)
     */
    @Override
    public void wireReference(ServiceReference reference,
            org.switchyard.Service service) {
        // TODO Auto-generated method stub
        
    }


    public Exchange createExchange(ServiceReference service, ServiceOperation operation) {
        return createExchange(service, operation, null);
    }

    public Exchange createExchange(
            ServiceReference service, ServiceOperation operation, ExchangeHandler handler) {
        // TODO : This assumes an exact match between service reference name and service name.  
        // We need to look for service wiring that might connect different names.
        List<Service> services = _registry.getServices(service.getName());
        
        if (services == null || services.isEmpty()) {
            throw new SwitchYardException("No registered service found for " + service.getName());
        }

        // At this stage, just pick the first service implementation we find and go with
        // it.  In the future, it would be nice if we could make this pluggable.
        Service targetService = services.get(0);
        Dispatcher dispatcher = _exchangeBus.getDispatcher(targetService);
        HandlerChain replyChain = null;
        
        if (handler != null) {
            replyChain = _defaultHandlers.copy();
            replyChain.addLast("replyHandler", handler);
        }

        ExchangeContract contract = new BaseExchangeContract(
                targetService.getInterface().getOperation(operation.getName()), operation);
        // create the exchange
        ExchangeImpl exchange = new ExchangeImpl(service.getName(), contract,
                dispatcher, _transformerRegistry, replyChain);
        return exchange;
    }
    
    @Override
    public QName getName() {
        return _name;
    }

    @Override
    public TransformerRegistry getTransformerRegistry() {
        return _transformerRegistry;
    }
    
    @Override
    public ValidatorRegistry getValidatorRegistry() {
        return _validatorRegistry;
    }
    
    public ServiceRegistry getServiceRegistry() {
        return _registry;
    }
    
    public ExchangeBus getBus() {
        return _exchangeBus;
    }

    @Override
    public HandlerChain getHandlerChain() {
        return _defaultHandlers;
    }

}
