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

package com.hp.octane.integrations.api;

import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;

public interface RestService {
	String SHARED_SPACE_INTERNAL_API_PATH_PART = "/internal-api/shared_spaces/";
	String SHARED_SPACE_API_PATH_PART = "/api/shared_spaces/";
	String ANALYTICS_CI_PATH_PART = "/analytics/ci/";
	String VULNERABILITIES = "/vulnerabilities";
	String ACCEPT_HEADER = "accept";
	String CONTENT_TYPE_HEADER = "content-type";
	String CONTENT_ENCODING_HEADER = "content-encoding";
	String GZIP_ENCODING = "gzip";

	/**
	 * Retrieves default REST client: the one initialized with plugin's provided configuration and listening on it changes
	 *
	 * @return pre-configured RestClient
	 */
	RestClient obtainClient();

	/**
	 * Creates new REST client pre-configured with the specified configuration
	 *
	 * @param proxyConfiguration optional proxy configuration, if relevant
	 * @return pre-configured RestClient
	 */
	RestClient createClient(CIProxyConfiguration proxyConfiguration);

	/**
	 * Notifies the service that configuration has been changed
	 */
	void notifyConfigurationChange();
}
