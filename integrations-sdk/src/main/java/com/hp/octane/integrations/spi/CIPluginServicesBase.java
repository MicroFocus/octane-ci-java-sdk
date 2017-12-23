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
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.CredentialsInfo;
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
 * Empty abstract implementation of CIPluginServices.
 * [YG] TODO: we'll better to define methods that are absolute MUST for and SDK to function and NOT provide default empty implementation of them, thus forcing their correct implementation
 */

public abstract class CIPluginServicesBase implements CIPluginServices {

	@Override
	public CIServerInfo getServerInfo() {
		return null;
	}

	@Override
	public CIPluginInfo getPluginInfo() {
		return null;
	}

	@Override
	public File getAllowedOctaneStorage() {
		return null;
	}

	@Override
	public File getPredictiveOctanePath() {
		return null;
	}

	@Override
	public OctaneConfiguration getOctaneConfiguration() {
		return null;
	}

	@Override
	public CIProxyConfiguration getProxyConfiguration(String targetHost) {
		return null;
	}

	@Override
	public CIJobsList getJobsList(boolean includeParameters) {
		return null;
	}

	@Override
	public PipelineNode getPipeline(String rootCIJobId) {
		return null;
	}

	@Override
	public void runPipeline(String ciJobId, String originalBody) {
		//do nothing
	}

	@Override
	public void suspendCiEvents(boolean suspend) {
		//do nothing
	}

	@Override
	public SnapshotNode getSnapshotLatest(String ciJobId, boolean subTree) {
		return null;
	}

	@Override
	public SnapshotNode getSnapshotByNumber(String ciJobId, String buildCiId, boolean subTree) {
		return null;
	}

	@Override
	public BuildHistory getHistoryPipeline(String ciJobId, String originalBody) {
		return null;
	}

	@Override
	public TestsResult getTestsResult(String jobCiId, String buildCiId) {
		return null;
	}

	@Override
	public void runTestDiscovery(DiscoveryInfo discoveryInfo) {
		//do nothing
	}

	@Override
	public void runTestSuiteExecution(TestSuiteExecutionInfo testSuiteExecutionInfo) {
		//do nothing
	}

	@Override
	public OctaneResponse checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo) {
		//do nothing
		return null;
	}

	@Override
	public void deleteExecutor(String id) {
		//do nothing
	}

	@Override
	public OctaneResponse upsertCredentials(CredentialsInfo credentialsInfo) {
		//do nothing
		return null;
	}
}
