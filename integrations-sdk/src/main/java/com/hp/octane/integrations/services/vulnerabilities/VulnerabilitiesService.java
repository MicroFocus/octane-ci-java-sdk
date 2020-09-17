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

package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.ClosableService;
import com.hp.octane.integrations.services.HasMetrics;
import com.hp.octane.integrations.services.HasQueueService;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.queueing.QueueingService;

import java.util.Map;

public interface VulnerabilitiesService extends ClosableService, HasQueueService, HasMetrics {

	/**
	 * 	 Service instance producer - for internal usage only (protected by inaccessible configurer)
	 * @param queueingService queueingService
	 * @param vulnerabilitiesToolServices vulnerabilitiesToolServices
	 * @param configurer configurer
	 * @param restService restService
	 * @param configurationService Configuration Service
	 * @return return initialized service
	 */
	static VulnerabilitiesService newInstance (QueueingService queueingService,VulnerabilitiesToolService[] vulnerabilitiesToolServices,
											   OctaneSDK.SDKServicesConfigurer configurer, RestService restService, ConfigurationService configurationService) {
		return new VulnerabilitiesServiceImpl(queueingService, vulnerabilitiesToolServices, configurer, restService, configurationService);
	}

	/**
	 * Enqueue retrieve and push vulnerabilities scan
	 * This is the preferred way to push vulnerabilities scan results to Octane. This method provides facilities of queue, non-main thread execution and retry.
	 *
	 * @param jobId            any identification of Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 * @param buildId          any identification of Build or the specified above Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 * @param toolType         toolType
	 * @param startRunTime     timestamp of build start
	 * @param queueItemTimeout timeout defined for this queue item
	 * @param additionalProperties additionalProperties
	 */
	void enqueueRetrieveAndPushVulnerabilities(String jobId,
											   String buildId,
											   ToolType toolType,
											   long startRunTime,
											   long queueItemTimeout,
											   Map<String,String> additionalProperties);
}
