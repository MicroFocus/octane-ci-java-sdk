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
 *
 */

package com.hp.octane.integrations;

import com.hp.octane.integrations.api.*;
import com.hp.octane.integrations.services.bridge.BridgeServiceImpl;
import com.hp.octane.integrations.services.configuration.ConfigurationServiceImpl;
import com.hp.octane.integrations.services.coverage.SonarServiceImpl;
import com.hp.octane.integrations.services.entities.EntitiesServiceImpl;
import com.hp.octane.integrations.services.events.EventsServiceImpl;
import com.hp.octane.integrations.services.logging.LoggingServiceImpl;
import com.hp.octane.integrations.services.logs.LogsServiceImpl;
import com.hp.octane.integrations.services.queue.QueueService;
import com.hp.octane.integrations.services.queue.QueueServiceImpl;
import com.hp.octane.integrations.services.rest.RestServiceImpl;
import com.hp.octane.integrations.services.tasking.TasksProcessorImpl;
import com.hp.octane.integrations.services.tests.TestsServiceImpl;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesServiceImpl;
import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

/**
 * This class provides main entry point of interaction between SDK and it's services,
 * and interaction between concrete plugin and it's services.
 */

public final class OctaneSDK {
    private static final Logger logger = LogManager.getLogger(OctaneSDK.class);
    private static volatile OctaneSDK instance;

    public static Integer API_VERSION;
    public static String SDK_VERSION;

    private final Object INTERNAL_USAGE_VALIDATOR = new Object();
    private final CIPluginServices pluginServices;
    private final QueueService queueService;
    private final RestService restService;
    private final ConfigurationService configurationService;
    private final TasksProcessor tasksProcessor;


    private final SonarService sonarService;
    private final EventsService eventsService;
    private final TestsService testsService;
    private final LogsService logsService;
    private final VulnerabilitiesService vulnerabilitiesService;
    private final EntitiesService entitiesService;

    private OctaneSDK(CIPluginServices ciPluginServices) {
        instance = this;
        initSDKProperties();
        pluginServices = ciPluginServices;
        new LoggingServiceImpl(INTERNAL_USAGE_VALIDATOR);
        queueService = new QueueServiceImpl(INTERNAL_USAGE_VALIDATOR);
        restService = new RestServiceImpl(INTERNAL_USAGE_VALIDATOR);
        tasksProcessor = new TasksProcessorImpl(INTERNAL_USAGE_VALIDATOR);
        configurationService = new ConfigurationServiceImpl(INTERNAL_USAGE_VALIDATOR, restService);
        eventsService = new EventsServiceImpl(INTERNAL_USAGE_VALIDATOR, restService);
        testsService = new TestsServiceImpl(INTERNAL_USAGE_VALIDATOR, queueService, restService);
        logsService = new LogsServiceImpl(INTERNAL_USAGE_VALIDATOR, queueService, restService);
        vulnerabilitiesService = new VulnerabilitiesServiceImpl(INTERNAL_USAGE_VALIDATOR, restService);
        entitiesService = new EntitiesServiceImpl(INTERNAL_USAGE_VALIDATOR, restService);
        sonarService = new SonarServiceImpl(INTERNAL_USAGE_VALIDATOR,queueService, restService);
        new BridgeServiceImpl(INTERNAL_USAGE_VALIDATOR, restService, tasksProcessor);
    }

    /**
     * To start using the CI Plugin SDK, first initialize an OctaneSDK instance.
     *
     * @param ciPluginServices Object that implements the CIPluginServices interface. This object is actually a composite
     *                         API of all the endpoints to be implemented by a hosting CI Plugin for ALM Octane use cases.
     */
    synchronized public static void init(CIPluginServices ciPluginServices) {
        if (instance == null) {
            if (ciPluginServices == null) {
                throw new IllegalArgumentException("initialization failed: MUST be initialized with valid plugin services provider");
            }
            new OctaneSDK(ciPluginServices);
            logger.info("initialized SUCCESSFULLY");
        } else {
            logger.error("SDK may be initialized only once, secondary initialization attempt encountered");
        }
    }

    public static OctaneSDK getInstance() {
        if (instance != null) {
            return instance;
        } else {
            throw new IllegalStateException("SDK MUST be initialized prior to any usage");
        }
    }

    public CIPluginServices getPluginServices() {
        return pluginServices;
    }

    public RestService getRestService() {
        return restService;
    }

    public SonarService getSonarService() {
        return sonarService;
    }

    public TasksProcessor getTasksProcessor() {
        return tasksProcessor;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public EventsService getEventsService() {
        return eventsService;
    }

    public TestsService getTestsService() {
        return testsService;
    }

    public LogsService getLogsService() {
        return logsService;
    }

    public VulnerabilitiesService getVulnerabilitiesService() {
        return vulnerabilitiesService;
    }

    public EntitiesService getEntitiesService() {
        return entitiesService;
    }

    private void initSDKProperties() {
        Properties p = new Properties();
        try {
            p.load(OctaneSDK.class.getClassLoader().getResourceAsStream("sdk.properties"));
        } catch (IOException ioe) {
            logger.error("initialization failed: failed to load SDK properties", ioe);
            throw new IllegalStateException("SDK initialization failed: failed to load SDK properties", ioe);
        }
        if (!p.isEmpty()) {
            API_VERSION = Integer.parseInt(p.getProperty("api.version"));
            SDK_VERSION = p.getProperty("sdk.version");
        }
    }

    //  the below base class used ONLY for the correct initiation enforcement of an SDK services
    public static abstract class SDKServiceBase {
        protected final CIPluginServices pluginServices;

        protected SDKServiceBase(Object internalUsageValidator) {
            if (instance == null || instance.pluginServices == null) {
                throw new IllegalStateException("Octane SDK has not yet been initialized, do that first");
            }
            if (internalUsageValidator == null || internalUsageValidator != instance.INTERNAL_USAGE_VALIDATOR) {
                throw new IllegalStateException("SDK's own services MAY NOT be initialized on themselves, use OctaneSDK instance to get reference to pre-initialized services");
            }
            pluginServices = instance.pluginServices;
        }
    }
}
