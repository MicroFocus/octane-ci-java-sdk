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

package com.hp.octane.integrations;

import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.CredentialsInfo;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.executor.TestSuiteExecutionInfo;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.general.SonarInfo;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Empty abstract implementation of CIPluginServices.
 */

public abstract class CIPluginServices {
	private String instanceId;

	//  ABSTRACTS
	//
	public abstract CIServerInfo getServerInfo();

	public abstract CIPluginInfo getPluginInfo();

	//  CONCRETE FINALS
	//
	protected final String getInstanceId() {
		return instanceId;
	}

	final void setInstanceId(String instanceId) {
		if (instanceId == null || instanceId.isEmpty()) {
			throw new IllegalArgumentException("instance ID MUST NOT be null nor empty");
		}
		if (instanceId.equals(this.instanceId)) {
			return;
		}
		if (this.instanceId != null) {
			throw new IllegalStateException("instance ID IS NOT EXPECTED to be overwrote; current value: " + this.instanceId + ", newly provided value: " + instanceId);
		}
		this.instanceId = instanceId;
	}

	final boolean isValid() {
		return instanceId != null && !instanceId.isEmpty() &&
				getPluginInfo() != null &&
				getServerInfo() != null;
	}

	//  CONCRETE STUBS FOR OVERRIDES
	//
	public File getAllowedOctaneStorage() {
		return null;
	}

	public CIProxyConfiguration getProxyConfiguration(URL targetUrl) {
		return null;
	}

	public CIJobsList getJobsList(boolean includeParameters) {
		return null;
	}

	public PipelineNode getPipeline(String rootCIJobId) {
		return null;
	}

	public void runPipeline(String ciJobId, String originalBody) {
	}

	public void suspendCIEvents(boolean suspend) {
	}

	public SnapshotNode getSnapshotLatest(String ciJobId, boolean subTree) {
		return null;
	}

	public SnapshotNode getSnapshotByNumber(String ciJobId, String buildCiId, boolean subTree) {
		return null;
	}

	public InputStream getTestsResult(String jobCiId, String buildCiId) {
		return null;
	}

	public InputStream getBuildLog(String jobCiId, String buildCiId) {
		return null;
	}

	public SonarInfo getSonarInfo() {
		return null;
	}

	public void runTestDiscovery(DiscoveryInfo discoveryInfo) {
	}

	public void runTestSuiteExecution(TestSuiteExecutionInfo testSuiteExecutionInfo) {
	}

	public OctaneResponse checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo) {
		return null;
	}

	public void deleteExecutor(String id) {
	}

	public OctaneResponse upsertCredentials(CredentialsInfo credentialsInfo) {
		return null;
	}

	public PipelineNode createExecutor(DiscoveryInfo discoveryInfo) {
		return null;
	}
}
