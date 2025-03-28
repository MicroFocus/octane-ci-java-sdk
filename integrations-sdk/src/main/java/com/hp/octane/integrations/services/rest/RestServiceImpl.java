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
package com.hp.octane.integrations.services.rest;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.function.Function;

/**
 * REST Service - default implementation
 */

final class RestServiceImpl implements RestService {
	private static final Logger logger = LogManager.getLogger(RestServiceImpl.class);
	private final Object DEFAULT_CLIENT_INIT_LOCK = new Object();
	private final Object SSC_CLIENT_INIT_LOCK = new Object();

	private final OctaneSDK.SDKServicesConfigurer configurer;
	private OctaneRestClientImpl defaultClient;
	private SSCRestClient sscRestClient;

	RestServiceImpl(OctaneSDK.SDKServicesConfigurer configurer) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}

		this.configurer = configurer;

		logger.info(configurer.octaneConfiguration.getLocationForLog() + "initializing a default Octane REST client");
		obtainOctaneRestClient();
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "default Octane REST client is initialized");
	}

	@Override
	public Function<URL, CIProxyConfiguration> getProxySupplier() {
		return configurer.pluginServices::getProxyConfiguration;
	}

	@Override
	public OctaneRestClient obtainOctaneRestClient() {
		if (defaultClient == null) {
			synchronized (DEFAULT_CLIENT_INIT_LOCK) {
				if (null == defaultClient) {
					try {
						defaultClient = new OctaneRestClientImpl(configurer);
					} catch (Exception e) {
						logger.error(configurer.octaneConfiguration.getLocationForLog() + "failed to initialize Octane's REST client");
					}
				}
			}
		}
		return defaultClient;
	}

	@Override
	public SSCRestClient obtainSSCRestClient() {
		if (sscRestClient == null) {
			synchronized (SSC_CLIENT_INIT_LOCK) {
				if (null == sscRestClient) {
					try {
						sscRestClient = new SSCRestClientImpl(configurer);
					} catch (Exception e) {
						logger.error(configurer.octaneConfiguration.getLocationForLog() + "failed to initialize Octane's REST client");
					}
				}
			}
		}
		return sscRestClient;
	}

	@Override
	public void notifyConfigurationChange() {
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "connectivity configuration change has been notified; publishing to the RestClients");
		if (defaultClient != null) {
			defaultClient.notifyConfigurationChange();
		} else {
			logger.error(configurer.octaneConfiguration.getLocationForLog() + "default client was not yet initialized");
		}
	}
}
