/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.octane.integrations.services.tests;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.services.ClosableService;
import com.hp.octane.integrations.services.HasMetrics;
import com.hp.octane.integrations.services.HasQueueService;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.RestService;

import java.io.IOException;
import java.io.InputStream;

public interface TestsService extends ClosableService, HasQueueService, HasMetrics {

	/**
	 * Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer      SDK services configurer object
	 * @param queueingService Queue service
	 * @param restService     Rest Service
	 * @param configurationService Configuration Service
	 * @return initialized service
	 */
	static TestsService newInstance(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService, ConfigurationService configurationService) {
		return new TestsServiceImpl(configurer, queueingService, restService, configurationService);
	}

	/**
	 *
	 * Publishes Tests Result to Octane server - SYNCHRONOUSLY
	 *
	 * @param testsResult ready-to-be-pushed TestsResult resource given as an InputStream
	 * @param jobId       ID of the job that produced the results
	 * @param buildId     ID of the build that produced the results
	 * @return OctaneResponse
	 * @throws IOException IOException
	 */
	OctaneResponse pushTestsResult(InputStream testsResult, String jobId, String buildId) throws IOException;


	/**
	 * Enqueue push tests result by submitting build reference for future tests retrieval.
	 * This is the preferred way to push tests results to Octane. This method provides facilities of queue, non-main thread execution and retry.
	 * Pay attention, that when pushing tests results this way, it is assumed that your SPI implementation knows to retrieve and provide TestsResult object given the relevant jobCiId/buildCiId (as they are provided hereby)
	 *
	 * @param jobId   any identification of Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 * @param buildId any identification of Build or the specified above Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 * @param rootJobId any identification of Root Job that triggered this job. Null - if there is no such job.
	 */
	void enqueuePushTestsResult(String jobId, String buildId, String rootJobId);
}
