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

package com.hp.octane.integrations.dto.connectivity.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneTaskAbridged;

import java.util.Map;

/**
 * OctaneTaskAbridged implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class OctaneTaskAbridgedImpl implements OctaneTaskAbridged {
	private String id;
	private String serviceId;
	private String url;
	private HttpMethod method;
	private Map<String, String> headers;
	private String body;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public OctaneTaskAbridged setId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public OctaneTaskAbridged setServiceId(String serviceId) {
		this.serviceId = serviceId;
		return this;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public OctaneTaskAbridged setUrl(String url) {
		this.url = url;
		return this;
	}

	@Override
	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public OctaneTaskAbridged setMethod(HttpMethod method) {
		this.method = method;
		return this;
	}

	@Override
	public Map<String, String> getHeaders() {
		return headers;
	}

	@Override
	public OctaneTaskAbridged setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	@Override
	public String getBody() {
		return body;
	}

	@Override
	public OctaneTaskAbridged setBody(String body) {
		this.body = body;
		return this;
	}
}
