/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
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
package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.CIServerInfo;

/**
 * CIServerInfo DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class CIServerInfoImpl implements CIServerInfo {
	private String type;
	private String version;
	private String url;
	private String instanceId;
	private Long instanceIdFrom;
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

	@Override
	public String getType() {
		return type;
	}

	@Override
	public CIServerInfo setType(String type) {
		this.type = type;
		return this;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public CIServerInfo setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public CIServerInfo setUrl(String url) {
		this.url = normalizeURL(url);
		return this;
	}

	@Override
	public String getInstanceId() {
		return instanceId;
	}

	@Override
	public CIServerInfo setInstanceId(String instanceId) {
		this.instanceId = instanceId;
		return this;
	}

	@Override
	public Long getInstanceIdFrom() {
		return instanceIdFrom;
	}

	@Override
	public CIServerInfo setInstanceIdFrom(Long instanceIdFrom) {
		this.instanceIdFrom = instanceIdFrom;
		return this;
	}

	@Override
	public Long getSendingTime() {
		return sendingTime;
	}

	@Override
	public CIServerInfo setSendingTime(Long sendingTime) {
		this.sendingTime = sendingTime;
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
