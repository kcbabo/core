/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.switchyard.config.model;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Scanner} that merges all {@link Model}s from other Scanners into one.
 *
 * @param <M> the Model type to scan for (and merge)
 *
 * @author David Ward &lt;<a href="mailto:dward@jboss.org">dward@jboss.org</a>&gt; (C) 2011 Red Hat Inc.
 */
public class MergeScanner<M extends Model> implements Scanner<M> {

    private M _model;
    private final boolean _fromOverridesTo;
    private final List<Scanner<M>> _scanners;

    /**
     * Constructs a new MergeScanner using the specified parameters.
     * @param model instance of the model to be merged into
     * @param fromOverridesTo whether or not each successfully merged Model's values will override the next Model to merge values
     * @param scanners the Scanners to merge output from
     */
    public MergeScanner(M model, boolean fromOverridesTo, Scanner<M>... scanners) {
        _model = model;
        _fromOverridesTo = fromOverridesTo;
        List<Scanner<M>> list = new ArrayList<Scanner<M>>();
        if (scanners != null) {
            for (Scanner<M> scanner : scanners) {
                if (scanner != null) {
                    list.add(scanner);
                }
            }
        }
        _scanners = list;
    }

    /**
     * Constructs a new MergeScanner using the specified parameters.
     * @param model instance of the model to be merged into
     * @param fromOverridesTo whether or not each successfully merged Model's values will override the next Model to merge values
     * @param scanners the Scanners to merge output from
     */
    public MergeScanner(M model, boolean fromOverridesTo, List<Scanner<M>> scanners) {
        _model = model;
        _fromOverridesTo = fromOverridesTo;
        _scanners = new ArrayList<Scanner<M>>();
        if (scanners != null) {
            for (Scanner<M> scanner : scanners) {
                if (scanner != null) {
                    _scanners.add(scanner);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScannerOutput<M> scan(ScannerInput<M> input) throws IOException {
        
        for (Scanner<M> scanner : _scanners) {
            try {
                Method setNamespace = scanner.getClass().getMethod("setNamespace", String.class);
                if (setNamespace != null) {
                    setNamespace.invoke(scanner, _model.getModelConfiguration().getQName().getNamespaceURI());
                }
            } catch (Exception ex) {
                System.err.println("Failed to set namespace on scanner: " + ex);
            }
            ScannerOutput<M> scannerOutput = scanner.scan(input);
            if (scannerOutput != null) {
                List<M> scanned_list = scannerOutput.getModels();
                if (scanned_list != null) {
                    for (M scanned : scanned_list) {
                        if (scanned != null) {
                            _model = Models.merge(scanned, _model, _fromOverridesTo);
                        }
                    }
                }
            }
        }
        return new ScannerOutput<M>().setModel(_model);
    }

}
