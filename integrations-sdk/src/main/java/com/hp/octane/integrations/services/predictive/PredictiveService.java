/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.integrations.services.predictive;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Created by benmeior on 2/1/2017.
 */
public final class PredictiveService extends OctaneSDK.SDKServiceBase {
    private static Logger logger = LogManager.getLogger(PredictiveService.class);

    private static final Object INIT_LOCKER = new Object();
    private static final String PEM_FILE_SYS_PARAM = "pem_file";
    private static final String PREDICTIVE_PEM_FILE_NAME = "predictive.pem";

    private final CIPluginServices pluginServices;

    public PredictiveService(Object configurator, CIPluginServices pluginServices) {
        super(configurator);

        if (pluginServices == null) {
            throw new IllegalArgumentException("plugin services MUST NOT be null");
        }

        this.pluginServices = pluginServices;
        configurePredictivePemFile();
    }

    private void configurePredictivePemFile() {
        File predictiveOctanePath = pluginServices.getPredictiveOctanePath();
        if (predictiveOctanePath != null) {
            synchronized (INIT_LOCKER) {
                String pemFilePath = predictiveOctanePath.getAbsolutePath() + File.separator + PREDICTIVE_PEM_FILE_NAME;
                System.setProperty(PEM_FILE_SYS_PARAM, pemFilePath);
                logger.info("Added path to predictive folder. Path: " + pemFilePath);
            }
        }
    }
}
