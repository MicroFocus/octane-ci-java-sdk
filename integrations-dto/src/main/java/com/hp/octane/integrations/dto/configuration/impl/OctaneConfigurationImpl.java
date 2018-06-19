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

package com.hp.octane.integrations.dto.configuration.impl;

import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Octane Server configuration descriptor
 */

class OctaneConfigurationImpl implements OctaneConfiguration {
	private String url;
	private String sharedSpace;
	private String apiKey;
	private String secret;

	public String getUrl() {
		return url;
	}

	public OctaneConfiguration setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getSharedSpace() {
		return sharedSpace;
	}

	public OctaneConfiguration setSharedSpace(String sharedSpace) {
		this.sharedSpace = sharedSpace;
		return this;
	}

	public String getApiKey() {
		return apiKey;
	}

	public OctaneConfiguration setApiKey(String apiKey) {
		this.apiKey = apiKey;
		return this;
	}

	public String getSecret() {
		return secret;
	}

	public OctaneConfiguration setSecret(String secret) {
		this.secret = secret;
		return this;
	}

	public boolean isValid() {
		boolean result = false;
		if (url != null && !url.isEmpty() && sharedSpace != null && !sharedSpace.isEmpty()) {
			try {
				URL tmp = new URL(url);
				result = true;
			} catch (MalformedURLException mue) {
				result = false;
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "NGAConfigurationImpl { " +
				"url: " + url +
				", sharedSpace: " + sharedSpace +
				", apiKey: " + apiKey + " }";
	}
}
