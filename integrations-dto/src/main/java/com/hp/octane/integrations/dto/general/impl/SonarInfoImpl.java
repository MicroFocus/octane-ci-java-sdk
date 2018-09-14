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

package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.SonarInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarInfoImpl implements SonarInfo {
	private String serverUrl;
	private String serverVersion;
	private String serverAuthenticationToken;

	public SonarInfoImpl() {

	}

	public SonarInfoImpl(String serverUrl, String serverVersion, String serverAuthenticationToken) {
		this.serverUrl = serverUrl;
		this.serverVersion = serverVersion;
		this.serverAuthenticationToken = serverAuthenticationToken;
	}

	@Override
	public String getServerUrl() {
		return serverUrl;
	}

	@Override
	public SonarInfo setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
		return this;
	}

	@Override
	public String getServerVersion() {
		return serverVersion;
	}

	@Override
	public SonarInfo setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
		return this;
	}

	@Override
	public String getServerAuthenticationToken() {
		return serverAuthenticationToken;
	}

	@Override
	public SonarInfo setServerAuthenticationToken(String serverAuthenticationToken) {
		this.serverAuthenticationToken = serverAuthenticationToken;
		return this;
	}
}
