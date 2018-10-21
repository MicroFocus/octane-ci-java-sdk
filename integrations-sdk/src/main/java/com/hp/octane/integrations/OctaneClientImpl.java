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
import com.hp.octane.integrations.services.coverage.CoverageService;
import com.hp.octane.integrations.services.coverage.SonarService;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.events.EventsService;
import com.hp.octane.integrations.services.logging.LoggingService;
import com.hp.octane.integrations.services.logs.LogsService;
import com.hp.octane.integrations.services.pipelines.PipelineContextService;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.tasking.TasksProcessor;
import com.hp.octane.integrations.services.tests.TestsService;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * protected implementation of the OctaneClient
 * for internal usage only
 * refer to OctaneClient API definition for a function specification
 */

final class OctaneClientImpl implements OctaneClient {
	private static final Logger logger = LogManager.getLogger(OctaneClientImpl.class);

	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final BridgeService bridgeService;
	private final ConfigurationService configurationService;
	private final CoverageService coverageService;
	private final SonarService sonarService;
	private final EntitiesService entitiesService;
	private final PipelineContextService pipelineContextService;
	private final EventsService eventsService;
	private final LogsService logsService;
	private final QueueingService queueingService;
	private final RestService restService;
	private final TasksProcessor tasksProcessor;
	private final TestsService testsService;
	private final VulnerabilitiesService vulnerabilitiesService;

	OctaneClientImpl(OctaneSDK.SDKServicesConfigurer configurer) {
		if (configurer == null) {
			throw new IllegalArgumentException("services configurer MUST NOT be null nor empty");
		}

		//  internals init
		this.configurer = configurer;
		ensureStorageIfAny();
		LoggingService.newInstance(configurer);
		queueingService = QueueingService.newInstance(configurer);

		//  independent services init
		restService = RestService.newInstance(configurer);
		tasksProcessor = TasksProcessor.newInstance(configurer);

		//  dependent services init
		configurationService = ConfigurationService.newInstance(configurer, restService);
		coverageService = CoverageService.newInstance(configurer, queueingService, restService);
		sonarService = SonarService.newInstance(configurer, queueingService, coverageService);
		entitiesService = EntitiesService.newInstance(configurer, restService);
		pipelineContextService = PipelineContextService.newInstance(configurer, restService);
		eventsService = EventsService.newInstance(configurer, restService);
		logsService = LogsService.newInstance(configurer, queueingService, restService);
		testsService = TestsService.newInstance(configurer, queueingService, restService);
		vulnerabilitiesService = VulnerabilitiesService.newInstance(configurer, queueingService, restService);

		//  bridge init is the last one, to make sure we are not processing any task until all services are up
		bridgeService = BridgeService.newInstance(configurer, restService, tasksProcessor);

		//  register shutdown hook to allow graceful shutdown of services/resources
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			String instanceId = configurer.octaneConfiguration.getInstanceId();
			logger.info("closing OctaneClient " + instanceId + " as per Runtime shutdown request...");
			try {
				this.close();
			} catch (Throwable throwable) {
				logger.error("failed during shutdown of OctaneClient " + instanceId, throwable);
			} finally {
				logger.info("...OctaneClient " + instanceId + " CLOSED");
			}
		}));

		logger.info("OctaneClient initialized with instance ID: " + configurer.octaneConfiguration.getInstanceId() + ", shared space ID: " + configurer.octaneConfiguration.getSharedSpace());
	}

	@Override
	public String getInstanceId() {
		return configurer.octaneConfiguration.getInstanceId();
	}

	@Override
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	@Override
	public CoverageService getCoverageService() {
		return coverageService;
	}

	@Override
	public SonarService getSonarService() {
		return sonarService;
	}

	@Override
	public EntitiesService getEntitiesService() {
		return entitiesService;
	}

	@Override
	public PipelineContextService getPipelineContextService() {
		return pipelineContextService;
	}

	@Override
	public EventsService getEventsService() {
		return eventsService;
	}

	@Override
	public LogsService getLogsService() {
		return logsService;
	}

	@Override
	public RestService getRestService() {
		return restService;
	}

	@Override
	public TasksProcessor getTasksProcessor() {
		return tasksProcessor;
	}

	@Override
	public TestsService getTestsService() {
		return testsService;
	}

	@Override
	public VulnerabilitiesService getVulnerabilitiesService() {
		return vulnerabilitiesService;
	}

	@Override
	public String toString() {
		return "OctaneClientImpl{ instanceId: " + configurer.octaneConfiguration.getInstanceId() + " }";
	}

	private void close() {
		queueingService.shutdown();
		bridgeService.shutdown();
		coverageService.shutdown();
		sonarService.shutdown();
		eventsService.shutdown();
		logsService.shutdown();
		testsService.shutdown();
		vulnerabilitiesService.shutdown();
		restService.obtainOctaneRestClient().shutdown();
	}

	void remove() {
		//  shut down services
		close();

		//  clean storage
		if (configurer.pluginServices.getAllowedOctaneStorage() != null) {
			String instanceId = configurer.octaneConfiguration.getInstanceId();
			File instanceOrientedStorage = new File(configurer.pluginServices.getAllowedOctaneStorage(), "nga" + File.separator + instanceId);
			if (deleteFolder(instanceOrientedStorage)) {
				logger.info("cleaned dedicated storage for OctaneClient instance " + instanceId);
			} else {
				logger.error("failed to clean dedicated storage for OctaneClient instance " + instanceId);
			}
		}
	}

	private void ensureStorageIfAny() {
		if (configurer.pluginServices.getAllowedOctaneStorage() != null) {
			String instanceId = configurer.octaneConfiguration.getInstanceId();
			File instanceOrientedStorage = new File(configurer.pluginServices.getAllowedOctaneStorage(), "nga" + File.separator + instanceId);
			if (instanceOrientedStorage.mkdirs()) {
				logger.info("verified dedicated storage for OctaneClient instance " + instanceId);
			} else {
				logger.error("failed to create dedicated storage for OctaneClient instance " + instanceId);
			}
		}
	}

	private boolean deleteFolder(File folder) {
		File[] children = folder.listFiles();
		if (children != null) {
			for (File file : children) {
				deleteFolder(file);
			}
		}
		return folder.delete();
	}
}
