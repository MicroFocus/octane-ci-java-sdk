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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.coverage.BuildCoverage;
import com.hp.octane.integrations.exceptions.OctaneSDKSonarException;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.queue.QueueService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.spi.CIPluginServices;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

class SonarServiceImpl implements SonarService {
	private static final Logger logger = LogManager.getLogger(SonarServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private static String WEBHOOK_CREATE_URI = "/api/webhooks/create";
	private static String WEBHOOK_LIST_URI = "/api/webhooks/list";
	private static String SONAR_STATUS_URI = "/api/system/status";
	private static String CONNECTION_FAILURE = "CONNECTION_FAILURE";
	private static String COMPONENT_TREE_URI = "/api/measures/component_tree";

	private final String BUILD_COVERAGE_QUEUE_FILE = "build-coverage-queue.dat";
	private final ObjectQueue<SonarServiceImpl.BuildCoverageQueueItem> sonarIntegrationQueue;
	private final CIPluginServices pluginServices;
	private final RestService restService;

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;
	private int LIST_EMPTY_INTERVAL = 3000;

	SonarServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, QueueService queueService, RestService restService) {
		if (configurer == null || configurer.pluginServices == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (queueService == null) {
			throw new IllegalArgumentException("queue service MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		this.pluginServices = configurer.pluginServices;
		this.restService = restService;

		if (queueService.isPersistenceEnabled()) {
			sonarIntegrationQueue = queueService.initFileQueue(BUILD_COVERAGE_QUEUE_FILE, SonarServiceImpl.BuildCoverageQueueItem.class);
		} else {
			sonarIntegrationQueue = queueService.initMemoQueue();
		}

		logger.info("starting background worker...");
		Executors
				.newSingleThreadExecutor(new SonarIntegrationWorkerThreadFactory())
				.execute(this::worker);
		logger.info("initialized SUCCESSFULLY (backed by " + sonarIntegrationQueue.getClass().getSimpleName() + ")");
	}

	// infallible everlasting background worker
	private void worker() {
		while (true) {
			if (sonarIntegrationQueue.size() > 0) {
				SonarServiceImpl.BuildCoverageQueueItem buildCoverageQueueItem = null;
				try {
					buildCoverageQueueItem = sonarIntegrationQueue.peek();
					pushSonarDataToOctane(pluginServices.getServerInfo().getInstanceId(), buildCoverageQueueItem);
					logger.debug("successfully processed " + buildCoverageQueueItem);
					sonarIntegrationQueue.remove();
				} catch (TemporaryException te) {
					logger.error("temporary error on " + buildCoverageQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", te);
					CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
				} catch (PermanentException pe) {
					logger.error("permanent error on " + buildCoverageQueueItem + ", passing over", pe);
					sonarIntegrationQueue.remove();
				} catch (Throwable t) {
					logger.error("unexpected error on build coverage item '" + buildCoverageQueueItem + "', passing over", t);
					sonarIntegrationQueue.remove();
				}
			} else {
				CIPluginSDKUtils.doWait(LIST_EMPTY_INTERVAL);
			}
		}
	}

	@Override
	public synchronized void ensureWebhookExist(String ciCallbackUrl, String sonarURL, String sonarToken) throws OctaneSDKSonarException {
		//problem in sonar project key in new project
		try {
			String webhookKey = getWebhookKey(ciCallbackUrl, sonarURL, sonarToken);
			if (webhookKey == null) {
				HttpClient httpClient = HttpClientBuilder.create().build();

				URIBuilder uriBuilder = new URIBuilder(sonarURL + WEBHOOK_CREATE_URI)
						.setParameter("name", "ci_" + pluginServices.getServerInfo().getInstanceId())
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
					throw new OctaneSDKSonarException(errorMessage);
				}
			}

		} catch (OctaneSDKSonarException e) {
			logger.error(e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			String errorMessage = "exception during webhook registration for ciNotificationUrl: " + ciCallbackUrl;
			logger.error(errorMessage, e);
			throw new OctaneSDKSonarException(errorMessage, e);
		}
	}

	@Override
	public void enqueueFetchAndPushSonarCoverageToOctane(String jobId, String buildId, String projectKey, String sonarURL, String sonarToken) {
		sonarIntegrationQueue.add(new SonarServiceImpl.BuildCoverageQueueItem(jobId, buildId, projectKey, sonarURL, sonarToken));
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
		} catch (Exception e) {
			return CONNECTION_FAILURE;
		}
	}

	private void pushSonarDataToOctane(String serverId, BuildCoverageQueueItem queueItem) {
		OctaneConfiguration octaneConfiguration = pluginServices.getOctaneConfiguration();
		if (octaneConfiguration == null || !octaneConfiguration.isValid()) {
			logger.warn("no (valid) Octane configuration found, bypassing " + queueItem);
			return;
		}

		//  preflight
		String[] workspaceIDs = preflightRequest(octaneConfiguration, serverId, queueItem.jobId);
		if (workspaceIDs.length == 0) {
			logger.info("coverage of " + queueItem + " found no interested workspace in Octane, passing over");
			return;
		} else {
			logger.info("coverage of " + queueItem + " found " + workspaceIDs.length + " interested workspace/s in Octane, dispatching the coverage");
		}

		StringBuilder errorMessage = new StringBuilder()
				.append("failed to inject sonarqube coverage data to octane for project key: ")
				.append(queueItem.projectKey)
				.append(" with ciIdentity: ").append(serverId)
				.append(" with jobId: ").append(queueItem.jobId)
				.append(" with buildId: ").append(queueItem.buildId);

		try {
			Integer pageIndex = 0;
			BuildCoverage buildCoverageReport = dtoFactory.newDTO(BuildCoverage.class);
			JsonNode jsonReport;
			do {
				pageIndex++;
				InputStream reportStream = getPageFromSonar(queueItem, pageIndex);
				jsonReport = CIPluginSDKUtils.getObjectMapper().readTree(reportStream);
				buildCoverageReport.mergeSonarCoverageReport(jsonReport);
			} while (coverageReportHasAnotherPage(pageIndex, jsonReport));

			OctaneRequest coveragePutRequest = buildCoveragePutRequest(buildCoverageReport, serverId, queueItem.jobId, queueItem.buildId);
			OctaneResponse response = restService.obtainOctaneRestClient().execute(coveragePutRequest);

			if (response.getStatus() != HttpStatus.SC_OK) {
				errorMessage.append(" with status code: ").append(response.getStatus())
						.append(" and response body: ").append(response.getBody());
				throw new PermanentException(errorMessage.toString());
			}
		} catch (Exception e) {
			logger.error(errorMessage.toString(), e);
			throw new PermanentException(e);
		}
	}

	private String[] preflightRequest(OctaneConfiguration octaneConfiguration, String serverId, String jobId) {
		String[] result = new String[0];
		OctaneResponse response;

		//  get result
		try {
			OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
					.setMethod(HttpMethod.GET)
					.setUrl(getAnalyticsContextPath(octaneConfiguration.getUrl(), octaneConfiguration.getSharedSpace()) +
							"servers/" + serverId + "/jobs/" + jobId + "/workspaceId");
			response = restService.obtainOctaneRestClient().execute(preflightRequest);
			if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
				throw new TemporaryException("preflight request failed with status " + response.getStatus());
			} else if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_NO_CONTENT) {
				throw new PermanentException("preflight request failed with status " + response.getStatus());
			}
		} catch (IOException ioe) {
			throw new TemporaryException(ioe);
		}

		//  parse result
		if (response.getBody() != null && !response.getBody().isEmpty()) {
			try {
				result = CIPluginSDKUtils.getObjectMapper().readValue(response.getBody(), String[].class);
			} catch (IOException ioe) {
				throw new PermanentException("failed to parse preflight response '" + response.getBody() + "' for '" + jobId + "'");
			}
		}
		return result;
	}

	private Boolean coverageReportHasAnotherPage(Integer pageIndex, JsonNode jsonContent) {
		JsonNode pagingNode = jsonContent.get("paging");
		Integer pageSize = pagingNode.get("pageSize").intValue();
		int total = pagingNode.get("total").intValue();
		return pageSize * pageIndex < total;
	}

	private OctaneRequest buildCoveragePutRequest(BuildCoverage buildCoverage, String ciIdentity, String jobId, String buildId) throws URISyntaxException, JsonProcessingException {
		URIBuilder uriBuilder = new URIBuilder(getAnalyticsContextPath(pluginServices.getOctaneConfiguration().getUrl(), pluginServices.getOctaneConfiguration().getSharedSpace()) + "coverage")
				.setParameter("ci-server-identity", CIPluginSDKUtils.urlEncodeQueryParam(ciIdentity))
				.setParameter("ci-job-id", CIPluginSDKUtils.urlEncodeQueryParam(jobId))
				.setParameter("ci-build-id", CIPluginSDKUtils.urlEncodeQueryParam(buildId))
				.setParameter("file-type", SonarService.SONAR_REPORT);
		String reportToOctane = CIPluginSDKUtils.getObjectMapper().writeValueAsString(buildCoverage);
		return dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.PUT)
				.setUrl(uriBuilder.toString())
				.setBody(reportToOctane);
	}


	private String getWebhookKey(String ciNotificationUrl, String sonarURL, String token) throws OctaneSDKSonarException {
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
					throw new OctaneSDKSonarException(errorMessage);

				}
			}
			return null;
		} catch (OctaneSDKSonarException e) {
			logger.error(e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			String errorMessage = ""
					.concat("failed to get webhook key from soanrqube with notification URL: ").concat(ciNotificationUrl);
			logger.error(errorMessage, e);
			throw new OctaneSDKSonarException(errorMessage, e);
		}
	}

	private InputStream getPageFromSonar(BuildCoverageQueueItem queueItem, Integer page) {
		String sonarURL = queueItem.sonarURL;
		String token = queueItem.sonarToken;
		String projectKey = queueItem.projectKey;
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

	private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART;
	}

	private static final class BuildCoverageQueueItem implements QueueService.QueueItem {
		private String jobId;
		private String buildId;
		private String projectKey;
		private String sonarURL;
		private String sonarToken;

		//  [YG] this constructor MUST be present, don't remove
		private BuildCoverageQueueItem() {
		}

		public BuildCoverageQueueItem(String jobId, String buildId, String projectKey, String sonarURL, String sonarToken) {
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
