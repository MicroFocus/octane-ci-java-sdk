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

package com.hp.octane.integrations;

import com.hp.octane.integrations.api.*;
import com.hp.octane.integrations.services.bridge.BridgeServiceImpl;
import com.hp.octane.integrations.services.configuration.ConfigurationServiceImpl;
import com.hp.octane.integrations.services.events.EventsServiceImpl;
import com.hp.octane.integrations.services.logging.LoggingService;
import com.hp.octane.integrations.services.rest.RestServiceImpl;
import com.hp.octane.integrations.services.tasking.TasksProcessorImpl;
import com.hp.octane.integrations.services.tests.TestsServiceImpl;
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

	private final CIPluginServices pluginServices;
	private final SDKConfigurer configurer;

	private OctaneSDK(CIPluginServices ciPluginServices) {
		initSDKProperties();
		pluginServices = ciPluginServices;
		configurer = new SDKConfigurer(ciPluginServices);
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
				throw new IllegalArgumentException("SDK initialization failed: MUST be initialized with valid plugin services provider");
			}
			instance = new OctaneSDK(ciPluginServices);
			logger.info("");
			logger.info("SDK has been initialized");
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
		return configurer.pluginServices;
	}

	public ConfigurationService getConfigurationService() {
		return configurer.configurationService;
	}

	public RestService getRestService() {
		return configurer.restService;
	}

	public TasksProcessor getTasksProcessor() {
		return configurer.tasksProcessor;
	}

	public EventsService getEventsService() {
		return configurer.eventsService;
	}

	public TestsService getTestsService() {
		return configurer.testsService;
	}

	private void initSDKProperties() {
		Properties p = new Properties();
		try {
			p.load(OctaneSDK.class.getClassLoader().getResourceAsStream("sdk.properties"));
		} catch (IOException ioe) {
			logger.error("SDK initialization failed: failed to load SDK properties", ioe);
			throw new IllegalStateException("SDK initialization failed: failed to load SDK properties", ioe);
		}
		if (!p.isEmpty()) {
			API_VERSION = Integer.parseInt(p.getProperty("api.version"));
			SDK_VERSION = p.getProperty("sdk.version");
		}
	}

	private static class SDKConfigurer {
		private final CIPluginServices pluginServices;
		private final LoggingService loggingService;
		private final RestService restService;
		private final ConfigurationService configurationService;
		private final BridgeServiceImpl bridgeServiceImpl;
		private final TasksProcessor tasksProcessor;
		private final EventsService eventsService;
		private final TestsService testsService;

		private SDKConfigurer(CIPluginServices pluginServices) {
			this.pluginServices = pluginServices;
			loggingService = new LoggingService(this);
			restService = new RestServiceImpl(this);
			tasksProcessor = new TasksProcessorImpl(this);
			configurationService = new ConfigurationServiceImpl(this, restService);
			eventsService = new EventsServiceImpl(this, restService);
			testsService = new TestsServiceImpl(this, restService);
			bridgeServiceImpl = new BridgeServiceImpl(this, restService, tasksProcessor);
		}
	}

	//  the below base class used ONLY for a correct initiation enforcement of an SDK services
	public static abstract class SDKServiceBase {

		protected SDKServiceBase(Object configurer) {
			if (configurer == null) {
				throw new IllegalArgumentException("configurator MUST NOT be null");
			}
			if (!(configurer instanceof SDKConfigurer)) {
				throw new IllegalArgumentException("configurator MUST be of a correct type");
			}
		}

		protected CIPluginServices getPluginServices() {
			if (instance == null || instance.pluginServices == null) {
				throw new IllegalStateException("this method MUST NOT be called prior to OctaneSDK initialization");
			}

			return instance.pluginServices;
		}
	}
}
