/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.octane.integrations.dto.configuration.impl;

import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;

/**
 * CIProxyConfiguration implementaion
 */

class CIProxyConfigurationImpl implements CIProxyConfiguration {
	private String host;
	private Integer port;
	private String username;
	private String password;

	public String getHost() {
		return host;
	}

	public CIProxyConfiguration setHost(String host) {
		this.host = host;
		return this;
	}

	public Integer getPort() {
		return port;
	}

	public CIProxyConfiguration setPort(Integer port) {
		this.port = port;
		return this;
	}

	public String getUsername() {
		return username;
	}

	public CIProxyConfiguration setUsername(String username) {
		this.username = username;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public CIProxyConfiguration setPassword(String password) {
		this.password = password;
		return this;
	}

	@Override
	public String toString() {
		return "CIProxyConfigurationImpl { " +
				"host=" + host +
				", port=" + port +
				", username=" + username + " }";
	}
}
