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

package com.hp.octane.integrations.services.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.coverage.BuildCoverage;
import com.hp.octane.integrations.dto.coverage.CoverageReportType;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.SonarIntegrationException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.VulnerabilitiesQueueItem;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementations of Sonar service
 */

class SonarServiceImpl implements SonarService {
	private static final Logger logger = LogManager.getLogger(SonarServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String SONAR_COVERAGE_QUEUE_FILE = "sonar-coverage-queue.dat";
	private static final String WEBHOOK_CREATE_URI = "/api/webhooks/create";
	private static final String WEBHOOK_LIST_URI = "/api/webhooks/list";
	private static final String SONAR_STATUS_URI = "/api/system/status";
	private static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";
	private static final String COMPONENT_TREE_URI = "/api/measures/component_tree";

	private final ExecutorService sonarIntegrationExecutor = Executors.newSingleThreadExecutor(new SonarIntegrationWorkerThreadFactory());
	private final Object NO_SONAR_COVERAGE_ITEMS_MONITOR = new Object();
	private final ObjectQueue<SonarBuildCoverageQueueItem> sonarIntegrationQueue;
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final CoverageService coverageService;
	private final RestService restService;

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;
	private int LIST_EMPTY_INTERVAL = 3000;

	SonarServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, CoverageService coverageService, RestService restService) {
		if (configurer == null || configurer.pluginServices == null || configurer.octaneConfiguration == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (queueingService == null) {
			throw new IllegalArgumentException("queue service MUST NOT be null");
		}
		if (coverageService == null) {
			throw new IllegalArgumentException("coverage service MUST NOT be null");
		}

		this.configurer = configurer;
		this.coverageService = coverageService;
		this.restService = restService;

		if (queueingService.isPersistenceEnabled()) {
			sonarIntegrationQueue = queueingService.initFileQueue(SONAR_COVERAGE_QUEUE_FILE, SonarBuildCoverageQueueItem.class);
		} else {
			sonarIntegrationQueue = queueingService.initMemoQueue();
		}

		logger.info("starting background worker...");
		sonarIntegrationExecutor.execute(this::worker);
		logger.info("initialized SUCCESSFULLY (backed by " + sonarIntegrationQueue.getClass().getSimpleName() + ")");
	}

	// infallible everlasting background worker
	private void worker() {
		while (!sonarIntegrationExecutor.isShutdown()) {
			if (sonarIntegrationQueue.size() == 0) {
				CIPluginSDKUtils.doBreakableWait(LIST_EMPTY_INTERVAL, NO_SONAR_COVERAGE_ITEMS_MONITOR);
				continue;
			}

			SonarBuildCoverageQueueItem sonarBuildCoverageQueueItem = null;
			try {
				sonarBuildCoverageQueueItem = sonarIntegrationQueue.peek();
				retrieveAndPushSonarDataToOctane(sonarBuildCoverageQueueItem);
				logger.debug("successfully processed " + sonarBuildCoverageQueueItem);
				sonarIntegrationQueue.remove();
			} catch (TemporaryException te) {
				logger.error("temporary error on " + sonarBuildCoverageQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", te);
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pe) {
				logger.error("permanent error on " + sonarBuildCoverageQueueItem + ", passing over", pe);
				sonarIntegrationQueue.remove();
			} catch (Throwable t) {
				logger.error("unexpected error on build coverage item '" + sonarBuildCoverageQueueItem + "', passing over", t);
				sonarIntegrationQueue.remove();
			}
		}
	}

	@Override
	public synchronized void ensureSonarWebhookExist(String ciCallbackUrl, String sonarURL, String sonarToken) throws SonarIntegrationException {
		//problem in sonar project key in new project
		try {
			String webhookKey = getWebhookKey(ciCallbackUrl, sonarURL, sonarToken);
			if (webhookKey == null) {
				HttpClient httpClient = HttpClientBuilder.create().build();

				URIBuilder uriBuilder = new URIBuilder(sonarURL + WEBHOOK_CREATE_URI)
						.setParameter("name", "ci_" + configurer.octaneConfiguration.getInstanceId())
						.setParameter("url", ciCallbackUrl);

				HttpPost request = new HttpPost(uriBuilder.toString());
				setTokenInHttpRequest(request, sonarToken);
				HttpResponse response = httpClient.execute(request);

				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					// error can sometimes return empty results
					String errorMessage = "exception during webhook registration for  ciNotificationUrl: "
							.concat(ciCallbackUrl)
							.concat(" with status code: ")
							.concat(String.valueOf(response.getStatusLine().getStatusCode()));
					throw new SonarIntegrationException(errorMessage);
				}
			}

		} catch (SonarIntegrationException e) {
			logger.error(e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			String errorMessage = "exception during webhook registration for ciNotificationUrl: " + ciCallbackUrl;
			logger.error(errorMessage, e);
			throw new SonarIntegrationException(errorMessage, e);
		}
	}

	@Override
	public void shutdown() {
		sonarIntegrationExecutor.shutdown();
	}

	@Override
	public void enqueueFetchAndPushSonarCoverage(String jobId, String buildId, String projectKey, String sonarURL, String sonarToken) {
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build ID MUST NOT be null nor empty");
		}
		if (sonarURL == null || sonarURL.isEmpty()) {
			throw new IllegalArgumentException("sonar URL MUST NOT be null nor empty");
		}
		//  [YG] TODO: check if the rest of the parameters are also non-optional and add validations

		sonarIntegrationQueue.add(new SonarBuildCoverageQueueItem(jobId, buildId, projectKey, sonarURL, sonarToken));
		synchronized (NO_SONAR_COVERAGE_ITEMS_MONITOR) {
			NO_SONAR_COVERAGE_ITEMS_MONITOR.notify();
		}
	}

	@Override
	public String getSonarStatus(String sonarURL) {
		try {
			URIBuilder uriBuilder = new URIBuilder(sonarURL + SONAR_STATUS_URI);
			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(uriBuilder.build());
			HttpResponse response = httpClient.execute(request);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return CIPluginSDKUtils.getObjectMapper().readTree(response.getEntity().getContent()).get("status").textValue();
			} else {
				return CONNECTION_FAILURE;
			}
		} catch (URISyntaxException | IOException e) {
			return CONNECTION_FAILURE;
		}
	}

	@Override
	public InputStream getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem queueItem){
		try {
			return new SonarVulnerabilitiesUtil(queueItem,restService,configurer).getVulnerabilitiesScanResultStream(queueItem);
		}
		catch (Exception e){
			throw new RuntimeException(e);
		}
	}



	private void retrieveAndPushSonarDataToOctane(SonarBuildCoverageQueueItem queueItem) {
		//  preflight
		if (!coverageService.isSonarReportRelevant(queueItem.jobId)) {
			return;
		}

		StringBuilder errorMessage = new StringBuilder()
				.append("failed to inject sonarqube coverage data to octane for project key: ")
				.append(queueItem.projectKey)
				.append(" with ciIdentity: ").append(configurer.octaneConfiguration.getInstanceId())
				.append(" with jobId: ").append(queueItem.jobId)
				.append(" with buildId: ").append(queueItem.buildId);

		try {
			//  retrieve coverage report from Sonar
			Integer pageIndex = 0;
			BuildCoverage buildCoverageReport = dtoFactory.newDTO(BuildCoverage.class);
			JsonNode jsonReport;
			do {
				pageIndex++;
				InputStream reportStream = getPageFromSonar(queueItem, pageIndex);
				jsonReport = CIPluginSDKUtils.getObjectMapper().readTree(reportStream);
				buildCoverageReport.mergeSonarCoverageReport(jsonReport);
			} while (SonarUtils.sonarReportHasAnotherPage(pageIndex, jsonReport));

			//  push coverage to Octane
			OctaneResponse response = coverageService.pushCoverage(queueItem.jobId, queueItem.buildId, CoverageReportType.SONAR_REPORT, dtoFactory.dtoToJsonStream(buildCoverageReport));
			if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
				errorMessage.append(" with status code: ").append(response.getStatus());
				throw new TemporaryException(errorMessage.toString());
			} else if (response.getStatus() != HttpStatus.SC_OK) {
				errorMessage.append(" with status code: ").append(response.getStatus())
						.append(" and response body: ").append(response.getBody());
				throw new PermanentException(errorMessage.toString());
			}
		} catch (Throwable throwable) {
			logger.error(errorMessage.toString(), throwable);
			throw new PermanentException(throwable);
		}
	}



	private String getWebhookKey(String ciNotificationUrl, String sonarURL, String token) throws SonarIntegrationException {
		try {
			URIBuilder uriBuilder = new URIBuilder(sonarURL + WEBHOOK_LIST_URI);
			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(uriBuilder.build());
			setTokenInHttpRequest(request, token);

			HttpResponse response = httpClient.execute(request);
			InputStream content = response.getEntity().getContent();
			// if webhooks exist
			if (content.available() != 0) {
				JsonNode jsonResponse = CIPluginSDKUtils.getObjectMapper().readTree(content);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					ArrayNode webhooksListJson = (ArrayNode) jsonResponse.get("webhooks");
					if (webhooksListJson.size() > 0) {
						for (JsonNode webhookNode : webhooksListJson) {
							String entryURL = webhookNode.get("url").textValue();
							if (entryURL.equals(ciNotificationUrl)) {
								return webhookNode.get("key").textValue();
							}
						}
					}
					return null;
				} else {
					String errorMessage = ""
							.concat("failed to get webhook key from soanrqube with notification URL: ")
							.concat(ciNotificationUrl)
							.concat(" with status code: ").concat(String.valueOf(response.getStatusLine().getStatusCode()))
							.concat(" with errors: ").concat(jsonResponse.get("errors").toString());
					throw new SonarIntegrationException(errorMessage);

				}
			}
			return null;
		} catch (SonarIntegrationException e) {
			logger.error(e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			String errorMessage = ""
					.concat("failed to get webhook key from soanrqube with notification URL: ").concat(ciNotificationUrl);
			logger.error(errorMessage, e);
			throw new SonarIntegrationException(errorMessage, e);
		}
	}

	private InputStream getPageFromSonar(SonarBuildCoverageQueueItem queueItem, Integer page) {
		String sonarURL = queueItem.sonarURL;
		String projectKey = queueItem.projectKey;
		String token = queueItem.sonarToken;
		try {

			URIBuilder uriBuilder = new URIBuilder(sonarURL + COMPONENT_TREE_URI);
			uriBuilder.setParameter("metricKeys", "lines_to_cover,uncovered_lines")
					.setParameter("component", projectKey)
					.setParameter("qualifiers", "FIL,TRK")
					.setParameter("ps", "500")
					.setParameter("p", page.toString());

			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(uriBuilder.build());
			setTokenInHttpRequest(request, token);

			HttpResponse httpResponse = httpClient.execute(request);
			return httpResponse.getEntity().getContent();

		} catch (Exception e) {
			String errorMessage = ""
					.concat("failed to get coverage data from sonar for project: ")
					.concat(projectKey);
			logger.error(errorMessage, e);
			throw new PermanentException(errorMessage, e);
		}
	}

	private void setTokenInHttpRequest(HttpRequest request, String token) throws AuthenticationException {
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(token, "");
		request.addHeader(new BasicScheme().authenticate(creds, request, null));
	}

	private static final class SonarBuildCoverageQueueItem implements QueueingService.QueueItem {
		private String jobId;
		private String buildId;
		private String projectKey;
		private String sonarURL;
		private String sonarToken;

		//  [YG] this constructor MUST be present, don't remove
		private SonarBuildCoverageQueueItem() {
		}

		public SonarBuildCoverageQueueItem(String jobId, String buildId, String projectKey, String sonarURL, String sonarToken) {
			this.jobId = jobId;
			this.buildId = buildId;
			this.projectKey = projectKey;
			this.sonarURL = sonarURL;
			this.sonarToken = sonarToken;
		}

		@Override
		public String toString() {
			return "'" + jobId + " #" + buildId + "'";
		}
	}

	private static final class SonarIntegrationWorkerThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("SonarIntegrationWorker-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}
}
