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
import com.hp.octane.integrations.dto.securityscans.SSCServerInfo;

/**
 * SSCServerInfo DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class SSCServerInfoImpl implements SSCServerInfo {
	private volatile String sscURL;
	private volatile String sscBaseAuthToken;
	private volatile long maxPollingTimeoutHours;

	@Override
	public String getSSCURL() {
		return sscURL;
	}

	@Override
	public SSCServerInfo setSSCURL(String sscUrl) {
		this.sscURL = sscUrl;
		return this;
	}

	@Override
	public String getSSCBaseAuthToken() {
		return sscBaseAuthToken;
	}

	@Override
	public SSCServerInfo setSSCBaseAuthToken(String sscBaseAuthToken) {
		this.sscBaseAuthToken = sscBaseAuthToken;
		return this;
	}

	@Override
	public long getMaxPollingTimeoutHours() {
		return maxPollingTimeoutHours;
	}

	@Override
	public SSCServerInfo setMaxPollingTimeoutHours(long maxPollingTimeoutHours) {
		this.maxPollingTimeoutHours = maxPollingTimeoutHours;
		return this;
	}
}
