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

package com.hp.octane.integrations.services.coverage;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.coverage.CoverageReportType;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.RestService;

import java.io.InputStream;

/**
 * Coverage service provides a means to get and submit coverage to Octane
 */

public interface CoverageService {

	/**
	 * Coverage Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer SDK services configurer object
	 * @return initialized service
	 */
	static CoverageService newInstance(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService) {
		return new CoverageServiceImpl(configurer, queueingService, restService);
	}

	/**
	 * performs 'preflight' request to Octane and returns Workspace IDs of a workspaces interested in coverage report of this job, if any
	 *
	 * @param jobId Job CI ID of a context job
	 * @return an array of interested Workspace IDs
	 */
	boolean isCoverageReportRelevant(String jobId);

	/**
	 * push coverage, directly pushes the report to Octane
	 *
	 * @param jobId          CI Job ID
	 * @param buildId        CI Build ID
	 * @param reportType     report type of the pushed content
	 * @param coverageReport coverage report content
	 */
	OctaneResponse pushCoverage(String jobId, String buildId, CoverageReportType reportType, InputStream coverageReport);

	/**
	 * enqueue push coverage task, which will be managed in queue, retrieve the content when running and attempt to push it to Octane
	 *
	 * @param jobId          CI Job ID
	 * @param buildId        CI Build ID
	 * @param reportType     report type of the pushed content
	 * @param reportFileName report file name, optional; when plugin knows that the report is a single file and doesn't need a name, may provide NULL here
	 */
	void enqueuePushCoverage(String jobId, String buildId, CoverageReportType reportType, String reportFileName);
}
