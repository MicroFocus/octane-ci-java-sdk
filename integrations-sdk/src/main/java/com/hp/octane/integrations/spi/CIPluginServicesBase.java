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
import com.hp.octane.integrations.dto.executor.CredentialsInfo;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.executor.TestSuiteExecutionInfo;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.SonarInfo;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.securityscans.SSCServerInfo;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Empty abstract implementation of CIPluginServices.
 */

public abstract class CIPluginServicesBase implements CIPluginServices {

	@Override
	public File getAllowedOctaneStorage() {
		return null;
	}

	@Override
	public CIProxyConfiguration getProxyConfiguration(URL targetUrl) {
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
	}

	@Override
	public void suspendCIEvents(boolean suspend) {
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
	public InputStream getTestsResult(String jobCiId, String buildCiId) {
		return null;
	}

	@Override
	public InputStream getBuildLog(String jobCiId, String buildCiId) {
		return null;
	}

	@Override
	public SonarInfo getSonarInfo() {
		return null;
	}

	@Override
	public void runTestDiscovery(DiscoveryInfo discoveryInfo) {
	}

	@Override
	public void runTestSuiteExecution(TestSuiteExecutionInfo testSuiteExecutionInfo) {
	}

	@Override
	public OctaneResponse checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo) {
		return null;
	}

	@Override
	public void deleteExecutor(String id) {
	}

	@Override
	public OctaneResponse upsertCredentials(CredentialsInfo credentialsInfo) {
		return null;
	}

	@Override
	public PipelineNode createExecutor(DiscoveryInfo discoveryInfo) {
		return null;
	}
}
