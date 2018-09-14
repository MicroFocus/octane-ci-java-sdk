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

package com.hp.octane.integrations;

import com.hp.octane.integrations.services.coverage.SonarService;
import com.hp.octane.integrations.services.bridge.BridgeService;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.events.EventsService;
import com.hp.octane.integrations.services.logging.LoggingService;
import com.hp.octane.integrations.services.logs.LogsService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.tasking.TasksProcessor;
import com.hp.octane.integrations.services.tests.TestsService;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesService;
import com.hp.octane.integrations.services.queue.QueueService;
import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * protected implementation of the OctaneClient
 * for internal usage only
 * refer to OctaneClient API definition for a function specification
 */

final class OctaneClientImpl implements OctaneClient {
	private static final Logger logger = LogManager.getLogger(OctaneClientImpl.class);

	private final CIPluginServices pluginServices;
	private final ConfigurationService configurationService;
	private final SonarService sonarService;
	private final EntitiesService entitiesService;
	private final EventsService eventsService;
	private final LogsService logsService;
	private final RestService restService;
	private final TasksProcessor tasksProcessor;
	private final TestsService testsService;
	private final VulnerabilitiesService vulnerabilitiesService;

	OctaneClientImpl(OctaneSDK.SDKServicesConfigurer configurer) {
		if (configurer == null || configurer.pluginServices == null) {
			throw new IllegalArgumentException("services configurer MUST NOT be null nor empty");
		}

		//  internals init
		pluginServices = configurer.pluginServices;
		LoggingService.newInstance(configurer);
		QueueService queueService = QueueService.newInstance(configurer);

		//  independent services init
		restService = RestService.newInstance(configurer);
		tasksProcessor = TasksProcessor.newInstance(configurer);

		//  dependent services init
		configurationService = ConfigurationService.newInstance(configurer, restService);
		sonarService = SonarService.newInstance(configurer, queueService, restService);
		entitiesService = EntitiesService.newInstance(configurer, restService);
		eventsService = EventsService.newInstance(configurer, restService);
		logsService = LogsService.newInstance(configurer, queueService, restService);
		testsService = TestsService.newInstance(configurer, queueService, restService);
		vulnerabilitiesService = VulnerabilitiesService.newInstance(configurer, queueService, restService);

		//  bridge init is the last one, to make sure we are not processing any task until all services are up
		BridgeService.newInstance(configurer, restService, tasksProcessor);

		logger.info("OctaneClient initialized with currently effective instance ID '" + getEffectiveInstanceId() + "' (remember, instance ID is not owned by SDK and may change on the fly)");
	}

	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	public SonarService getSonarService() {
		return sonarService;
	}

	public EntitiesService getEntitiesService() {
		return entitiesService;
	}

	public EventsService getEventsService() {
		return eventsService;
	}

	public LogsService getLogsService() {
		return logsService;
	}

	public RestService getRestService() {
		return restService;
	}

	public TasksProcessor getTasksProcessor() {
		return tasksProcessor;
	}

	public TestsService getTestsService() {
		return testsService;
	}

	public VulnerabilitiesService getVulnerabilitiesService() {
		return vulnerabilitiesService;
	}

	@Override
	public String getEffectiveInstanceId() throws IllegalStateException {
		if (pluginServices.getServerInfo() == null) {
			throw new IllegalStateException("plugin services resolved CIServerInfo to be NULL; this was not the case when client was initially created");
		}
		if (pluginServices.getServerInfo().getInstanceId() == null || pluginServices.getServerInfo().getInstanceId().isEmpty()) {
			throw new IllegalStateException("plugin services resolved instance ID to be NULL or empty; this was not the case when client was initially created");
		}
		return pluginServices.getServerInfo().getInstanceId();
	}

	@Override
	public void close() {
		restService.obtainOctaneRestClient().shutdown();
		OctaneSDK.removeClient(this);
	}

	@Override
	public String toString() {
		return "OctaneClientImpl{ instanceId: " + getEffectiveInstanceId() + " }";
	}
}
