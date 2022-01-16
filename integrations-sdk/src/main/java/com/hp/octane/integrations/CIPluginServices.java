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
import com.hp.octane.integrations.dto.general.*;
import com.hp.octane.integrations.dto.parameters.CIParameters;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.securityscans.FodServerConfiguration;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.exceptions.SPIMethodNotImplementedException;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Definition of CIPluginServices SPI
 *
 * CI Plugin Services is an SPI definition, linking logic enabling SDK define a required essentials from and to interact with the hosting plugin
 * Hosting plugin MUST implement a concrete and valid class extending this one and provide it upon OctaneClient initialization
 * CI Plugin Services implementation MUST be stateless, the only state that will be injected into it by an SDK is the bounded INSTANCE ID
 */

public abstract class CIPluginServices {
	private String instanceId;

	//
	//  ABSTRACTS
	//

	/**
	 * Provides CI Server information
	 *
	 * @return ServerInfo object; MUST NOT return null
	 */
	public abstract CIServerInfo getServerInfo();

	/**
	 * Provides Plugin's information
	 *
	 * @return PluginInfo object; MUST NOT return null
	 */
	public abstract CIPluginInfo getPluginInfo();

	//
	//  CONCRETE FINALS
	//

	/**
	 * Provides instance ID of the Octane Client that this instance of Plugin Services is bound to
	 *
	 * @return Octane Client instance ID
	 */
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

	//
	//  CONCRETE STUBS FOR OVERRIDES
	//

	/**
	 * Provides the folder that the plugin is allowed to write to (logs, queues, temporary stuff etc)
	 *
	 * @return File object of type Directory; if no available storage exists the implementation should return NULL
	 */
	public File getAllowedOctaneStorage() {
		return null;
	}

	/**
	 * Provides CI Server proxy configuration (managed by plugin implementation)
	 *
	 * @param targetUrl target URL that the proxy, if available, should be relevant to
	 * @return ProxyConfiguration object; if no configuration available the implementation should return NULL
	 */
	public CIProxyConfiguration getProxyConfiguration(URL targetUrl) {
		return null;
	}

	/**
	 * Provides a list of Projects existing on this CI Server
	 *
	 * @param includeParameters should the jobs data include parameters or not
	 * @param workspaceId workspace id the request comes from, This allow to show different jobs for different workspaces
	 * @return ProjectList object holding the list of the projects; MUST NOT be NULL; MAY contain an empty list
	 */
	public CIJobsList getJobsList(boolean includeParameters, Long workspaceId) {
		return null;
	}

	/**
	 * Provides Pipeline (structure) from the root CI Job
	 *
	 * @param rootJobId root Job CI ID to start pipeline from
	 * @return pipeline's structure or null if CI Job not found; MAY be NULL, meaning job with such ID not exists
	 */
	public PipelineNode getPipeline(String rootJobId) {
		return null;
	}

	/**
	 * Executes the Pipeline, running the root job
	 *
	 * @param jobId        Job CI ID to execute
	 * @param ciParameters execution parameters
	 */
	public void runPipeline(String jobId, CIParameters ciParameters) {
		throw new SPIMethodNotImplementedException("run API is not implemented");
	}

	/**
	 * Stops the Pipeline, running the root job
	 *
	 * @param jobId        Job CI ID to stop
	 * @param ciParameters execution parameters
	 */
	public void stopPipelineRun(String jobId, CIParameters ciParameters) {
		throw new SPIMethodNotImplementedException("stop API is not implemented");
	}

	/**
	 * Get build status by some parameter
	 * @param jobCiId
	 * @param parameterName
	 * @param parameterValue
	 * @return
	 */
	public CIBuildStatusInfo getJobBuildStatus(String jobCiId, String parameterName, String parameterValue) {
		throw new SPIMethodNotImplementedException("getJobBuildStatus API is not implemented");
	}

	/**
	 * Suspends events
	 *
	 * @param suspend desired state of CI events suspension
	 */
	public void suspendCIEvents(boolean suspend) {
	}

	/**
	 * Provides tests result report for the specific build
	 *
	 * @param jobId   Job CI ID to get tests results of
	 * @param buildId Build CI ID to get tests results of
	 * @return TestsResult data; NULL if no tests result available
	 */
	public InputStream getTestsResult(String jobId, String buildId) {
		return null;
	}

	/**
	 * Provides build's log as an InputStream
	 *
	 * @param jobId   job CI ID of the specific build to get log for
	 * @param buildId build CI ID to get log for
	 * @return build's log as an InputStream; NULL if no log available
	 */
	public InputStream getBuildLog(String jobId, String buildId) {
		return null;
	}

	/**
	 * Provides SCM data as an InputStream
	 *
	 * @param jobId   job CI ID of the specific build to get log for
	 * @param buildId build CI ID to get log for
	 * @return SCM data as an InputStream; NULL if no SCM data available
	 */
	public InputStream getSCMData(String jobId, String buildId) {
		return null;
	}


	/**
	 * Provides tests result report for the specific build
	 *
	 * @param jobId          Job CI ID to get tests results of
	 * @param buildId        Build CI ID to get tests results of
	 * @param reportFileName specific report file name, optional (if NULL, 0 or 1 report is expected to be provided)
	 * @return TestsResult data; NULL if no tests result available
	 */
	public InputStream getCoverageReport(String jobId, String buildId, String reportFileName) {
		return null;
	}

	/**
	 * Provides SonarQube info from CI
	 * - this API assumes that there might be only one SSC Server in the CI
	 * - if/when this assumption will be proved wrong we'll add specifying parameter to the method
	 *
	 * @return SonarQube info, if any available
	 */
	public SonarInfo getSonarInfo() {
		return null;
	}

	/**
	 * Provides SSC (Fortify on premise) server configuration
	 *
	 * @param jobId   job ID
	 * @param buildId build ID
	 * @return valid SSC project configuration; NULL if no relevant valid configuration found or if any of essentials is missing
	 */
	public SSCProjectConfiguration getSSCProjectConfiguration(String jobId, String buildId) {
		return null;
	}

	public FodServerConfiguration getFodServerConfiguration(){
		return null;
	}
	public Long getFodRelease(String jobId, String buildId){
		return null;
	}
	public void runTestDiscovery(DiscoveryInfo discoveryInfo) {
	}

	public OctaneResponse checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo) {
		return null;
	}

	public void deleteExecutor(String id) {
	}

	public OctaneResponse upsertCredentials(CredentialsInfo credentialsInfo) {
		return null;
	}

	/**
	 * Get credentials ids and names (no real passwords) for auto generation of UFT test runners
	 * @return return existing credentials
	 */
	public List<CredentialsInfo> getCredentials() {
		return Collections.emptyList();
	}

	public PipelineNode createExecutor(DiscoveryInfo discoveryInfo) {
		return null;
	}

	/**
	 * Get parent job name
	 * @param jobId jobId
	 * @return name of the parent job, or null if there is no parent
	 */
	public String getParentJobName (String jobId) {
		return null;
	}
}
