package com.hp.octane.integrations.api;

/**
 * OctaneClient is a single entry point for an integration with specific Octane target (server AND shared space)
 * OctaneClient instance is responsible for a correct initialization/shutdown cycle and provisioning of a services in the concrete context
 * OctaneClient instance's context is defined by a specific instance of CIPluginServices
 */

public interface OctaneClient {

	/**
	 * provides Configuration service
	 *
	 * @return service, MUST NOT be null
	 */
	ConfigurationService getConfigurationService();

	/**
	 * provides REST service
	 *
	 * @return service, MUST NOT be null
	 */
	RestService getRestService();

	/**
	 * provides Tasks service
	 *
	 * @return service, MUST NOT be null
	 */
	TasksProcessor getTasksProcessor();

	/**
	 * provides Events service
	 *
	 * @return service, MUST NOT be null
	 */
	EventsService getEventsService();

	/**
	 * provides Tests service
	 *
	 * @return service, MUST NOT be null
	 */
	TestsService getTestsService();

	/**
	 * provides Logs service
	 *
	 * @return service, MUST NOT be null
	 */
	LogsService getLogsService();

	/**
	 * provides Vulnerabilities service
	 *
	 * @return service, MUST NOT be null
	 */
	VulnerabilitiesService getVulnerabilitiesService();

	/**
	 * provides Entities service
	 *
	 * @return service, MUST NOT be null
	 */
	EntitiesService getEntitiesService();

	/**
	 * resolves instance ID from the plugin services
	 * pay attention: this property is mutable on the plugin's side, therefore we'll be resolving it at runtime
	 *
	 * @return instance ID; won't be NULL, if the underlying plugin service will resolve instance ID to NULL, this method should throw IllegalStateException
	 */
	String getEffectiveInstanceId() throws IllegalStateException;
}
