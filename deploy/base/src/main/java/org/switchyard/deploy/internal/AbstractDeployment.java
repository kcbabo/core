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

package org.switchyard.deploy.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.xml.namespace.QName;

import org.switchyard.ServiceDomain;
import org.switchyard.config.model.switchyard.SwitchYardModel;
import org.switchyard.internal.DefaultServiceRegistry;
import org.switchyard.internal.DomainImpl;
import org.switchyard.internal.LocalExchangeBus;
import org.switchyard.internal.transform.BaseTransformerRegistry;
import org.switchyard.spi.ExchangeBus;
import org.switchyard.spi.ServiceRegistry;
import org.switchyard.transform.TransformerRegistryLoader;

/**
 * Abstract SwitchYard application deployment.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class AbstractDeployment {
    /**
     * Default classpath location for the switchyard configuration.
     */
    public static final String SWITCHYARD_XML = "/META-INF/switchyard.xml";
    /**
     * Root domain property.
     */
    public static final QName ROOT_DOMAIN = new QName("org.switchyard.domains.root");
    /**
     * Exchange Bus provider class name key.
     */
    public static final String BUS_CLASS_NAME = "org.switchyard.spi.ExchangeBus";
    /**
     * Registry class name property.
     */
    public static final String REGISTRY_CLASS_NAME = "org.switchyard.spi.ServiceRegistry";

    /**
     * Parent deployment.
     */
    private AbstractDeployment _parentDeployment;
    /**
     * The Service Domain.
     */
    private ServiceDomain _serviceDomain;
    /**
     * TransformerRegistry Loader class.
     */
    private TransformerRegistryLoader _transformerRegistryLoader;
    /**
     * SwitchYard configuration.
     */
    private SwitchYardModel _switchyardConfig;
    
    /**
     * Create a new instance of a deployment from a configuration model.
     * @param configModel switchyard config model
     */
    protected AbstractDeployment(SwitchYardModel configModel) {
        _switchyardConfig = configModel;
    }

    /**
     * Set the parent deployment.
     * <p/>
     * This must be called before calling {@link #init()}.
     * @param parentDeployment The parent deployment.
     */
    public void setParentDeployment(AbstractDeployment parentDeployment) {
        this._parentDeployment = parentDeployment;
    }

    /**
     * Initialise the deployment.
     */
    public void init() {
        if (_parentDeployment == null) {
            createDomain();
        }
    }

    /**
     * Start/un-pause the deployment.
     */
    public abstract void start();

    /**
     * Stop/pause the deployment.
     */
    public abstract void stop();

    /**
     * Destroy the deployment.
     */
    public abstract void destroy();

    /**
     * Get the {@link ServiceDomain} associated with the deployment.
     * @return The domain instance.
     */
    public ServiceDomain getDomain() {
        if (_parentDeployment == null) {
            return _serviceDomain;
        } else {
            return _parentDeployment.getDomain();
        }
    }

    /**
     * Get the {@link TransformerRegistryLoader} associated with the deployment.
     * @return The TransformerRegistryLoader instance.
     */
    public TransformerRegistryLoader getTransformerRegistryLoader() {
        return _transformerRegistryLoader;
    }
    
    protected SwitchYardModel getConfig() {
        return _switchyardConfig;
    }

    private void createDomain() {
        // Initialize defaults
        String registryClassName = DefaultServiceRegistry.class.getName();
        String busClassName = LocalExchangeBus.class.getName();
        Map<String, String> domainProps = new HashMap<String, String>();
        
        // Use domain configuration when creating registry and bus providers.
        // This is really a temporary measure as a domain should support multiple
        // bus and registry providers.
        if (_switchyardConfig != null && _switchyardConfig.getDomain() != null) {
            if (_switchyardConfig.getDomain().getProperty(REGISTRY_CLASS_NAME) != null) {
                 registryClassName = _switchyardConfig.getDomain().getProperty(REGISTRY_CLASS_NAME);
            }
            if (_switchyardConfig.getDomain().getProperty(BUS_CLASS_NAME) != null) {
                busClassName = _switchyardConfig.getDomain().getProperty(BUS_CLASS_NAME);
            }
            domainProps = _switchyardConfig.getDomain().getProperties();
        }

        try {
            ServiceRegistry registry = getRegistry(registryClassName);
            ExchangeBus exchangeBus = getExchangeBus(busClassName);
            exchangeBus.init(_serviceDomain, domainProps);
            BaseTransformerRegistry transformerRegistry = new BaseTransformerRegistry();

            _serviceDomain = new DomainImpl(ROOT_DOMAIN, registry, exchangeBus, transformerRegistry);

            _transformerRegistryLoader = new TransformerRegistryLoader(transformerRegistry);
            _transformerRegistryLoader.loadOOTBTransforms();
        } catch (NullPointerException npe) {
            throw new RuntimeException(npe);
        }

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
     * Returns an instance of ExchangeBus.
     * @param providerClass class name of the ExchangeBus implementation
     * @return EndpointProvider
     */
    private static ExchangeBus getExchangeBus(final String providerClass) {
        ServiceLoader<ExchangeBus> providerServices
                = ServiceLoader.load(ExchangeBus.class);
        
        for (ExchangeBus provider : providerServices) {
            if (providerClass.equals(provider.getClass().getName())) {
                return provider;
            }
        }
        return null;
    }
}
