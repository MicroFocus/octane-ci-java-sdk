/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.api;

import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.tests.TestsResult;

import java.io.IOException;
import java.io.InputStream;

public interface TestsService {

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
	 * @param jobCiId   any identification of Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 * @param buildCiId any identification of Build or the specified above Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 */
	void enqueuePushTestsResult(String jobCiId, String buildCiId);
}
