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

package com.hp.octane.integrations.api;

import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;

import java.io.IOException;

public interface ConfigurationService {

	/**
	 * Builds configuration object from raw data, usually supplied from UI or storage
	 *
	 * @param rawUrl Octane server url
	 * @param apiKey API Key
	 * @param secret API Secret
	 * @return OctaneConfiguration
	 */
	OctaneConfiguration buildConfiguration(String rawUrl, String apiKey, String secret) throws IllegalArgumentException;

	/**
	 * Tests connectivity to the Octane server with the supplied configuration
	 *
	 * @param configuration Octane configuration
	 * @return OctaneResponse
	 * @throws IOException in case of connection failure
	 */
	OctaneResponse validateConfiguration(OctaneConfiguration configuration) throws IOException;

	/**
	 * Notify SDK upon Octane configuration change
	 */
	void notifyChange();
}
