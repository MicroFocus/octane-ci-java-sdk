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

package com.hp.octane.integrations.dto.connectivity;

import com.hp.octane.integrations.dto.DTOBase;

import java.io.InputStream;
import java.util.Map;

/**
 * REST Request DTO (normalized to ALM Octane usages)
 */

public interface OctaneRequest extends DTOBase {

	String getUrl();

	OctaneRequest setUrl(String url);

	HttpMethod getMethod();

	OctaneRequest setMethod(HttpMethod method);

	Map<String, String> getHeaders();

	OctaneRequest setHeaders(Map<String, String> headers);

	InputStream getBody();

	OctaneRequest setBody(InputStream bodyAsStream);

	OctaneRequest setBody(String body);
}
