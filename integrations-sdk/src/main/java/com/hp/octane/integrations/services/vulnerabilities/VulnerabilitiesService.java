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
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.RestService;

public interface VulnerabilitiesService {

	/**
	 * Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer  SDK services configurer object
	 * @param restService Rest Service
	 * @return initialized service
	 */
	static VulnerabilitiesService newInstance(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService) {
		return new VulnerabilitiesServiceImpl(configurer, queueingService, restService);
	}

	/**
	 * Enqueue retrieve and push vulnerabilities scan
	 * This is the preferred way to push vulnerabilities scan results to Octane. This method provides facilities of queue, non-main thread execution and retry.
	 *
	 * @param jobId   any identification of Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 * @param buildId any identification of Build or the specified above Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 */
	void enqueueRetrieveAndPushVulnerabilities(String jobId, String buildId,
	                                           String projectName, String projectVersion,
	                                           long startRunTime,
	                                           long queueItemTimeout);
}
