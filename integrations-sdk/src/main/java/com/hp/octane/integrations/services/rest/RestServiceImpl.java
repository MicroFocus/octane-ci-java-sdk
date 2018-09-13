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

package com.hp.octane.integrations.services.rest;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.SSCClient;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * REST Service - default implementation
 */

final class RestServiceImpl implements RestService {
	private static final Logger logger = LogManager.getLogger(RestServiceImpl.class);
	private final Object DEFAULT_CLIENT_INIT_LOCK = new Object();
	private final Object SSC_CLIENT_INIT_LOCK = new Object();

	private final OctaneSDK.SDKServicesConfigurer configurer;
	private RestClientImpl defaultClient;
	private SSCClient sscClient;

	RestServiceImpl(OctaneSDK.SDKServicesConfigurer configurer) {
		if (configurer == null || configurer.pluginServices == null) {
			throw new IllegalArgumentException("invalid configurer");
		}

		this.configurer = configurer;

		logger.info("initializing a default Octane REST client...");
		obtainOctaneRestClient();
		logger.info("...default Octane REST client initialized");
	}

	@Override
	public RestClient obtainOctaneRestClient() {
		if (defaultClient == null) {
			synchronized (DEFAULT_CLIENT_INIT_LOCK) {
				if (defaultClient == null) {
					try {
						defaultClient = new RestClientImpl(configurer);
					} catch (Exception e) {
						logger.error("failed to initialize Octane's REST client");
					}
				}
			}
		}
		return defaultClient;
	}

	@Override
	public SSCClient obtainSSCRestClient() {
		if (sscClient == null) {
			synchronized (SSC_CLIENT_INIT_LOCK) {
				if (sscClient == null) {
					try {
						sscClient = new SSCClientImpl();
					} catch (Exception e) {
						logger.error("failed to initialize Octane's REST client");
					}
				}
			}
		}
		return sscClient;
	}

	@Override
	public RestClient createOctaneRestClient(CIProxyConfiguration proxyConfiguration) {
		return new RestClientImpl(configurer);
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
