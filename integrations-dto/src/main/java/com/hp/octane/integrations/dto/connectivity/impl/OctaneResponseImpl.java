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

package com.hp.octane.integrations.dto.connectivity.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;

import java.util.Map;

/**
 * OctaneResponse DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class OctaneResponseImpl implements OctaneResponse {
	private int status;
	private Map<String, String> headers;
	private String body;
	private String correlationId;

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public OctaneResponse setStatus(int status) {
		this.status = status;
		return this;
	}

	@Override
	public Map<String, String> getHeaders() {
		return headers;
	}

	@Override
	public OctaneResponse setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	@Override
	public String getBody() {
		return body;
	}

	@Override
	public OctaneResponse setBody(String body) {
		this.body = body;
		return this;
	}

	@Override
	public String getCorrelationId() {
		return correlationId;
	}

	@Override
	public OctaneResponse setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
		return this;
	}
}
