/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.services.configuration;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.general.OctaneConnectivityStatus;
import com.hp.octane.integrations.services.HasMetrics;
import com.hp.octane.integrations.services.rest.RestService;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Future;

public interface ConfigurationService extends HasMetrics {

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
	OctaneConfiguration getConfiguration();

	/**
	 * Get connectivity status : octane version, supported SDK version
	 * @return octane connectivity status
	 */
	OctaneConnectivityStatus getOctaneConnectivityStatus();

	/**
	 * Tests connectivity to the Octane server with the supplied configuration
	 * @return OctaneConnectivityStatus
	 * @throws IOException throw if configuration is not valid
	 */
	OctaneConnectivityStatus validateConfigurationAndGetConnectivityStatus() throws IOException;

	boolean isOctaneVersionGreaterOrEqual(String version);

	boolean isConnected();

	Collection<String> getOctaneRootsCacheCollection();

	Future<Boolean> resetOctaneRootsCache();

	void addToOctaneRootsCache(String rootJob);

	boolean removeFromOctaneRoots(String rootJob);

	boolean isRelevantForOctane(Collection<String> rootJobs);
}
