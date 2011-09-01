package org.switchyard.deploy;

import org.switchyard.ServiceDomain;
import org.switchyard.config.Configuration;

public interface Component {
    Activator getActivator(ServiceDomain domain);
    String getName();
    void init(Configuration config);
    void destroy();
}
