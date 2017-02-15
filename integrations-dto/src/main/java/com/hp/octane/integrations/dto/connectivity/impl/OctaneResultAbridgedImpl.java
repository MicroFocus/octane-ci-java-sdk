/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.dto.connectivity.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.connectivity.OctaneResultAbridged;

import java.util.Map;

/**
 * Created by gullery on 08/01/2016.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class OctaneResultAbridgedImpl implements OctaneResultAbridged {
	private String id;
	private String serviceId;
	private int status;
	private Map<String, String> headers;
	private String body;

	public String getId() {
		return id;
	}

	public OctaneResultAbridged setId(String id) {
		this.id = id;
		return this;
	}

	public String getServiceId() {
		return serviceId;
	}

	public OctaneResultAbridged setServiceId(String serviceId) {
		this.serviceId = serviceId;
		return this;
	}

	public int getStatus() {
		return status;
	}

	public OctaneResultAbridged setStatus(int status) {
		this.status = status;
		return this;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public OctaneResultAbridged setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public String getBody() {
		return body;
	}

	public OctaneResultAbridged setBody(String body) {
		this.body = body;
		return this;
	}
}
