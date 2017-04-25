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

package com.hp.octane.integrations.spi;

import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.executor.TestSuiteExecutionInfo;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.pipelines.BuildHistory;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;
import com.hp.octane.integrations.dto.tests.TestsResult;

import java.io.File;

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
	 * Provides the folder that the plugin is allowed to write to (logs, temporary stuff etc)
	 *
	 * @return File object of type Directory; if no available storage exists the implementation should return NULL
	 */
	File getAllowedOctaneStorage();

	/**
	 * Provider the folder that includes predictive-Octane properties
	 *
	 * @return File object of type Directory; if no available storage exists the implementation should return NULL
	 */
	File getPredictiveOctanePath();

	/**
	 * Provides Octane Server configuration (managed by plugin implementation)
	 *
	 * @return OctaneConfiguration object; if no configuration available the implementation should return NULL
	 */
	OctaneConfiguration getOctaneConfiguration();

	/**
	 * Provides CI Server proxy configuration (managed by plugin implementation)
	 *
	 * @param targetHost target host that the proxy, if available, should be relevant to
	 * @return ProxyConfiguration object; if no configuration available the implementation should return NULL
	 */
	CIProxyConfiguration getProxyConfiguration(String targetHost);

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
	 * Retrieves aggregated latest builds info
	 *
	 * @param ciJobId      Job CI ID to get history data for
	 * @param originalBody request body
	 * @return history data for the specified pipeline
	 */
	@Deprecated
	BuildHistory getHistoryPipeline(String ciJobId, String originalBody);

	/**
	 * Retrieves tests result report for the specific build
	 *
	 * @param jobId       Job CI ID to get tests results of
	 * @param buildNumber Build CI ID to get tests results of
	 * @return TestsResult data; NULL if no tests result available
	 */
	TestsResult getTestsResult(String jobId, String buildNumber);

	void runTestDiscovery(DiscoveryInfo discoveryInfo);

	void runTestSuiteExecution(TestSuiteExecutionInfo testSuiteExecutionInfo);

	boolean checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo);
}
