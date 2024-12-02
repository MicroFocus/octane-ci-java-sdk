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

import java.net.URL;
import java.util.function.Function;

public interface RestService {
	String SHARED_SPACE_INTERNAL_API_PATH_PART = "/internal-api/shared_spaces/";
	String SHARED_SPACE_API_PATH_PART = "/api/shared_spaces/";
	String ANALYTICS_CI_PATH_PART = "/analytics/ci/";
	String VULNERABILITIES = "/vulnerabilities";
	String VULNERABILITIES_PRE_FLIGHT = "/vulnerabilities/preflight";
	String OPEN_VULNERABILITIES_FROM_OCTANE = "/vulnerabilities/remote-issue-ids";
	String ACCEPT_HEADER = "accept";
	String CORRELATION_ID_HEADER = "X-Correlation-ID";
	String CONTENT_TYPE_HEADER = "content-type";
	String CONTENT_ENCODING_HEADER = "content-encoding";
	String GZIP_ENCODING = "gzip";
	String SCMDATA_API_PATH_PART = "/scm-commits";

	/**
	 * Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer SDK services configurer object
	 * @return initialized service
	 */
	static RestService newInstance(OctaneSDK.SDKServicesConfigurer configurer) {
		return new RestServiceImpl(configurer);
	}

	/**
	 * Get proxy supplier
	 * @return proxy supplier
	 */
	Function<URL, CIProxyConfiguration> getProxySupplier();

	/**
	 * Retrieves default REST client: the one initialized with plugin's provided configuration and listening on it changes
	 *
	 * @return pre-configured RestClient
	 */
	OctaneRestClient obtainOctaneRestClient();

	SSCRestClient obtainSSCRestClient();

	/**
	 * Notifies the service that configuration has been changed
	 */
	void notifyConfigurationChange();
}
