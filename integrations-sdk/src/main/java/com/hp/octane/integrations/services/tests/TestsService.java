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
 *
 */

package com.hp.octane.integrations.services.tests;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.services.queue.QueueService;
import com.hp.octane.integrations.services.rest.RestService;

import java.io.IOException;
import java.io.InputStream;

public interface TestsService {

	/**
	 * Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer   SDK services configurer object
	 * @param queueService Queue service
	 * @param restService  Rest Service
	 * @return initialized service
	 */
	static TestsService newInstance(OctaneSDK.SDKServicesConfigurer configurer, QueueService queueService, RestService restService) {
		return new TestsServiceImpl(configurer, queueService, restService);
	}

	/**
	 * Verifies against Octane, whether the tests result for the specific Job are relevant or not
	 */
	boolean isTestsResultRelevant(String serverCiId, String jobCiId) throws IOException;

	/**
	 * Publishes Tests Result to Octane server - SYNCHRONOUSLY
	 *
	 * @param testsResult ready-to-be-pushed TestsResult object, having a collection of tests results with the relevant build context
	 */
	OctaneResponse pushTestsResult(TestsResult testsResult) throws IOException;

	/**
	 * Publishes Tests Result to Octane server - SYNCHRONOUSLY
	 *
	 * @param testsResult ready-to-be-pushed TestsResult resource given as an InputStream
	 */
	OctaneResponse pushTestsResult(InputStream testsResult) throws IOException;

	/**
	 * Enqueue push tests result by submitting build reference for future tests retrieval.
	 * This is the preferred way to push tests results to Octane. This method provides facilities of queue, non-main thread execution and retry.
	 * Pay attention, that when pushing tests results this way, it is assumed that your SPI implementation knows to retrieve and provide TestsResult object given the relevant jobCiId/buildCiId (as they are provided hereby)
	 *
	 * @param jobId   any identification of Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 * @param buildId any identification of Build or the specified above Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 */
	void enqueuePushTestsResult(String jobId, String buildId);
}
