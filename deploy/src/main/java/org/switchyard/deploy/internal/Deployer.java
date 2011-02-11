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

package org.switchyard.deploy.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;
import org.switchyard.ExchangeHandler;
import org.switchyard.Service;
import org.switchyard.ServiceDomain;
import org.switchyard.config.model.ModelResource;
import org.switchyard.config.model.composite.BindingModel;
import org.switchyard.config.model.composite.ComponentModel;
import org.switchyard.config.model.composite.CompositeModel;
import org.switchyard.config.model.composite.ExternalServiceModel;
import org.switchyard.config.model.composite.InternalServiceModel;
import org.switchyard.config.model.composite.ReferenceModel;
import org.switchyard.deploy.BindingActivator;
import org.switchyard.deploy.ImplementationActivator;
import org.switchyard.internal.DefaultEndpointProvider;
import org.switchyard.internal.DefaultServiceRegistry;
import org.switchyard.internal.DomainImpl;
import org.switchyard.internal.transform.BaseTransformerRegistry;
import org.switchyard.spi.EndpointProvider;
import org.switchyard.spi.ServiceRegistry;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class Deployer {
    
    private static final String BEAN_ACTIVATOR_CLASS = 
        "org.switchyard.component.bean.deploy.BeanComponentActivator";

    /**
     * Root domain property.
     */
    public static final String ROOT_DOMAIN = "org.switchyard.domains.root";
    /**
     * Endpoint provider class name key.
     */
    public static final String ENDPOINT_PROVIDER_CLASS_NAME
        = "org.switchyard.endpoint.provider.class.name";
    /**
     * Registry class name property.
     */
    public static final String REGISTRY_CLASS_NAME
        = "org.switchyard.registry.class.name";

    private static final Logger LOG = Logger.getLogger(Deployer.class);

    private CompositeModel _switchyardConfig;
    private ServiceDomain _serviceDomain;
    private Map<String, ImplementationActivator> _implActivators = 
        new HashMap<String, ImplementationActivator>();
    private Map<String, BindingActivator> _bindingActivators = 
        new HashMap<String, BindingActivator>();

    public Deployer(InputStream switchyardConfig) throws IOException {
        _switchyardConfig = (CompositeModel)new ModelResource().pull(switchyardConfig);
    }

    public void deploy() {
        createDomain();
        createActivators();
        deployReferenceBindings();
        deployServices();
        deployReferences();
        deployServiceBindings();
    }

    public void undeploy() {
        undeployServiceBindings();
        undeployServices();
        undeployReferences();
        undeployReferenceBindings();
        destroyDomain();
    }

    private void createDomain() {
        String registryClassName = System.getProperty(REGISTRY_CLASS_NAME, DefaultServiceRegistry.class.getName());
        String endpointProviderClassName = System.getProperty(ENDPOINT_PROVIDER_CLASS_NAME, DefaultEndpointProvider.class.getName());

        try {
            ServiceRegistry registry = getRegistry(registryClassName);
            EndpointProvider endpointProvider = getEndpointProvider(endpointProviderClassName);
            BaseTransformerRegistry transformerRegistry = new BaseTransformerRegistry();

            _serviceDomain = new DomainImpl(ROOT_DOMAIN, registry, endpointProvider, transformerRegistry);
        } catch (NullPointerException npe) {
            throw new RuntimeException(npe);
        }

    }

    private void createActivators() {
        try {
            _implActivators.put("bean", (ImplementationActivator)Class.forName(BEAN_ACTIVATOR_CLASS).newInstance());
            _bindingActivators.put("soap", null);
        }
        catch (Exception ex) {
            throw new RuntimeException("Failed to load activator class for component", ex);
        }
    }

    private void deployReferenceBindings() {
        LOG.info("Deploying reference bindings ...");
       
    }

    private void deployServices() {
        LOG.info("Deploying services ...");
        // deploy services to each implementation found in the application
        for (ComponentModel component : _switchyardConfig.getComponents()) {
            ImplementationActivator act = _implActivators.get(
                    component.getImplementation().getType());
            // register a service for each one declared in the component
            for (InternalServiceModel service : component.getServices()) {
                LOG.info("Registering service " + service.getName() + 
                        " for component " + component.getImplementation().getType());
                ExchangeHandler handler = act.activateService(service.getQName(), service);
                _serviceDomain.registerService(service.getQName(), handler);
            }
        }
        
    }
    
    private void deployReferences() {
        LOG.info("Deploying references ...");
        for (ComponentModel component : _switchyardConfig.getComponents()) {
            ImplementationActivator act = _implActivators.get(
                    component.getImplementation().getType());
            // register a service for each one declared in the component
            for (ReferenceModel reference : component.getReferences()) {
                LOG.info("Registering reference " + reference.getName() + 
                        " for component " + component.getImplementation().getType());
                Service service = _serviceDomain.getService(reference.getQName());
                act.activateReference(reference.getQName(), service);
            }
        }
    }

    private void deployServiceBindings() {
        LOG.info("Deploying service bindings ...");
        // activate bindings for each service
        for (ExternalServiceModel service : _switchyardConfig.getServices()) {
            for (BindingModel binding : service.getBindings()) {
                LOG.info("Deploying binding " + binding.getType() + " for service " + service.getName());
                //BindingActivator act = _bindingActivators.get(binding.getType());
                //Service s = _serviceDomain.getService(service.getQName());
                //act.activateService(s, binding);
            }
        }
    }

    private void undeployServiceBindings() {
        LOG.info("Undeploying reference bindings ...");
    }

    private void undeployServices() {
        LOG.info("Undeploying services ...");
    }

    private void undeployReferences() {
        LOG.info("Undeploying references ...");
    }

    private void undeployReferenceBindings() {
        LOG.info("Undeploying reference bindings ...");
    }

    private void destroyDomain() {

    }

    /**
     * Returns an instance of the ServiceRegistry.
     * @param registryClass class name of the serviceregistry
     * @return ServiceRegistry
     */
    private static ServiceRegistry getRegistry(final String registryClass) {
        ServiceLoader<ServiceRegistry> registryServices
                = ServiceLoader.load(ServiceRegistry.class);
        for (ServiceRegistry serviceRegistry : registryServices) {
            if (registryClass.equals(serviceRegistry.getClass().getName())) {
                return serviceRegistry;
            }
        }
        return null;
    }


    /**
     * Returns an instance of the EndpointProvider.
     * @param providerClass class name of the endpointprovider implementation
     * @return EndpointProvider
     */
    private static EndpointProvider
    getEndpointProvider(final String providerClass) {
        ServiceLoader<EndpointProvider> providerServices
                = ServiceLoader.load(EndpointProvider.class);
        for (EndpointProvider provider : providerServices) {
            if (providerClass.equals(provider.getClass().getName())) {
                return provider;
            }
        }
        return null;
    }
}

