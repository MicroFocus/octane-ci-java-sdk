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

public interface TestsService {

	/**
	 * Publishes CI Event to the NGA server
	 * Tests result pushed to NGA in a synchronous manner, use this method with caution
	 * Returns the NGAResponse.
	 * throws IOException
	 *
	 * @param testsResult
	 */
	OctaneResponse pushTestsResult(TestsResult testsResult) throws IOException;

	/**
	 * Enqueue push tests result by submitting build reference for future tests retrieval
	 * This is the preferred way to push tests results to NGA, since provides facilities of queue, non-main thread execution and retry
	 *
	 * @param jobId
	 * @param buildNumber
	 */
	void enqueuePushTestsResult(String jobId, String buildNumber);
}
