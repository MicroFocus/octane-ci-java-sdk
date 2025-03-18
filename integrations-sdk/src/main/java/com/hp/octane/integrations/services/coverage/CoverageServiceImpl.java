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
package com.hp.octane.integrations.services.coverage;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.coverage.CoverageReportType;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.WorkerPreflight;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.configuration.ConfigurationServiceImpl;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.services.rest.RestService.CORRELATION_ID_HEADER;

/**
 * Default implementation of Coverage Service
 * com.hp.mqm.analytics.common.resources.CIAnalyticsCommonSSAResource#pushCoverage
 */

class CoverageServiceImpl implements CoverageService {
	private static final Logger logger = LogManager.getLogger(CoverageServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final ExecutorService coveragePushExecutor = Executors.newSingleThreadExecutor(new CoveragePushWorkerThreadFactory());
	private final String BUILD_COVERAGE_QUEUE_FILE = "coverage-push-queue.dat";
	private final ObjectQueue<CoverageQueueItem> coveragePushQueue;
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;
	protected final ConfigurationService configurationService;
	private final WorkerPreflight workerPreflight;

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;

	CoverageServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService, ConfigurationService configurationService) {
		if (configurer == null || configurer.pluginServices == null || configurer.octaneConfiguration == null) {
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

		this.configurer = configurer;
		this.restService = restService;
		this.configurationService = configurationService;
		this.workerPreflight = new WorkerPreflight(this, configurationService, logger);

		if (queueingService.isPersistenceEnabled()) {
			coveragePushQueue = queueingService.initFileQueue(BUILD_COVERAGE_QUEUE_FILE, CoverageQueueItem.class);
		} else {
			coveragePushQueue = queueingService.initMemoQueue();
		}

		logger.info(configurer.octaneConfiguration.getLocationForLog() + "starting background worker...");
		coveragePushExecutor.execute(this::worker);
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "initialized SUCCESSFULLY (backed by " + coveragePushQueue.getClass().getSimpleName() + ")");
	}

	// infallible everlasting background worker
	private void worker() {
		while (!coveragePushExecutor.isShutdown()) {
			if(!workerPreflight.preflight()){
				continue;
			}

			CoverageQueueItem coverageQueueItem = null;
			try {
				coverageQueueItem = coveragePushQueue.peek();
				pushCoverageWithPreflight(coverageQueueItem);
				logger.debug(configurer.octaneConfiguration.getLocationForLog() + "successfully processed " + coverageQueueItem);
				coveragePushQueue.remove();
			} catch (TemporaryException te) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "temporary error on " + coverageQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", te);
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pe) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "permanent error on " + coverageQueueItem + ", passing over", pe);
				coveragePushQueue.remove();
			} catch (Throwable t) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "unexpected error on build coverage item '" + coverageQueueItem + "', passing over", t);
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

		boolean base64 = isEncodeBase64();
		String encodedJobId = base64 ? CIPluginSDKUtils.urlEncodeBase64(jobId) : CIPluginSDKUtils.urlEncodePathParam(jobId);

		//  get result
		try {
			String url = getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
					"servers/" + CIPluginSDKUtils.urlEncodePathParam(configurer.octaneConfiguration.getInstanceId()) +
					"/jobs/" + encodedJobId + "/workspaceId";
			if (base64) {
				url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
			}

			OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class).setMethod(HttpMethod.GET).setUrl(url);

			response = restService.obtainOctaneRestClient().execute(preflightRequest);
			if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY || response.getStatus() == 429) {
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
					logger.info(configurer.octaneConfiguration.getLocationForLog() + "coverage of " + jobId + " found " + wss.length + " interested workspace/s in Octane, dispatching the coverage");
					result = true;
				} else {
					logger.info(configurer.octaneConfiguration.getLocationForLog() + "coverage of " + jobId + ", found no interested workspace in Octane");
				}
			} catch (IOException ioe) {
				throw new PermanentException("failed to parse preflight response '" + response.getBody() + "' for '" + jobId + "'");
			}
		}
		return result;
	}

	private boolean isEncodeBase64() {
		return ConfigurationParameterFactory.isEncodeCiJobBase64(configurer.octaneConfiguration);
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
		boolean base64 = isEncodeBase64();
		String tempJobId = jobId;
		if (base64) {
			tempJobId = CIPluginSDKUtils.urlEncodeBase64(jobId);
		}

		String url;
		try {
			url = new URIBuilder(getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) + "coverage")
					.setParameter("ci-server-identity", configurer.octaneConfiguration.getInstanceId())
					.setParameter("ci-job-id", tempJobId)
					.setParameter("ci-build-id", buildId)
					.setParameter("file-type", reportType.name())
					.toString();
			if (base64) {
				url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
			}
		} catch (URISyntaxException urise) {
			throw new PermanentException("failed to build URL to push coverage report", urise);
		}

		String correlationId = CIPluginSDKUtils.getNextCorrelationId();
		Map<String, String> headers = new HashMap<>();
		headers.put(CORRELATION_ID_HEADER, correlationId);
		OctaneRequest pushCoverageRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.PUT)
				.setUrl(url)
				.setHeaders(headers)
				.setBody(coverageReport);
		try {
			return restService.obtainOctaneRestClient().execute(pushCoverageRequest);
		} catch (IOException ioe) {
			throw new TemporaryException("failed to push coverage report", ioe);
		}
	}

	@Override
	public void enqueuePushCoverage(String jobId, String buildId, CoverageReportType reportType, String reportFileName, String rootJobId) {
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build ID MUST NOT be null nor empty");
		}
		if (reportType == null) {
			throw new IllegalArgumentException("report type MUST NOT be null");
		}
		if (this.configurer.octaneConfiguration.isDisabled()) {
			return;
		}
		if (!((ConfigurationServiceImpl) configurationService).isRelevantForOctane(rootJobId)) {
			return;
		}

		coveragePushQueue.add(new CoverageQueueItem(jobId, buildId, reportType, reportFileName));
		workerPreflight.itemAddedToQueue();
	}

	@Override
	public void shutdown() {
		coveragePushExecutor.shutdown();
	}

	@Override
	public boolean isShutdown() {
		return coveragePushExecutor.isShutdown();
	}

	private void pushCoverageWithPreflight(CoverageQueueItem queueItem) {
		//  preflight
		if (!isSonarReportRelevant(queueItem.jobId)) {
			return;
		}

		//  get coverage report content
		InputStream coverageReport = configurer.pluginServices.getCoverageReport(queueItem.jobId, queueItem.buildId, queueItem.reportFileName);
		if (coverageReport == null) {
			logger.info(configurer.octaneConfiguration.getLocationForLog() + "no log for " + queueItem + " found, abandoning");
			return;
		}

		//  push coverage
		OctaneResponse response = pushCoverage(queueItem.jobId, queueItem.buildId, queueItem.reportType, coverageReport);
		if (response.getStatus() == HttpStatus.SC_OK) {
			logger.info(configurer.octaneConfiguration.getLocationForLog() + "successfully pushed coverage of " + queueItem + ", CorrelationId - " + response.getCorrelationId());
		} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY || response.getStatus() == 429) {
			throw new TemporaryException("temporary failed to push coverage of " + queueItem + ", status: " + HttpStatus.SC_SERVICE_UNAVAILABLE);
		} else {
			throw new PermanentException("permanently failed to push coverage of " + queueItem + ", status: " + response.getStatus());
		}
	}

	private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART;
	}

	@Override
	public long getQueueSize() {
		return coveragePushQueue.size();
	}

	@Override
	public void clearQueue() {
		while (coveragePushQueue.size() > 0) {
			coveragePushQueue.remove();
		}
	}

	@Override
	public Map<String, Object> getMetrics() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("queueSize", this.getQueueSize());
		workerPreflight.addMetrics(map);
		return map;
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
