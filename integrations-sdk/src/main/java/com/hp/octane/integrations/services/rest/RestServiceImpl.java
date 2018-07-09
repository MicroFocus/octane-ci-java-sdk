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

package com.hp.octane.integrations.services.rest;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.RestClient;
import com.hp.octane.integrations.api.RestService;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * REST Service - default implementation
 */

public final class RestServiceImpl extends OctaneSDK.SDKServiceBase implements RestService {
	private static final Logger logger = LogManager.getLogger(RestServiceImpl.class);
	private static final Object DEFAULT_CLIENT_INIT_LOCK = new Object();

	private RestClientImpl defaultClient;

	public RestServiceImpl(Object internalUsageValidator) {
		super(internalUsageValidator);

		logger.info("initializing a default Octane REST client...");
		obtainClient();
		logger.info("...default Octane REST client initialized");
	}

	public RestClient obtainClient() {
		if (defaultClient == null) {
			synchronized (DEFAULT_CLIENT_INIT_LOCK) {
				if (defaultClient == null) {
					try {
						defaultClient = new RestClientImpl(pluginServices);
					} catch (Exception e) {
						logger.error("failed to initialize Octane's REST client");
					}
				}
			}
		}
		return defaultClient;
	}

	public RestClient createClient(CIProxyConfiguration proxyConfiguration) {
		return new RestClientImpl(pluginServices);
	}

	@Override
	public void notifyConfigurationChange() {
		logger.info("connectivity configuration change has been notified; publishing to the RestClients");
		if (defaultClient != null) {
			defaultClient.notifyConfigurationChange();
		} else {
			logger.error("default client was not yet initialized");
		}
	}
}
