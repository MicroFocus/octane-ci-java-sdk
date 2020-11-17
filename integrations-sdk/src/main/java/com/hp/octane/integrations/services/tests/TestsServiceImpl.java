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

package com.hp.octane.integrations.services.tests;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.RequestTimeoutException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.WorkerPreflight;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.configuration.ConfigurationServiceImpl;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.services.rest.RestService.CORRELATION_ID_HEADER;

/**
 * Default implementation of tests service
 * Handled by
 * com.hp.mqm.analytics.devops.insights.resources.DevopsInsightsSSAResource#isTestResultNeeded
 * com.hp.mqm.testbox.rest.TestResultCIPushResource#pushXml
 */

final class TestsServiceImpl implements TestsService {
	private static final Logger logger = LogManager.getLogger(TestsServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String TESTS_RESULTS_QUEUE_FILE = "test-results-queue.dat";
	public static int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;

	private final ExecutorService testsPushExecutor = Executors.newSingleThreadExecutor(new TestsResultPushWorkerThreadFactory());
	private final ObjectQueue<TestsResultQueueItem> testResultsQueue;
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;
	private final WorkerPreflight workerPreflight;
	private final ConfigurationService configurationService;

	//Metrics
	private long requestTimeoutCount = 0;
	private long lastRequestTimeoutTime = 0;

	TestsServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService, ConfigurationService configurationService) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (queueingService == null) {
			throw new IllegalArgumentException("queue service MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}
		if (configurationService == null) {
			throw new IllegalArgumentException("configuration service MUST NOT be null");
		}


		if (queueingService.isPersistenceEnabled()) {
			testResultsQueue = queueingService.initFileQueue(TESTS_RESULTS_QUEUE_FILE, TestsResultQueueItem.class);
		} else {
			testResultsQueue = queueingService.initMemoQueue();
		}

		this.configurer = configurer;
		this.restService = restService;
		this.configurationService = configurationService;
		this.workerPreflight = new WorkerPreflight(this, configurationService, logger);

		logger.info(configurer.octaneConfiguration.geLocationForLog() + "starting background worker...");
		testsPushExecutor.execute(this::worker);
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "initialized SUCCESSFULLY (backed by " + testResultsQueue.getClass().getSimpleName() + ")");
	}

	@Override
	public boolean isTestsResultRelevant(String jobId, String rootJobId) {
		String serverCiId = configurer.octaneConfiguration.getInstanceId();
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job CI ID MUST NOT be null nor empty");
		}

		boolean base64 = isEncodeBase64();
		String jobIdEncoded = base64 ? CIPluginSDKUtils.urlEncodeBase64(jobId) : CIPluginSDKUtils.urlEncodePathParam(jobId);
		String rootJobIdEncoded = base64 ? CIPluginSDKUtils.urlEncodeBase64(rootJobId) : CIPluginSDKUtils.urlEncodeQueryParam(rootJobId);

		String url = getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
				"servers/" + CIPluginSDKUtils.urlEncodePathParam(serverCiId) +
				"/jobs/" + jobIdEncoded + "/tests-result-preflight";
		if (rootJobId != null && !rootJobId.isEmpty()) {
			url += "?rootJobId=" + rootJobIdEncoded;
		}
		if (base64) {
			url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
			logger.info("Using base64, " + url);
		}

		OctaneRequest preflightRequest = dtoFactory
				.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setTimeoutSec(60)
				.setUrl(url);

		try {
			OctaneResponse response = restService.obtainOctaneRestClient().execute(preflightRequest);
			if (response.getStatus() == HttpStatus.SC_OK) {
				return String.valueOf(true).equals(response.getBody());
			} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
				throw new TemporaryException("preflight request failed with status " + response.getStatus());
			} else if (response.getStatus() == HttpStatus.SC_UNAUTHORIZED || response.getStatus() == HttpStatus.SC_FORBIDDEN) {
				CIPluginSDKUtils.doWait(30000);
				throw new PermanentException("preflight request failed with status " + response.getStatus());
			} else {
				throw new PermanentException("preflight request failed with status " + response.getStatus() + ". JobId: '" + jobId + "'. Request URL : " + url);
			}
		} catch (InterruptedIOException ie) {
			throw new RequestTimeoutException("!!!!!!!!!!!!!!!!!!! request timeout during preflight : " + ie.getClass().getCanonicalName() + " - " + ie.getMessage());
		} catch (IOException ioe) {
			throw new TemporaryException(ioe);
		}
	}

	private boolean isEncodeBase64() {
		return ConfigurationParameterFactory.isEncodeCiJobBase64(configurer.octaneConfiguration);
	}

	@Override
	public OctaneResponse pushTestsResult(TestsResult testsResult, String jobId, String buildId) throws IOException {
		if (testsResult == null) {
			throw new IllegalArgumentException("tests result MUST NOT be null");
		}
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build ID MUST NOT be null nor empty");
		}

		String testsResultAsXml = dtoFactory.dtoToXml(testsResult);
		InputStream testsResultAsStream = new ByteArrayInputStream(testsResultAsXml.getBytes(Charsets.UTF_8));
		return pushTestsResult(testsResultAsStream, jobId, buildId);
	}

	@Override
	public OctaneResponse pushTestsResult(InputStream testsResult, String jobId, String buildId) throws IOException {
		if (testsResult == null) {
			throw new IllegalArgumentException("tests result MUST NOT be null");
		}
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build ID MUST NOT be null nor empty");
		}

		OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_XML.getMimeType());
		headers.put(CORRELATION_ID_HEADER, CIPluginSDKUtils.getNextCorrelationId());

		String tempJobId = jobId;
		boolean base64 = isEncodeBase64();
		if (base64) {
			tempJobId = CIPluginSDKUtils.urlEncodeBase64(jobId);
		}

		String uri;
		try {
			uri = new URIBuilder(getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) + "test-results")
					.addParameter("skip-errors", "false")
					.addParameter("instance-id", configurer.octaneConfiguration.getInstanceId())
					.addParameter("job-ci-id", tempJobId)
					.addParameter("build-ci-id", buildId)
					.build()
					.toString();
		} catch (URISyntaxException urise) {
			throw new PermanentException("failed to build URL to Octane's 'test-results' resource", urise);
		}
		if (base64) {
			uri = CIPluginSDKUtils.addParameterEncode64ToUrl(uri);
		}
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.POST)
				.setUrl(uri)
				.setHeaders(headers)
				.setBody(testsResult)
				.setTimeoutSec(60 * 2);//give 2 min for case of big number of tests

		try {
			return octaneRestClient.execute(request);
		} catch (InterruptedIOException ie) {
			throw new RequestTimeoutException("!!!!!!!!!!!!!!!!!!! request timeout during pushTestsResult : " + ie.getClass().getCanonicalName() + " - " + ie.getMessage());
		}
	}

	@Override
	public void enqueuePushTestsResult(String jobId, String buildId, String rootJobId) {
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build ID MUST NOT be null nor empty");
		}
		if (this.configurer.octaneConfiguration.isDisabled()) {
			return;
		}

		if (!((ConfigurationServiceImpl) configurationService).isRelevantForOctane(rootJobId)) {
			return;
		}

		testResultsQueue.add(new TestsResultQueueItem(jobId, buildId, rootJobId));
		workerPreflight.itemAddedToQueue();
	}

	@Override
	public void shutdown() {
		testsPushExecutor.shutdown();
	}

	@Override
	public boolean isShutdown() {
		return testsPushExecutor.isShutdown();
	}

	//  infallible everlasting background worker
	private void worker() {
		while (!testsPushExecutor.isShutdown()) {
			if(!workerPreflight.preflight()){
				continue;
			}

			TestsResultQueueItem testsResultQueueItem = null;
			try {
				testsResultQueueItem = testResultsQueue.peek();
				doPreflightAndPushTestResult(testsResultQueueItem);
				testResultsQueue.remove();
			} catch (RequestTimeoutException rte){
				requestTimeoutCount++;
				lastRequestTimeoutTime = System.currentTimeMillis();
				logger.error(configurer.octaneConfiguration.geLocationForLog() + rte.getMessage());
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (TemporaryException tque) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() + "temporary error on " + testsResultQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pqie) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() + "permanent error on " + testsResultQueueItem + ", passing over", pqie);
				testResultsQueue.remove();
			} catch (Throwable t) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() + "unexpected error on build log item '" + testsResultQueueItem + "', passing over", t);
				testResultsQueue.remove();
			}
		}
	}

	private void doPreflightAndPushTestResult(TestsResultQueueItem queueItem) {

		//  validate test result - first to be done as it is the cheapest to 'fail fast'
		InputStream testsResultA = configurer.pluginServices.getTestsResult(queueItem.jobId, queueItem.buildId);
		if (testsResultA == null) {
			logger.warn(configurer.octaneConfiguration.geLocationForLog() + "test result of " + queueItem + " resolved to be NULL, skipping");
			return;
		}
		try {
			//  preflight
			InputStream testsResultB;
			boolean isRelevant = isTestsResultRelevant(queueItem.jobId, queueItem.rootJobId);
			logger.info(configurer.octaneConfiguration.geLocationForLog() + "test results preflight " + queueItem + " = " + isRelevant);
			if (!isRelevant) {
				return;
			}

			//  [YG] TODO: TEMPORARY SOLUTION - ci server ID, job ID and build ID should move to become a query parameters
			try {
				String testResultXML = CIPluginSDKUtils.inputStreamToUTF8String(testsResultA);
				testResultXML = testResultXML.replaceAll("<build.*?>",
						"<build server_id=\"" + configurer.octaneConfiguration.getInstanceId() + "\" job_id=\"" + queueItem.jobId + "\" build_id=\"" + queueItem.buildId + "\"/>")
				.replace("</build>","");//remove closing build element if exist

				testsResultB = new ByteArrayInputStream(testResultXML.getBytes(Charsets.UTF_8));
			} catch (Exception e) {
				throw new PermanentException("failed to update ci server instance ID in the test results XML");
			}

			//  push
			try {
				OctaneResponse response = pushTestsResult(testsResultB, queueItem.jobId, queueItem.buildId);
				if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
					logger.info(configurer.octaneConfiguration.geLocationForLog() + "successfully pushed test results for " + queueItem + "; status: " + response.getStatus() +
							", response: " + response.getBody() + ", CorrelationId - " + response.getCorrelationId());
				} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
					throw new TemporaryException("push request TEMPORARILY failed with status " + response.getStatus());
				} else {
					throw new PermanentException("push request PERMANENTLY failed with status " + response.getStatus());
				}
			} catch (IOException ioe) {
				throw new TemporaryException("failed to perform push test results request for " + queueItem, ioe);
			} finally {
				try {
					testsResultB.close();
				} catch (IOException e) {
					logger.warn(configurer.octaneConfiguration.geLocationForLog() + "failed to close test result file after push test for " + queueItem);
				}
			}
		} finally {
			try {
				testsResultA.close();
			} catch (IOException e) {
				logger.warn(configurer.octaneConfiguration.geLocationForLog() + "failed to close test result file after push test for " + queueItem);
			}
		}
	}

	private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART;
	}

	@Override
	public long getQueueSize() {
		return testResultsQueue.size();
	}

	@Override
	public void clearQueue() {
		while (testResultsQueue.size() > 0) {
			testResultsQueue.remove();
		}
	}

	@Override
	public Map<String, Object> getMetrics() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("isShutdown", this.isShutdown());
		map.put("queueSize", this.getQueueSize());
		map.put("requestTimeoutCount", this.requestTimeoutCount);
		if (lastRequestTimeoutTime > 0) {
			map.put("lastRequestTimeoutTime", new Date(lastRequestTimeoutTime));
		}
		workerPreflight.addMetrics(map);
		return map;
	}

	private static final class TestsResultQueueItem implements QueueingService.QueueItem {
		private String jobId;
		private String buildId;
		private String rootJobId;

		//  [YG] this constructor MUST be present
		private TestsResultQueueItem() {
		}

		private TestsResultQueueItem(String jobId, String buildId, String rootJobId) {
			this.jobId = jobId;
			this.buildId = buildId;
			this.rootJobId = rootJobId;
		}

		@Override
		public String toString() {
			return "'" + jobId + " #" + buildId + (rootJobId != null ? "', root job : " + rootJobId : "");
		}
	}

	private static final class TestsResultPushWorkerThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("TestsResultPushWorker-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}
}
