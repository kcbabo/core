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

package org.switchyard.deploy.cdi;

import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;

import org.switchyard.deploy.Deployer;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@ApplicationScoped
public class SwitchYardCDIBootstrapper implements Extension {

    private Deployer deployer;

    /**
     * {@link AfterDeploymentValidation} CDI event observer.
     *
     * @param event         CDI Event instance.
     */
    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        InputStream swConfigStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/switchyard.xml");

        if(swConfigStream != null) {
            try {
                deployer = new Deployer(swConfigStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            deployer.deploy();
        }
    }

    /**
     * {@link BeforeShutdown} CDI event observer.
     *
     * @param event       CDI Event instance.
     */
    public void beforeShutdown(@Observes BeforeShutdown event) {
        if(deployer != null) {
            deployer.undeploy();
        }
    }
}
