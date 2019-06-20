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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OctaneRequest implementation
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class OctaneRequestImpl implements OctaneRequest {
	private String url;
	private HttpMethod method;
	private Map<String, String> headers;
	private InputStream body;

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public OctaneRequest setUrl(String url) {
		if (url == null || url.isEmpty()) {
			throw new IllegalArgumentException("URL MUST NOT be null nor empty");
		}
		try {
			new URL(url);
		} catch (MalformedURLException mue) {
			throw new IllegalArgumentException("URL argument is not valid", mue);
		}
		this.url = url;
		return this;
	}

	@Override
	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public OctaneRequest setMethod(HttpMethod method) {
		if (method == null) {
			throw new IllegalArgumentException("method MUST NOT be null");
		}
		this.method = method;
		return this;
	}

	@Override
	public Map<String, String> getHeaders() {
		return headers;
	}

	@Override
	public OctaneRequest setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	@Override
	public InputStream getBody() {
		return body;
	}

	@JsonIgnore
	@Override
	public OctaneRequest setBody(InputStream body) {
		this.body = body;
		return this;
	}

	@JsonProperty
	@Override
	public OctaneRequest setBody(String body) {
		this.body = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
		return this;
	}
}
