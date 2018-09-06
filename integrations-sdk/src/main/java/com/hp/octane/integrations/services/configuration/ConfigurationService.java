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
 */

package com.hp.octane.integrations.services.configuration;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;

import java.io.IOException;

public interface ConfigurationService {

	/**
	 * Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer  SDK services configurer object
	 * @param restService Rest Service
	 * @return initialized service
	 */
	static ConfigurationService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
		return new ConfigurationServiceImpl(configurer, restService);
	}

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
	 * Check if current configuration is valid
	 */
	boolean isConfigurationValid();

	/**
	 * Notify SDK upon Octane configuration change
	 */
	void notifyChange();
}