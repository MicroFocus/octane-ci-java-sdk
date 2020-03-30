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

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.general.OctaneConnectivityStatus;
import com.hp.octane.integrations.services.rest.RestService;

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
	 * Provides current OctaneConfiguration that THIS OctaneClient instance is configured with
	 * - ATTENTION: this is a LIVE configuration object, any changes to its properties will have immediate or almost immediate effect on THIS OctaneClient instance
	 *
	 * @return current Octane Configuration
	 */
	OctaneConfiguration getCurrentConfiguration();

	/**
	 * Get connectivity status : octane version, supported SDK version
	 * @param forceFetch fetch from octane and not use cache value
	 * @return octane connectivity status
	 */
	OctaneConnectivityStatus getOctaneConnectivityStatus(boolean forceFetch);

	/**
	 * Tests connectivity to the Octane server with the supplied configuration
	 * @return OctaneConnectivityStatus
	 * @throws IOException throw if configuration is not valid
	 */
	OctaneConnectivityStatus validateConfigurationAndGetConnectivityStatus() throws IOException;
}
