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

package com.hp.octane.integrations.services.logs;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.ClosableService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.queueing.QueueingService;

public interface LogsService extends ClosableService {

	/**
	 * Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer   SDK services configurer object
	 * @param queueingService Queue service
	 * @param restService  Rest Service
	 * @return initialized service
	 */
	static LogsService newInstance(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService) {
		return new LogsServiceImpl(configurer, queueingService, restService);
	}

	/**
	 * Enqueue push build log by submitting build reference for future log retrieval
	 * This method provides facilities of queue, non-main thread execution and retry
	 * Pay attention: it is assumed that your SPI implementation knows to retrieve and provide build log as a stream given the relevant jobId/buildId (as they are provided hereby)
	 *
	 * @param jobId   any identification of Job, that the build log is related to and that SPI's `getBuildLog` method will know to work with
	 * @param buildId any identification of Build of the specified above Job, that the build log is related to and that SPI's `getBuildLog` method will know to work with
	 * @param rootJobId any identification of Root Job that triggered this job. Null - if there is no such job.
	 */
	void enqueuePushBuildLog(String jobId, String buildId, String rootJobId);
}
