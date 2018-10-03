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
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.spi.CIPluginServices;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Base implementation of Configuration Service API
 */

final class ConfigurationServiceImpl implements ConfigurationService {
	private static final Logger logger = LogManager.getLogger(ConfigurationServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private static final String CONNECTIVITY_STATUS_URL = "/analytics/ci/servers/connectivity/status";
	private static final String UI_CONTEXT_PATH = "/ui";
	private static final String PARAM_SHARED_SPACE = "p";

	private final CIPluginServices pluginServices;
	private final RestService restService;

	ConfigurationServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
		if (configurer == null || configurer.pluginServices == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		this.pluginServices = configurer.pluginServices;
		this.restService = restService;
		logger.info("initialized SUCCESSFULLY");
	}

	@Override
	public OctaneConfiguration buildConfiguration(String rawUrl, String apiKey, String secret) throws IllegalArgumentException {
		OctaneConfiguration result = null;
		try {
			String url;
			URL tmpUrl = new URL(rawUrl);
			int contextPathPosition = rawUrl.indexOf(UI_CONTEXT_PATH);
			if (contextPathPosition < 0) {
				throw new IllegalArgumentException("URL does not conform to the expected format");
			} else {
				url = rawUrl.substring(0, contextPathPosition);
			}
			List<NameValuePair> params = URLEncodedUtils.parse(tmpUrl.toURI(), "UTF-8");
			for (NameValuePair param : params) {
				if (param.getName().equals(PARAM_SHARED_SPACE)) {
					String[] sharedSpaceAndWorkspace = param.getValue().split("/");
					if (sharedSpaceAndWorkspace.length < 1 || sharedSpaceAndWorkspace[0].isEmpty()) {
						throw new IllegalArgumentException("shared space parameter MUST be present");
					}
					result = dtoFactory.newDTO(OctaneConfiguration.class)
							.setUrl(url)
							.setSharedSpace(sharedSpaceAndWorkspace[0])
							.setApiKey(apiKey)
							.setSecret(secret);
				}
			}
		} catch (MalformedURLException murle) {
			throw new IllegalArgumentException("invalid URL", murle);
		} catch (URISyntaxException uirse) {
			throw new IllegalArgumentException("invalid URL (parameters)", uirse);
		}

		if (result == null) {
			throw new IllegalArgumentException("failed to extract NGA server URL and shared space ID from '" + rawUrl + "'");
		} else {
			return result;
		}
	}

	@Override
	public boolean isConfigurationValid() {
		try {
			OctaneResponse response = validateConfiguration(pluginServices.getOctaneConfiguration());
			return response.getStatus() == HttpStatus.SC_OK;
		} catch (Exception e) {
			logger.error("failed to validate Octane server configuration, resolving isConfigurationValid to FALSE", e);
			return false;
		}
	}

	@Override
	public OctaneResponse validateConfiguration(OctaneConfiguration configuration) throws IOException {
		if (configuration == null) {
			throw new IllegalArgumentException("configuration MUST not be null");
		}
		if (!configuration.isValid()) {
			throw new IllegalArgumentException("configuration " + configuration + " is not valid");
		}

		URL octaneUrl = CIPluginSDKUtils.parseURL(configuration.getUrl());
		CIProxyConfiguration proxyConfiguration = pluginServices.getProxyConfiguration(octaneUrl);
		OctaneRestClient octaneRestClientImpl = restService.createOctaneRestClient(proxyConfiguration);
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setUrl(configuration.getUrl() + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + configuration.getSharedSpace() + CONNECTIVITY_STATUS_URL);
		return octaneRestClientImpl.execute(request, configuration);
	}

	@Override
	public OctaneConfiguration getOctaneConfiguration() {
		return pluginServices.getOctaneConfiguration();
	}

	@Override
	public void notifyChange() {
		logger.info("notified about Octane Server configuration change, propagating to RestService");
		restService.notifyConfigurationChange();
	}
}
