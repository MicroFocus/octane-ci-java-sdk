/*
 *     © 2017 EntIT Software LLC, a Micro Focus company, L.P.
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
import com.hp.octane.integrations.dto.general.CIServerTypes;
import com.hp.octane.integrations.dto.general.CIServerInfo;

/**
 * CIServerInfo DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class CIServerInfoImpl implements CIServerInfo {
	private String type;
	private boolean suspended;
	private String version;
	private String url;
	private String instanceId;
	private Long instanceIdFrom;
	private String impersonatedUser;
	private Long sendingTime = System.currentTimeMillis();

	public CIServerInfoImpl() {
	}

	public CIServerInfoImpl(String type, String version, String url, String instanceId, Long instanceIdFrom) {
		this.type = type;
		this.version = version;
		this.url = normalizeURL(url);
		this.instanceId = instanceId;
		this.instanceIdFrom = instanceIdFrom;
	}

	public String getType() {
		return type;
	}

	public CIServerInfo setType(String type) {
		this.type = type;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public CIServerInfo setVersion(String version) {
		this.version = version;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public CIServerInfo setUrl(String url) {
		this.url = normalizeURL(url);
		return this;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public CIServerInfo setInstanceId(String instanceId) {
		this.instanceId = instanceId;
		return this;
	}

	public Long getInstanceIdFrom() {
		return instanceIdFrom;
	}

	public CIServerInfo setInstanceIdFrom(Long instanceIdFrom) {
		this.instanceIdFrom = instanceIdFrom;
		return this;
	}

	public Long getSendingTime() {
		return sendingTime;
	}

	public CIServerInfo setSendingTime(Long sendingTime) {
		this.sendingTime = sendingTime;
		return this;
	}

	@Override
	public String getImpersonatedUser() {
		return impersonatedUser;
	}

	@Override
	public CIServerInfo setImpersonatedUser(String impersonatedUser) {
		this.impersonatedUser = impersonatedUser;
		return this;
	}

	@Override
	public boolean isSuspended() {
		return this.suspended;
	}

	@Override
	public CIServerInfo setSuspended(boolean suspended) {
		this.suspended = suspended;
		return this;
	}

	private String normalizeURL(String input) {
		String result;
		if (input != null && input.endsWith("/")) {
			result = input.substring(0, input.length() - 1);
		} else {
			result = input;
		}
		return result;
	}
}
