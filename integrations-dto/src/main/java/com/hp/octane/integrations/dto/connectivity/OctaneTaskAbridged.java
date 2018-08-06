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

import java.io.Serializable;
import java.util.Map;

/**
 * Task container DTO, as to be used in abridged tasking in ALM Octane
 */

public interface OctaneTaskAbridged extends DTOBase, Serializable {

	String getId();

	OctaneTaskAbridged setId(String id);

	String getServiceId();

	OctaneTaskAbridged setServiceId(String serviceId);

	String getUrl();

	OctaneTaskAbridged setUrl(String url);

	HttpMethod getMethod();

	OctaneTaskAbridged setMethod(HttpMethod method);

	Map<String, String> getHeaders();

	OctaneTaskAbridged setHeaders(Map<String, String> headers);

	String getBody();

	OctaneTaskAbridged setBody(String body);
}
