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
	private final QueueService queueService;
	private final RestService restService;
	private final ConfigurationService configurationService;
	private final TasksProcessor tasksProcessor;
	private final EventsService eventsService;
	private final TestsService testsService;
	private final LogsService logsService;
	private final VulnerabilitiesService vulnerabilitiesService;
	private final EntitiesService entitiesService;

	OctaneClientImpl(OctaneSDK.SDKServicesConfigurer configurer) {
		if (configurer == null || configurer.pluginServices == null) {
			throw new IllegalArgumentException("services configurer MUST NOT be null nor empty");
		}
		this.pluginServices = configurer.pluginServices;
		LoggingService.newInstance(configurer);
		queueService = QueueService.newInstance(configurer);
		restService = RestService.newInstance(configurer);
		tasksProcessor = TasksProcessor.newInstance(configurer);
		configurationService = ConfigurationService.newInstance(configurer, restService);
		eventsService = EventsService.newInstance(configurer, restService);
		testsService = TestsService.newInstance(configurer, queueService, restService);
		logsService = LogsService.newInstance(configurer, queueService, restService);
		vulnerabilitiesService = VulnerabilitiesService.newInstance(configurer, restService);
		entitiesService = EntitiesService.newInstance(configurer, restService);
		BridgeService.newInstance(configurer, restService, tasksProcessor);
		logger.info("OctaneClient initialized with currently effective instance ID '" + getEffectiveInstanceId() + "' (remember, instance ID is not owned by SDK and may change on the fly)");
	}

	public RestService getRestService() {
		return restService;
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

	@Override
	public String getEffectiveInstanceId() throws IllegalStateException {
		if (pluginServices.getServerInfo() == null) {
			throw new IllegalStateException("plugin services resolved CIServerInfo to be NULL");
		}
		if (pluginServices.getServerInfo().getInstanceId() == null || pluginServices.getServerInfo().getInstanceId().isEmpty()) {
			throw new IllegalStateException("plugin services resolved instance ID to be NULL or empty");
		}
		return pluginServices.getServerInfo().getInstanceId();
	}

	@Override
	public void close() {
		//  TODO: perform any closing actions on the services (Rest Service is a good candidate here)
		OctaneSDK.removeClient(this);
	}
}
