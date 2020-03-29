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

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.coverage.CoverageReportType;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of Coverage Service
 */

class CoverageServiceImpl implements CoverageService {
	private static final Logger logger = LogManager.getLogger(CoverageServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final ExecutorService coveragePushExecutor = Executors.newSingleThreadExecutor(new CoveragePushWorkerThreadFactory());
	private final Object NO_COVERAGES_MONITOR = new Object();
	private final String BUILD_COVERAGE_QUEUE_FILE = "coverage-push-queue.dat";
	private final ObjectQueue<CoverageQueueItem> coveragePushQueue;
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;
	private int LIST_EMPTY_INTERVAL = 3000;
	private int REGULAR_CYCLE_PAUSE = 250;

	CoverageServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService) {
		if (configurer == null || configurer.pluginServices == null || configurer.octaneConfiguration == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (queueingService == null) {
			throw new IllegalArgumentException("queue service MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		this.configurer = configurer;
		this.restService = restService;

		if (queueingService.isPersistenceEnabled()) {
			coveragePushQueue = queueingService.initFileQueue(BUILD_COVERAGE_QUEUE_FILE, CoverageQueueItem.class);
		} else {
			coveragePushQueue = queueingService.initMemoQueue();
		}

		logger.info(configurer.octaneConfiguration.geLocationForLog() + "starting background worker...");
		coveragePushExecutor.execute(this::worker);
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "initialized SUCCESSFULLY (backed by " + coveragePushQueue.getClass().getSimpleName() + ")");
	}

	// infallible everlasting background worker
	private void worker() {
		while (!coveragePushExecutor.isShutdown()) {
			CIPluginSDKUtils.doWait(REGULAR_CYCLE_PAUSE);
			if (coveragePushQueue.size() == 0) {
				CIPluginSDKUtils.doBreakableWait(LIST_EMPTY_INTERVAL, NO_COVERAGES_MONITOR);
				continue;
			}

			CoverageQueueItem coverageQueueItem = null;
			try {
				coverageQueueItem = coveragePushQueue.peek();
				pushCoverageWithPreflight(coverageQueueItem);
				logger.debug(configurer.octaneConfiguration.geLocationForLog() + "successfully processed " + coverageQueueItem);
				coveragePushQueue.remove();
			} catch (TemporaryException te) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() + "temporary error on " + coverageQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", te);
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pe) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() + "permanent error on " + coverageQueueItem + ", passing over", pe);
				coveragePushQueue.remove();
			} catch (Throwable t) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() + "unexpected error on build coverage item '" + coverageQueueItem + "', passing over", t);
				coveragePushQueue.remove();
			}
		}
	}

	@Override
	public boolean isSonarReportRelevant(String jobId) {
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}

		boolean result = false;
		OctaneResponse response;

		//  get result
		try {
			OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
					.setMethod(HttpMethod.GET)
					.setUrl(getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
							"servers/" + CIPluginSDKUtils.urlEncodePathParam(configurer.octaneConfiguration.getInstanceId()) +
							"/jobs/" + CIPluginSDKUtils.urlEncodePathParam(jobId) + "/workspaceId");
			response = restService.obtainOctaneRestClient().execute(preflightRequest);
			if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
				throw new TemporaryException("preflight request failed with status " + response.getStatus());
			} else if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_NO_CONTENT) {
				throw new PermanentException("preflight request failed with status " + response.getStatus());
			}
		} catch (IOException ioe) {
			throw new TemporaryException("failed to perform preflight request", ioe);
		}

		//  parse result
		if (response.getStatus() == HttpStatus.SC_OK && response.getBody() != null && !response.getBody().isEmpty()) {
			try {
				String[] wss = CIPluginSDKUtils.getObjectMapper().readValue(response.getBody(), String[].class);
				if (wss.length > 0) {
					logger.info(configurer.octaneConfiguration.geLocationForLog() + "coverage of " + jobId + " found " + wss.length + " interested workspace/s in Octane, dispatching the coverage");
					result = true;
				} else {
					logger.info(configurer.octaneConfiguration.geLocationForLog() + "coverage of " + jobId + ", found no interested workspace in Octane");
				}
			} catch (IOException ioe) {
				throw new PermanentException("failed to parse preflight response '" + response.getBody() + "' for '" + jobId + "'");
			}
		}
		return result;
	}

	@Override
	public OctaneResponse pushCoverage(String jobId, String buildId, CoverageReportType reportType, InputStream coverageReport) {
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build ID MUST NOT be null nor empty");
		}
		if (reportType == null) {
			throw new IllegalArgumentException("report type MUST NOT be null");
		}
		if (coverageReport == null) {
			throw new IllegalArgumentException("coverage report data MUST NOT be null");
		}

		String url;
		try {
			url = new URIBuilder(getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) + "coverage")
					.setParameter("ci-server-identity", configurer.octaneConfiguration.getInstanceId())
					.setParameter("ci-job-id", jobId)
					.setParameter("ci-build-id", buildId)
					.setParameter("file-type", reportType.name())
					.toString();
		} catch (URISyntaxException urise) {
			throw new PermanentException("failed to build URL to push coverage report", urise);
		}
		OctaneRequest pushCoverageRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.PUT)
				.setUrl(url)
				.setBody(coverageReport);
		try {
			return restService.obtainOctaneRestClient().execute(pushCoverageRequest);
		} catch (IOException ioe) {
			throw new TemporaryException("failed to push coverage report", ioe);
		}
	}

	@Override
	public void enqueuePushCoverage(String jobId, String buildId, CoverageReportType reportType, String reportFileName) {
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build ID MUST NOT be null nor empty");
		}
		if (reportType == null) {
			throw new IllegalArgumentException("report type MUST NOT be null");
		}
		if (!this.configurer.octaneConfiguration.isDisabled()) {
			return;
		}

		coveragePushQueue.add(new CoverageQueueItem(jobId, buildId, reportType, reportFileName));

		synchronized (NO_COVERAGES_MONITOR) {
			NO_COVERAGES_MONITOR.notify();
		}
	}

	@Override
	public void shutdown() {
		coveragePushExecutor.shutdown();
	}

	private void pushCoverageWithPreflight(CoverageQueueItem queueItem) {
		//  preflight
		if (!isSonarReportRelevant(queueItem.jobId)) {
			return;
		}

		//  get coverage report content
		InputStream coverageReport = configurer.pluginServices.getCoverageReport(queueItem.jobId, queueItem.buildId, queueItem.reportFileName);
		if (coverageReport == null) {
			logger.info(configurer.octaneConfiguration.geLocationForLog() + "no log for " + queueItem + " found, abandoning");
			return;
		}

		//  push coverage
		OctaneResponse response = pushCoverage(queueItem.jobId, queueItem.buildId, queueItem.reportType, coverageReport);
		if (response.getStatus() == HttpStatus.SC_OK) {
			logger.info(configurer.octaneConfiguration.geLocationForLog() + "successfully pushed coverage of " + queueItem);
		} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
			throw new TemporaryException("temporary failed to push coverage of " + queueItem + ", status: " + HttpStatus.SC_SERVICE_UNAVAILABLE);
		} else {
			throw new PermanentException("permanently failed to push coverage of " + queueItem + ", status: " + response.getStatus());
		}
	}

	private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART;
	}

	private static final class CoverageQueueItem implements QueueingService.QueueItem {
		private String jobId;
		private String buildId;
		private CoverageReportType reportType;
		private String reportFileName;

		//  [YG] this constructor MUST be present, don't remove
		private CoverageQueueItem() {
		}

		private CoverageQueueItem(String jobId, String buildId, CoverageReportType reportType, String reportFileName) {
			this.jobId = jobId;
			this.buildId = buildId;
			this.reportType = reportType;
			this.reportFileName = reportFileName;
		}

		@Override
		public String toString() {
			return reportType + " of '" + jobId + " #" + buildId + "' " + " [optional fileName: " + reportFileName + "]";
		}
	}

	private static final class CoveragePushWorkerThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("CoveragePushWorker-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}
}
