/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.services.logging;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class CommonLoggerContextUtil {

    private static final Object INIT_LOCKER = new Object();
    private static LoggerContext commonLoggerContext;
    private static final String OCTANE_ALLOWED_STORAGE_LOCATION = "octaneAllowedStorage";


    public static LoggerContext configureLogger(File allowedStorage) {
        if (allowedStorage != null && (allowedStorage.isDirectory() || !allowedStorage.exists())) {
            synchronized (INIT_LOCKER) {
                if (commonLoggerContext != null) {
                    if (!commonLoggerContext.isStarted()) {
                        commonLoggerContext.start();
                    }
                    return commonLoggerContext;
                }
                commonLoggerContext = LoggerContext.getContext(false);

                if (!commonLoggerContext.isStarted()) {
                    commonLoggerContext.start();
                }
                System.setProperty(OCTANE_ALLOWED_STORAGE_LOCATION, allowedStorage.getAbsolutePath() + File.separator);

                //case for team city, that cannot find log4j
                if (commonLoggerContext.getConfiguration() == null ||
                        !(commonLoggerContext.getConfiguration() instanceof XmlConfiguration)) {

                    URL path = LoggingServiceImpl.class.getClassLoader().getResource("log4j2.xml");
                    if (path != null) {
                        try {
                            commonLoggerContext.setConfigLocation(path.toURI());
                            //reconfigure is called inside
                        } catch (URISyntaxException e) {
                            //failed to convert to URI
                        }
                    }
                }

                commonLoggerContext.reconfigure();
            }
        }
        return commonLoggerContext;
    }
}
