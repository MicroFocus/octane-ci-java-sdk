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

package com.hp.octane.integrations.services.sonar;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.exceptions.SonarIntegrationException;
import com.hp.octane.integrations.services.ClosableService;
import com.hp.octane.integrations.services.HasMetrics;
import com.hp.octane.integrations.services.HasQueueService;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.coverage.CoverageService;
import com.hp.octane.integrations.services.queueing.QueueingService;

/**
 * Sonar service provides an integration functionality related to SonarQube
 * It is a full responsibility of this service to
 * - manage its own queue items
 * - call preflight API to ensure the data is relevant to Octane
 * - retrieve relevant content from Sonar server
 * - push relevant convent to Octane
 */

public interface SonarService extends ClosableService, HasQueueService, HasMetrics {

	/**
	 * Sonar integration Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer SDK services configurer object
	 * @param queueingService queueingService
	 * @param coverageService coverageService
	 * @param configurationService Configuration Service
	 * @return initialized service

	 */
	static SonarService newInstance(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService,
									CoverageService coverageService, ConfigurationService configurationService) {
		return new SonarServiceImpl(configurer, queueingService, coverageService, configurationService);
	}

	/**
	 * get status object from a SonarQube server listens to sonarURL, as specified in sonar documentation :
	 * <p>
	 * STARTING: SonarQube Web Server is up and serving some Web Services (eg. api/system/status) but initialization is still ongoing
	 * UP: SonarQube instance is up and running
	 * DOWN: SonarQube instance is up but not running because migration has failed (refer to WS /api/system/migrate_db for details) or some other reason (check logs).
	 * RESTARTING: SonarQube instance is still up but a restart has been requested (refer to WS /api/system/restart for details).
	 * DB_MIGRATION_NEEDED: database migration is required. DB migration can be started using WS /api/system/migrate_db.
	 * DB_MIGRATION_RUNNING: DB migration is running (refer to WS /api/system/migrate_db for details)
	 * </p>
	 * in case of connection failure the status will be:
	 * CONNECTION_FAILURE
	 *
	 * @param sonarURL target Sonar server
	 * @return Sonar server status
	 */
	String getSonarStatus(String sonarURL);

	/**
	 * enqueue FetchAndPushSonarCoverageToOctane task, the fetch coverage data for projectKey from sonarQube server associated with sonarURL, and inject it to Octane
	 * the ci parameters will be sent to octane as parameters
	 *
	 * @param jobId      CI Job ID
	 * @param buildId    CI Build ID
	 * @param projectKey Key of a project in Sonar server
	 * @param sonarURL   Sonar server URL
	 * @param sonarToken Sonar server authentication token
	 * @param rootJobId rootJobId
	 */
	void enqueueFetchAndPushSonarCoverage(String jobId, String buildId, String projectKey, String sonarURL, String sonarToken, String rootJobId);

	/**
	 * ensure that webhook with the ciCallbackUrl exist in the Sonar server
	 * if no webhook with ciCallbackUrl is found, new webhook will be created
	 *
	 * @param ciCallbackUrl URL of an endpoint in CI Server that will receive the callback
	 * @param sonarURL      Sonar server URL
	 * @param sonarToken    Sonar server authentication token
	 * @throws SonarIntegrationException Sonar integration exception
	 */
	void ensureSonarWebhookExist(String ciCallbackUrl, String sonarURL, String sonarToken) throws SonarIntegrationException;

}
