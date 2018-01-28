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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;

import java.io.InputStream;
import java.util.Map;

/**
 * OctaneRequest implementation
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class OctaneRequestImpl implements OctaneRequest {
	private String url;
	private HttpMethod method;
	private Map<String, String> headers;
	private String body;
	private InputStream bodyAsStream;

	public String getUrl() {
		return url;
	}

	public OctaneRequest setUrl(String url) {
		this.url = url;
		return this;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public OctaneRequest setMethod(HttpMethod method) {
		this.method = method;
		return this;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public OctaneRequest setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public String getBody() {
		return body;
	}

	public OctaneRequest setBody(String body) {
		if (bodyAsStream != null) {
			throw new IllegalStateException("request MAY be used only with body as STRING or as INPUT STREAM, but not both; this request already has INPUT STREAM body (not-null)");
		}
		this.body = body;
		return this;
	}

	@JsonIgnore
	public InputStream getBodyAsStream() {
		return bodyAsStream;
	}

	@JsonIgnore
	public OctaneRequest setBodyAsStream(InputStream bodyAsStream) {
		if (body != null) {
			throw new IllegalStateException("request MAY be used only with body as STRING or as INPUT STREAM, but not both; this request already has STRING body '" + body + "'");
		}
		this.bodyAsStream = bodyAsStream;
		return this;
	}
}
