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

package com.hp.octane.integrations.api;

import com.hp.octane.integrations.dto.connectivity.OctaneResponse;

import java.io.IOException;
import java.io.InputStream;

public interface VulnerabilitiesService {



	/**
	 * Publishes Tests Result to Octane server - SYNCHRONOUSLY
	 *
	 * @param vulnerabilities ready-to-be-pushed TestsResult resource given as an InputStream
	 * @param jobId
	 * @param buildId
	 */
	OctaneResponse pushVulnerabilities(InputStream vulnerabilities, String jobId, String buildId) throws IOException;

	/**
	 * Enqueue push tests result by submitting build reference for future tests retrieval.
	 * This is the preferred way to push tests results to Octane. This method provides facilities of queue, non-main thread execution and retry.
	 * Pay attention, that when pushing tests results this way, it is assumed that your SPI implementation knows to retrieve and provide TestsResult object given the relevant jobCiId/buildCiId (as they are provided hereby)
	 *
	 * @param jobId   any identification of Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 * @param buildId any identification of Build or the specified above Job, that the tests results are related to and that SPI's `getTestsResult` method will know to work with
	 */
	void enqueuePushVulnerabilitiesScanResult(String jobId, String buildId, String projectName, String projectVersion, String dir);
	/**
	 * check if the corresponding pipeline in octane is exists and is type of security
	 * @param jobId
	 * @param buildId
	 * @return
	 * @throws IOException
	 */

	boolean isVulnerabilitiesRelevant( String jobId, String buildId) throws IOException;
}
