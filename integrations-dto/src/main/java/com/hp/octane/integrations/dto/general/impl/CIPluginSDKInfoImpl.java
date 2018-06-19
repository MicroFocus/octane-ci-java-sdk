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

package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.CIPluginSDKInfo;

/**
 * CIPluginSDKInfo DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class CIPluginSDKInfoImpl implements CIPluginSDKInfo {
	private Integer apiVersion;
	private String sdkVersion;

	public Integer getApiVersion() {
		return apiVersion;
	}

	public CIPluginSDKInfo setApiVersion(Integer apiVersion) {
		this.apiVersion = apiVersion;
		return this;
	}

	public String getSdkVersion() {
		return sdkVersion;
	}

	public CIPluginSDKInfo setSdkVersion(String sdkVersion) {
		this.sdkVersion = sdkVersion;
		return this;
	}
}
