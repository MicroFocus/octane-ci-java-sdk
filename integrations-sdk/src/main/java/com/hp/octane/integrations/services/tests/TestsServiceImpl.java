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
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.services.queueing.QueueingService;
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
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of tests service
 */

final class TestsServiceImpl implements TestsService {
	private static final Logger logger = LogManager.getLogger(TestsServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String TESTS_RESULTS_QUEUE_FILE = "test-results-queue.dat";

	private final ExecutorService testsPushExecutor = Executors.newSingleThreadExecutor(new TestsResultPushWorkerThreadFactory());
	private final Object NO_TEST_RESULTS_MONITOR = new Object();
	private final ObjectQueue<TestsResultQueueItem> testResultsQueue;
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 10000;
	private int LIST_EMPTY_INTERVAL = 3000;

	TestsServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (queueingService == null) {
			throw new IllegalArgumentException("queue service MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		if (queueingService.isPersistenceEnabled()) {
			testResultsQueue = queueingService.initFileQueue(TESTS_RESULTS_QUEUE_FILE, TestsResultQueueItem.class);
		} else {
			testResultsQueue = queueingService.initMemoQueue();
		}

		this.configurer = configurer;
		this.restService = restService;

		logger.info("starting background worker...");
		testsPushExecutor.execute(this::worker);
		logger.info("initialized SUCCESSFULLY (backed by " + testResultsQueue.getClass().getSimpleName() + ")");
	}

	@Override
	public boolean isTestsResultRelevant(String jobId) throws IOException {
		String serverCiId = configurer.octaneConfiguration.getInstanceId();
		if (serverCiId == null || serverCiId.isEmpty()) {
			throw new IllegalArgumentException("server CI ID MUST NOT be null nor empty");
		}
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job CI ID MUST NOT be null nor empty");
		}

		OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setUrl(getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
						"servers/" + CIPluginSDKUtils.urlEncodePathParam(serverCiId) +
						"/jobs/" + CIPluginSDKUtils.urlEncodePathParam(jobId) + "/tests-result-preflight");

		OctaneResponse response = restService.obtainOctaneRestClient().execute(preflightRequest);
		return response.getStatus() == HttpStatus.SC_OK && String.valueOf(true).equals(response.getBody());
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
		InputStream testsResultAsStream = new ByteArrayInputStream(testsResultAsXml.getBytes(Charset.defaultCharset()));
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
		String uri;
		try {
			uri = new URIBuilder(getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) + "test-results")
					.addParameter("skip-errors", "false")
					.addParameter("instance-id", configurer.octaneConfiguration.getInstanceId())
					.addParameter("job-ci-id", jobId)
					.addParameter("build-ci-id", buildId)
					.build()
					.toString();
		} catch (URISyntaxException urise) {
			throw new PermanentException("failed to build URL to Octane's 'test-results' resource", urise);
		}
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.POST)
				.setUrl(uri)
				.setHeaders(headers)
				.setBody(testsResult);
		OctaneResponse response = octaneRestClient.execute(request);
		logger.info("tests result pushed; status: " + response.getStatus() + ", response: " + response.getBody());
		return response;
	}

	@Override
	public void enqueuePushTestsResult(String jobId, String buildId) {
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build ID MUST NOT be null nor empty");
		}

		testResultsQueue.add(new TestsResultQueueItem(jobId, buildId));
		synchronized (NO_TEST_RESULTS_MONITOR) {
			NO_TEST_RESULTS_MONITOR.notify();
		}
	}

	@Override
	public void shutdown() {
		testsPushExecutor.shutdown();
	}

	//  infallible everlasting background worker
	private void worker() {
		while (!testsPushExecutor.isShutdown()) {
			if (testResultsQueue.size() == 0) {
				CIPluginSDKUtils.doBreakableWait(LIST_EMPTY_INTERVAL, NO_TEST_RESULTS_MONITOR);
				continue;
			}

			TestsResultQueueItem testsResultQueueItem = null;
			try {
				testsResultQueueItem = testResultsQueue.peek();
				doPreflightAndPushTestResult(testsResultQueueItem);
				logger.debug("successfully processed " + testsResultQueueItem);
				testResultsQueue.remove();
			} catch (TemporaryException tque) {
				logger.error("temporary error on " + testsResultQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pqie) {
				logger.error("permanent error on " + testsResultQueueItem + ", passing over", pqie);
				testResultsQueue.remove();
			} catch (Throwable t) {
				logger.error("unexpected error on build log item '" + testsResultQueueItem + "', passing over", t);
				testResultsQueue.remove();
			}
		}
	}

	private void doPreflightAndPushTestResult(TestsResultQueueItem queueItem) {

		//  validate test result - first to be done as it is the cheapest to 'fail fast'
		InputStream testsResult = configurer.pluginServices.getTestsResult(queueItem.jobId, queueItem.buildId);
		if (testsResult == null) {
			logger.warn("test result of " + queueItem + " resolved to be NULL, skipping");
			return;
		}

		//  preflight
		boolean isRelevant;
		try {
			isRelevant = isTestsResultRelevant(queueItem.jobId);
			if (!isRelevant) {
				logger.debug("no interest found in Octane for test results of " + queueItem + ", skipping");
				return;
			}
		} catch (IOException ioe) {
			throw new TemporaryException("failed to perform preflight request for " + queueItem, ioe);
		}

		//  [YG] TODO: TEMPORARY SOLUTION - ci server ID, job ID and build ID should move to become a query parameters
		try {
			String testResultXML = CIPluginSDKUtils.inputStreamToUTF8String(testsResult);
			testResultXML = testResultXML.replaceAll("<build.*?>",
					"<build server_id=\"" + configurer.octaneConfiguration.getInstanceId() + "\" job_id=\"" + queueItem.jobId + "\" build_id=\"" + queueItem.buildId + "\"/>");
			testsResult = new ByteArrayInputStream(testResultXML.getBytes(Charsets.UTF_8));
		} catch (Exception e) {
			throw new PermanentException("failed to update ci server instance ID in the test results XML");
		}

		//  push
		try {
			OctaneResponse response = pushTestsResult(testsResult, queueItem.jobId, queueItem.buildId);
			if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
				throw new TemporaryException("push request TEMPORARILY failed with status " + response.getStatus());
			} else if (response.getStatus() != HttpStatus.SC_ACCEPTED) {
				throw new PermanentException("push request PERMANENTLY failed with status " + response.getStatus());
			}
		} catch (IOException ioe) {
			throw new TemporaryException("failed to perform push test results request for " + queueItem, ioe);
		}
	}

	private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART;
	}

	private static final class TestsResultQueueItem implements QueueingService.QueueItem {
		private String jobId;
		private String buildId;

		//  [YG] this constructor MUST be present
		private TestsResultQueueItem() {
		}

		private TestsResultQueueItem(String jobId, String buildId) {
			this.jobId = jobId;
			this.buildId = buildId;
		}

		@Override
		public String toString() {
			return "'" + jobId + " #" + buildId + "'";
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
