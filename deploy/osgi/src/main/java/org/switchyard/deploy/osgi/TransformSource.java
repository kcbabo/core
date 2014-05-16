package org.switchyard.deploy.osgi;

import java.net.URL;

/**
 *  Service registered by bundles which contain transformer definitions.  Used primarily
 *  by SwitchYard runtime components to indicate they have OOTB transformers that need 
 *  to be registered by the SwitchYard container.
 */
public interface TransformSource {

    String TRANSFORMS_XML = "META-INF/switchyard/transforms.xml";

    URL getTransformsURL();
}
