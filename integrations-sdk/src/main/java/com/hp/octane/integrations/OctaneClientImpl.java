package com.hp.octane.integrations;

import com.hp.octane.integrations.api.ConfigurationService;
import com.hp.octane.integrations.api.EntitiesService;
import com.hp.octane.integrations.api.EventsService;
import com.hp.octane.integrations.api.LogsService;
import com.hp.octane.integrations.api.OctaneClient;
import com.hp.octane.integrations.api.RestService;
import com.hp.octane.integrations.api.TasksProcessor;
import com.hp.octane.integrations.api.TestsService;
import com.hp.octane.integrations.api.VulnerabilitiesService;
import com.hp.octane.integrations.services.bridge.BridgeServiceImpl;
import com.hp.octane.integrations.services.configuration.ConfigurationServiceImpl;
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

/**
 * protected implementation of the OctaneClient
 * for internal usage only
 * refer to OctaneClient API definition for a function specification
 */

class OctaneClientImpl implements OctaneClient {
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
		new LoggingServiceImpl(configurer);
		queueService = new QueueServiceImpl(configurer);
		restService = new RestServiceImpl(configurer);
		tasksProcessor = new TasksProcessorImpl(configurer);
		configurationService = new ConfigurationServiceImpl(configurer, restService);
		eventsService = new EventsServiceImpl(configurer, restService);
		testsService = new TestsServiceImpl(configurer, queueService, restService);
		logsService = new LogsServiceImpl(configurer, queueService, restService);
		vulnerabilitiesService = new VulnerabilitiesServiceImpl(configurer, restService);
		entitiesService = new EntitiesServiceImpl(configurer, restService);
		new BridgeServiceImpl(configurer, restService, tasksProcessor);
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
}
