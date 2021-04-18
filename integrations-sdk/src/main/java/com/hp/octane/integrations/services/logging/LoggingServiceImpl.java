/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
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
 */

package com.hp.octane.integrations.services.logging;

import com.hp.octane.integrations.OctaneSDK;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Service for management logging capabilities of the plugin (SDK); currently meant for the internal usage only
 */

final class LoggingServiceImpl implements LoggingService {
    private static final Logger logger = LogManager.getLogger(LoggingServiceImpl.class);


    private final OctaneSDK.SDKServicesConfigurer configurer;
    private boolean isShutdown;

    private final LoggerContext commonLoggerContext;

    LoggingServiceImpl(OctaneSDK.SDKServicesConfigurer configurer) {
        if (configurer == null) {
            throw new IllegalArgumentException("invalid configurer");
        }
        this.configurer = configurer;
        commonLoggerContext = CommonLoggerContextUtil.configureLogger(configurer.pluginServices.getAllowedOctaneStorage());
        logger.info(configurer.octaneConfiguration.geLocationForLog() + "logger is configured");
    }

    @Override
    public void shutdown() {
        if (OctaneSDK.getClients().isEmpty() && commonLoggerContext != null) {
            logger.info(configurer.octaneConfiguration.geLocationForLog() + "last client is closing; general logger context is STOPPING");
            commonLoggerContext.stop();
            isShutdown = true;
        }
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }


}
