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
package com.hp.octane.integrations.services.logs;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.services.rest.RestService.CORRELATION_ID_HEADER;

/**
 * Default implementation of build logs dispatching service
 * com.hp.mqm.analytics.common.resources.CIAnalyticsCommonWSAResource#bdiDispatchLogs
 */

final class LogsServiceImpl implements LogsService {
	private static final Logger logger = LogManager.getLogger(LogsServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String BUILD_LOG_QUEUE_FILE = "build-logs-queue.dat";

	private final ExecutorService logsPushExecutor = Executors.newSingleThreadExecutor(new BuildLogsPushWorkerThreadFactory());
	private final ObjectQueue<BuildLogQueueItem> buildLogsQueue;
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;
	private final WorkerPreflight workerPreflight;
	private final ConfigurationService configurationService;

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;

	LogsServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService, ConfigurationService configurationService) {
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

		if (queueingService.isPersistenceEnabled()) {
			buildLogsQueue = queueingService.initFileQueue(BUILD_LOG_QUEUE_FILE, BuildLogQueueItem.class);
		} else {
			buildLogsQueue = queueingService.initMemoQueue();
		}

		this.configurer = configurer;
		this.restService = restService;
		this.configurationService = configurationService;
		this.workerPreflight = new WorkerPreflight(this, configurationService, logger);

		logger.info(configurer.octaneConfiguration.getLocationForLog() + "starting background worker...");
		logsPushExecutor.execute(this::worker);
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "initialized SUCCESSFULLY (backed by " + buildLogsQueue.getClass().getSimpleName() + ")");
	}

	@Override
	public void enqueuePushBuildLog(String jobId, String buildId, String rootJobId) {
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

		buildLogsQueue.add(new BuildLogQueueItem(jobId, buildId, rootJobId));
		workerPreflight.itemAddedToQueue();
	}

	@Override
	public void shutdown() {
		logsPushExecutor.shutdown();
	}

	@Override
	public boolean isShutdown() {
		return logsPushExecutor.isShutdown();
	}

	//  infallible everlasting background worker
	private void worker() {
		while (!logsPushExecutor.isShutdown()) {
			if(!workerPreflight.preflight()){
				continue;
			}

			BuildLogQueueItem buildLogQueueItem = null;
			try {
				buildLogQueueItem = buildLogsQueue.peek();
				pushBuildLog(configurer.octaneConfiguration.getInstanceId(), buildLogQueueItem);
				logger.debug(configurer.octaneConfiguration.getLocationForLog() + "successfully processed " + buildLogQueueItem);
				buildLogsQueue.remove();
			} catch (TemporaryException tque) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "temporary error on " + buildLogQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying");
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pqie) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "permanent error on " + buildLogQueueItem + ", passing over", pqie);
				buildLogsQueue.remove();
			} catch (Throwable t) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "unexpected error on build log item '" + buildLogQueueItem + "', passing over", t);
				buildLogsQueue.remove();
			}
		}
	}

	private void pushBuildLog(String serverId, BuildLogQueueItem queueItem) {
		OctaneConfiguration octaneConfiguration = configurer.octaneConfiguration;
		String encodedServerId = CIPluginSDKUtils.urlEncodePathParam(serverId);
		String encodedBuildId = CIPluginSDKUtils.urlEncodePathParam(queueItem.buildId);

		boolean base64 = isEncodeBase64();
		String encodedJobId = base64 ? CIPluginSDKUtils.urlEncodeBase64(queueItem.jobId) : CIPluginSDKUtils.urlEncodePathParam(queueItem.jobId);
		String encodedRootJobId = base64 ? CIPluginSDKUtils.urlEncodeBase64(queueItem.rootJobId) : CIPluginSDKUtils.urlEncodeQueryParam(queueItem.rootJobId);

		//  preflight
		String[] workspaceIDs = preflightRequest(octaneConfiguration, encodedServerId, encodedJobId, encodedRootJobId, base64);
		if (workspaceIDs.length == 0) {
			logger.info(configurer.octaneConfiguration.getLocationForLog() + "log of " + queueItem + ", no interested workspace is found");
			return;
		} else {
			logger.info(configurer.octaneConfiguration.getLocationForLog() + "log of " + queueItem + ", found " + workspaceIDs.length + " interested workspace/s");
		}

		//  submit log for each workspace returned by the 'preflight' API
		//  [YG] TODO: the code below should and will be refactored to a simpler state once Octane's APIs will be adjusted
		InputStream log;
		OctaneRequest request;
		OctaneResponse response;
		for (String workspaceId : workspaceIDs) {
			String url = octaneConfiguration.getUrl() + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + octaneConfiguration.getSharedSpace() +
					"/workspaces/" + workspaceId + RestService.ANALYTICS_CI_PATH_PART +
					encodedServerId + "/" + encodedJobId + "/" + encodedBuildId + "/logs";
			if (base64) {
				url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
			}

			String correlationId = CIPluginSDKUtils.getNextCorrelationId();
			Map<String, String> headers = new HashMap<>();
			headers.put(CORRELATION_ID_HEADER, correlationId);
			request = dtoFactory.newDTO(OctaneRequest.class)
					.setMethod(HttpMethod.POST)
					.setHeaders(headers)
					.setUrl(url);
			try {
				log = configurer.pluginServices.getBuildLog(queueItem.jobId, queueItem.buildId);
				if (log == null) {
					logger.info(configurer.octaneConfiguration.getLocationForLog() + "no log for " + queueItem + " found, abandoning");
					break;
				}
				request.setBody(log);
				response = restService.obtainOctaneRestClient().execute(request);
				if (response.getStatus() == HttpStatus.SC_OK) {
					logger.info(configurer.octaneConfiguration.getLocationForLog() + "successfully pushed log of " + queueItem + " to WS " + workspaceId + ", correlation Id = " + correlationId);
				} else {
					logger.error(configurer.octaneConfiguration.getLocationForLog() + "failed to push log of " + queueItem + " to WS " + workspaceId + ", status: " + response.getStatus() + ", correlation Id = " + correlationId);
				}
			} catch (IOException ioe) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "failed to push log of " + queueItem + " to WS " + workspaceId + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying one more time due to IOException", ioe);
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
				log = configurer.pluginServices.getBuildLog(queueItem.jobId, queueItem.buildId);
				if (log == null) {
					logger.info(configurer.octaneConfiguration.getLocationForLog() + "no log for " + queueItem + " found, abandoning");
					break;
				}
				request.setBody(log);
				try {
					response = restService.obtainOctaneRestClient().execute(request);
					if (response.getStatus() == HttpStatus.SC_OK) {
						logger.info(configurer.octaneConfiguration.getLocationForLog() + "successfully pushed log of " + queueItem + " to WS " + workspaceId);
					} else {
						logger.error(configurer.octaneConfiguration.getLocationForLog() + "failed to push log of " + queueItem + " to WS " + workspaceId + ", status: " + response.getStatus());
					}
				} catch (IOException ioem) {
					logger.error(configurer.octaneConfiguration.getLocationForLog() + "failed to push log of " + queueItem + " to WS " + workspaceId + " for the second time, abandoning", ioem);
				}
			}
		}
	}

	private boolean isEncodeBase64() {
		return ConfigurationParameterFactory.isEncodeCiJobBase64(configurer.octaneConfiguration);
	}

	private String[] preflightRequest(OctaneConfiguration octaneConfiguration, String encodedServerId, String encodedJobId, String encodedRootJobId, boolean base64) {
		String[] result = new String[0];
		OctaneResponse response;

		//  get result
		String url = getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), octaneConfiguration.getSharedSpace()) +
				"servers/" + encodedServerId + "/jobs/" + encodedJobId + "/workspaceId";
		if (encodedRootJobId != null && !encodedRootJobId.isEmpty()) {
			url += "?rootJobId=" + encodedRootJobId;
		}
		if (base64) {
			url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
		}
		try {
			OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
					.setMethod(HttpMethod.GET)
					.setUrl(url);

			response = restService.obtainOctaneRestClient().execute(preflightRequest);
			if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
				throw new TemporaryException("preflight request failed with status " + response.getStatus());
			} else if (response.getStatus() == HttpStatus.SC_UNAUTHORIZED || response.getStatus() == HttpStatus.SC_FORBIDDEN) {
				CIPluginSDKUtils.doWait(30000);
				throw new PermanentException("preflight request failed with status " + response.getStatus());
			} else if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_NO_CONTENT) {
				throw new PermanentException("preflight request failed with status " + response.getStatus()  + ". JobId: '" + encodedJobId + "'. Request URL : " + url);
			}
		} catch (IOException ioe) {
			throw new TemporaryException(ioe);
		}

		//  parse result
		if (response.getBody() != null && !response.getBody().isEmpty()) {
			try {
				result = CIPluginSDKUtils.getObjectMapper().readValue(response.getBody(), String[].class);
			} catch (IOException ioe) {
				if (CIPluginSDKUtils.isServiceTemporaryUnavailable(response.getBody())) {
					throw new TemporaryException("Saas service is temporary unavailable.");
				} else {
					throw new PermanentException("failed to parse preflight response '" + response.getBody() + "' for '" + encodedJobId + "'");
				}
			}
		}
		return result;
	}

	private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART;
	}

	@Override
	public long getQueueSize() {
		return buildLogsQueue.size();
	}

	@Override
	public void clearQueue() {
		while (buildLogsQueue.size() > 0) {
			buildLogsQueue.remove();
		}
	}

	@Override
	public Map<String, Object> getMetrics() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("queueSize", this.getQueueSize());
		workerPreflight.addMetrics(map);
		return map;
	}

	private static final class BuildLogQueueItem implements QueueingService.QueueItem {
		private String jobId;
		private String buildId;
		private String rootJobId;

		//  [YG] this constructor MUST be present, don't remove
		private BuildLogQueueItem() {
		}

		private BuildLogQueueItem(String jobId, String buildId, String rootJobId) {
			this.jobId = jobId;
			this.buildId = buildId;
			this.rootJobId = rootJobId;
		}

		@Override
		public String toString() {
			return "'" + jobId + " #" + buildId + "', root job : " + rootJobId;
		}
	}

	private static final class BuildLogsPushWorkerThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("BuildLogsPushWorker-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}
}
