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
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.general.OctaneConnectivityStatus;
import com.hp.octane.integrations.exceptions.OctaneConnectivityException;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation of Configuration Service API
 */

final class ConfigurationServiceImpl implements ConfigurationService {
	private static final Logger logger = LogManager.getLogger(ConfigurationServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private static final String CONNECTIVITY_STATUS_URL = "/analytics/ci/servers/connectivity/status";

	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;
	private OctaneConnectivityStatus octaneConnectivityStatus;
	private long octaneConnectivityStatusDate;

	ConfigurationServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		this.configurer = configurer;
		this.restService = restService;
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "initialized SUCCESSFULLY");
	}

	@Override
	public OctaneConfiguration getCurrentConfiguration() {
		return configurer.octaneConfiguration;
	}

	@Override
	public OctaneConnectivityStatus getOctaneConnectivityStatus(boolean forceFetch) {

		try {
			if (forceFetch || octaneConnectivityStatus == null || isLastUpdateDone24HBefore()) {
				octaneConnectivityStatus = validateConfigurationAndGetConnectivityStatusInternal(configurer.octaneConfiguration);
				octaneConnectivityStatusDate = System.currentTimeMillis();
			}
		} catch (Exception e) {
			logger.error(configurer.octaneConfiguration.geLocationForLog() + "failed to getOctaneConnectivityStatus : " + e.getMessage());
		}

		return octaneConnectivityStatus;
	}

	private boolean isLastUpdateDone24HBefore() {
		long diffInMillies = System.currentTimeMillis() - octaneConnectivityStatusDate;
		long diffInHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
		return diffInHours > 24;
	}

	@Override
	public OctaneConnectivityStatus validateConfigurationAndGetConnectivityStatus() throws IOException {
		return validateConfigurationAndGetConnectivityStatusInternal(configurer.octaneConfiguration);
	}

	private OctaneConnectivityStatus validateConfigurationAndGetConnectivityStatusInternal(OctaneConfiguration configuration) throws IOException {
		if (configuration == null) {
			throw new IllegalArgumentException("configuration MUST not be null");
		}

		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setUrl(configuration.getUrl() + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + configuration.getSharedSpace() + CONNECTIVITY_STATUS_URL);

		OctaneResponse response = restService.obtainOctaneRestClient().execute(request, configuration);
		if (response.getStatus() == 401) {
			throw new OctaneConnectivityException(response.getStatus(), OctaneConnectivityException.AUTHENTICATION_FAILURE_KEY, OctaneConnectivityException.AUTHENTICATION_FAILURE_MESSAGE);
		} else if (response.getStatus() == 403) {
			throw new OctaneConnectivityException(response.getStatus(), OctaneConnectivityException.AUTHORIZATION_FAILURE_KEY, OctaneConnectivityException.AUTHORIZATION_FAILURE_MESSAGE);
		} else if (response.getStatus() == 404) {
			throw new OctaneConnectivityException(response.getStatus(), OctaneConnectivityException.CONN_SHARED_SPACE_INVALID_KEY, OctaneConnectivityException.CONN_SHARED_SPACE_INVALID_MESSAGE);
		} else if (response.getStatus() == 200) {
			OctaneConnectivityStatus octaneConnectivityStatus = DTOFactory.getInstance().dtoFromJson(response.getBody(), OctaneConnectivityStatus.class);
			return octaneConnectivityStatus;
		} else {
			throw new OctaneConnectivityException(response.getStatus(), OctaneConnectivityException.UNEXPECTED_FAILURE_KEY, OctaneConnectivityException.UNEXPECTED_FAILURE_MESSAGE + ": " + response.getStatus());
		}
	}
}
