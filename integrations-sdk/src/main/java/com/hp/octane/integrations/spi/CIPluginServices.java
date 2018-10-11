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

package com.hp.octane.integrations.spi;

import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.CredentialsInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.executor.TestSuiteExecutionInfo;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.general.SonarInfo;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.securityscans.SSCServerInfo;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Composite API of all the endpoints to be implemented by a hosting CI Plugin for Octane use cases.
 */

public interface CIPluginServices {

	/**
	 * Provides CI Server information
	 *
	 * @return ServerInfo object; MUST NOT return null
	 */
	CIServerInfo getServerInfo();

	/**
	 * Provides Plugin's information
	 *
	 * @return PluginInfo object; MUST NOT return null
	 */
	CIPluginInfo getPluginInfo();

	/**
	 * Provides the folder that the plugin is allowed to write to (logs, queues, temporary stuff etc)
	 *
	 * @return File object of type Directory; if no available storage exists the implementation should return NULL
	 */
	File getAllowedOctaneStorage();

	/**
	 * Provides CI Server proxy configuration (managed by plugin implementation)
	 *
	 * @param targetUrl target URL that the proxy, if available, should be relevant to
	 * @return ProxyConfiguration object; if no configuration available the implementation should return NULL
	 */
	CIProxyConfiguration getProxyConfiguration(URL targetUrl);

	/**
	 * Provides a list of Projects existing on this CI Server
	 *
	 * @param includeParameters should the jobs data include parameters or not
	 * @return ProjectList object holding the list of the projects
	 */
	CIJobsList getJobsList(boolean includeParameters);

	/**
	 * Provides Pipeline (structure) from the root CI Job
	 *
	 * @param rootCIJobId root Job CI ID to start pipeline from
	 * @return pipeline's structure or null if CI Job not found
	 */
	PipelineNode getPipeline(String rootCIJobId);

	/**
	 * Executes the Pipeline, running the root job
	 *
	 * @param ciJobId      Job CI ID to execute
	 * @param originalBody request body, expected to be JSON that holds parameters
	 */
	void runPipeline(String ciJobId, String originalBody);       //  [YG]: TODO: replace with parsed parameters/DTO

	/**
	 * Suspends events
	 *
	 * @param suspend desired state of CI events suspension
	 */
	void suspendCIEvents(boolean suspend);

	/**
	 * Provides Snapshot of the latest CI Build of the specified CI Job
	 *
	 * @param ciJobId Job CI ID to get latest snapshot for
	 * @param subTree should the snapshot include sub tree or not
	 * @return latest snapshot's structure or null if build data not found
	 */
	SnapshotNode getSnapshotLatest(String ciJobId, boolean subTree);

	/**
	 * Provides Snapshot of the specified CI Build of the specified CI Job
	 *
	 * @param ciJobId   Job CI ID to get the specified snapshot for
	 * @param buildCiId Build CI ID to get snapshot of
	 * @param subTree   should the snapshot include sub tree or not
	 * @return specified snapshot's structure or null if build data not found
	 */
	SnapshotNode getSnapshotByNumber(String ciJobId, String buildCiId, boolean subTree);

	/**
	 * Provides tests result report for the specific build
	 *
	 * @param jobCiId   Job CI ID to get tests results of
	 * @param buildCiId Build CI ID to get tests results of
	 * @return TestsResult data; NULL if no tests result available
	 */
	InputStream getTestsResult(String jobCiId, String buildCiId);

	/**
	 * Provides build's log as an InputStream
	 *
	 * @param jobCiId   job CI ID of the specific build to get log for
	 * @param buildCiId build CI ID to get log for
	 * @return build's log as an InputStream; NULL if no log available
	 */
	InputStream getBuildLog(String jobCiId, String buildCiId);

	/**
	 * Provides SonarQube info from CI
	 * - this API assumes that there might be only one SSC Server in the CI
	 * - if/when this assumption will be proved wrong we'll add specifying parameter to the method
	 *
	 * @return SonarQube info, if any available
	 */
	SonarInfo getSonarInfo();

	void runTestDiscovery(DiscoveryInfo discoveryInfo);

	void runTestSuiteExecution(TestSuiteExecutionInfo testSuiteExecutionInfo);

	OctaneResponse checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo);

	void deleteExecutor(String id);

	OctaneResponse upsertCredentials(CredentialsInfo credentialsInfo);

	PipelineNode createExecutor(DiscoveryInfo discoveryInfo);

	default boolean isValid() {
		return this.getPluginInfo() != null &&
				this.getServerInfo() != null;
	}
}
