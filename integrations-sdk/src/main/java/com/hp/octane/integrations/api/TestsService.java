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
	 * Publishes CI Event to the Octane server.
	 * Tests result is pushed to Octane server in a synchronous manner.
	 */
	OctaneResponse pushTestsResult(TestsResult testsResult) throws IOException;

	/**
	 * Enqueue push tests result by submitting build reference for future tests retrieval.
	 * This is the preferred way to push tests results to Octane. This method provides facilities of queue,
     * non-main thread execution and retry.
	 */
	void enqueuePushTestsResult(String jobId, String buildNumber);
}
