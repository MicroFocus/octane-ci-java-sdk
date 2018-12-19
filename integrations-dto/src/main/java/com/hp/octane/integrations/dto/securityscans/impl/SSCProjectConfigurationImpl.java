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

package com.hp.octane.integrations.dto.securityscans.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;

/**
 * SSCServerInfo DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class SSCProjectConfigurationImpl implements SSCProjectConfiguration {
	private String sscURL;
	private String sscBaseAuthToken;
	private String projectName;
	private String projectVersion;
	private long maxPollingTimeoutHours;

	@Override
	public String getSSCUrl() {
		return sscURL;
	}

	@Override
	public SSCProjectConfiguration setSSCUrl(String sscUrl) {
		this.sscURL = sscUrl;
		return this;
	}

	@Override
	public String getSSCBaseAuthToken() {
		return sscBaseAuthToken;
	}

	@Override
	public SSCProjectConfiguration setSSCBaseAuthToken(String sscBaseAuthToken) {
		this.sscBaseAuthToken = sscBaseAuthToken;
		return this;
	}

	@Override
	public String getProjectName() {
		return projectName;
	}

	@Override
	public SSCProjectConfiguration setProjectName(String projectName) {
		this.projectName = projectName;
		return this;
	}

	@Override
	public String getProjectVersion() {
		return projectVersion;
	}

	@Override
	public SSCProjectConfiguration setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
		return this;
	}

	@Override
	public long getMaxPollingTimeoutHours() {
		return maxPollingTimeoutHours;
	}

	@Override
	public SSCProjectConfiguration setMaxPollingTimeoutHours(long maxPollingTimeoutHours) {
		this.maxPollingTimeoutHours = maxPollingTimeoutHours;
		return this;
	}

	@Override
	public String getRemoteTag() {return getProjectName()+getProjectVersion();}

	@Override
	public boolean isValid() {
		return sscURL != null && !sscURL.isEmpty() &&
				projectName != null && !projectName.isEmpty() &&
				projectVersion != null && !projectVersion.isEmpty();
	}
}
