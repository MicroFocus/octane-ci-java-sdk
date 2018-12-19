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

package com.hp.octane.integrations.services.logs;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of build logs dispatching service
 */

final class LogsServiceImpl implements LogsService {
	private static final Logger logger = LogManager.getLogger(LogsServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String BUILD_LOG_QUEUE_FILE = "build-logs-queue.dat";

	private final ExecutorService logsPushExecutor = Executors.newSingleThreadExecutor(new BuildLogsPushWorkerThreadFactory());
	private final Object NO_LOGS_MONITOR = new Object();
	private final ObjectQueue<BuildLogQueueItem> buildLogsQueue;
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;
	private int LIST_EMPTY_INTERVAL = 3000;

	LogsServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService) {
		if (configurer == null || configurer.pluginServices == null || configurer.octaneConfiguration == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (queueingService == null) {
			throw new IllegalArgumentException("queue service MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		if (queueingService.isPersistenceEnabled()) {
			buildLogsQueue = queueingService.initFileQueue(BUILD_LOG_QUEUE_FILE, BuildLogQueueItem.class);
		} else {
			buildLogsQueue = queueingService.initMemoQueue();
		}

		this.configurer = configurer;
		this.restService = restService;

		logger.info("starting background worker...");
		logsPushExecutor.execute(this::worker);
		logger.info("initialized SUCCESSFULLY (backed by " + buildLogsQueue.getClass().getSimpleName() + ")");
	}

	@Override
	public void enqueuePushBuildLog(String jobId, String buildId) {
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
		}
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build ID MUST NOT be null nor empty");
		}

		buildLogsQueue.add(new BuildLogQueueItem(jobId, buildId));
		synchronized (NO_LOGS_MONITOR) {
			NO_LOGS_MONITOR.notify();
		}
	}

	@Override
	public void shutdown() {
		logsPushExecutor.shutdown();
	}

	//  infallible everlasting background worker
	private void worker() {
		while (!logsPushExecutor.isShutdown()) {
			if (buildLogsQueue.size() == 0) {
				CIPluginSDKUtils.doBreakableWait(LIST_EMPTY_INTERVAL, NO_LOGS_MONITOR);
				continue;
			}

			BuildLogQueueItem buildLogQueueItem = null;
			try {
				buildLogQueueItem = buildLogsQueue.peek();
				pushBuildLog(configurer.octaneConfiguration.getInstanceId(), buildLogQueueItem);
				logger.debug("successfully processed " + buildLogQueueItem);
				buildLogsQueue.remove();
			} catch (TemporaryException tque) {
				logger.error("temporary error on " + buildLogQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pqie) {
				logger.error("permanent error on " + buildLogQueueItem + ", passing over", pqie);
				buildLogsQueue.remove();
			} catch (Throwable t) {
				logger.error("unexpected error on build log item '" + buildLogQueueItem + "', passing over", t);
				buildLogsQueue.remove();
			}
		}
	}

	private void pushBuildLog(String serverId, BuildLogQueueItem queueItem) {
		OctaneConfiguration octaneConfiguration = configurer.octaneConfiguration;
		String encodedServerId = CIPluginSDKUtils.urlEncodePathParam(serverId);
		String encodedJobId = CIPluginSDKUtils.urlEncodePathParam(queueItem.jobId);
		String encodedBuildId = CIPluginSDKUtils.urlEncodePathParam(queueItem.buildId);

		//  preflight
		String[] workspaceIDs = preflightRequest(octaneConfiguration, encodedServerId, encodedJobId);
		if (workspaceIDs.length == 0) {
			logger.info("log of " + queueItem + " found no interested workspace in Octane, passing over");
			return;
		} else {
			logger.info("log of " + queueItem + " found " + workspaceIDs.length + " interested workspace/s in Octane, dispatching the log");
		}

		//  submit log for each workspace returned by the 'preflight' API
		//  [YG] TODO: the code below should and will be refactored to a simpler state once Octane's APIs will be adjusted
		InputStream log;
		OctaneRequest request;
		OctaneResponse response;
		for (String workspaceId : workspaceIDs) {
			request = dtoFactory.newDTO(OctaneRequest.class)
					.setMethod(HttpMethod.POST)
					.setUrl(octaneConfiguration.getUrl() + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + octaneConfiguration.getSharedSpace() +
							"/workspaces/" + workspaceId + RestService.ANALYTICS_CI_PATH_PART +
							encodedServerId + "/" + encodedJobId + "/" + encodedBuildId + "/logs");
			try {
				log = configurer.pluginServices.getBuildLog(queueItem.jobId, queueItem.buildId);
				if (log == null) {
					logger.info("no log for " + queueItem + " found, abandoning");
					break;
				}
				request.setBody(log);
				response = restService.obtainOctaneRestClient().execute(request);
				if (response.getStatus() == HttpStatus.SC_OK) {
					logger.info("successfully pushed log of " + queueItem + " to WS " + workspaceId);
				} else {
					logger.error("failed to push log of " + queueItem + " to WS " + workspaceId + ", status: " + response.getStatus());
				}
			} catch (IOException ioe) {
				logger.error("failed to push log of " + queueItem + " to WS " + workspaceId + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying one more time due to IOException", ioe);
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
				log = configurer.pluginServices.getBuildLog(queueItem.jobId, queueItem.buildId);
				if (log == null) {
					logger.info("no log for " + queueItem + " found, abandoning");
					break;
				}
				request.setBody(log);
				try {
					response = restService.obtainOctaneRestClient().execute(request);
					if (response.getStatus() == HttpStatus.SC_OK) {
						logger.info("successfully pushed log of " + queueItem + " to WS " + workspaceId);
					} else {
						logger.error("failed to push log of " + queueItem + " to WS " + workspaceId + ", status: " + response.getStatus());
					}
				} catch (IOException ioem) {
					logger.error("failed to push log of " + queueItem + " to WS " + workspaceId + " for the second time, abandoning", ioem);
				}
			}
		}
	}

	private String[] preflightRequest(OctaneConfiguration octaneConfiguration, String serverId, String jobId) {
		String[] result = new String[0];
		OctaneResponse response;

		//  get result
		try {
			OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
					.setMethod(HttpMethod.GET)
					.setUrl(getAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), octaneConfiguration.getSharedSpace()) +
							"servers/" + serverId + "/jobs/" + jobId + "/workspaceId");
			response = restService.obtainOctaneRestClient().execute(preflightRequest);
			if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
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

	private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART;
	}

	private static final class BuildLogQueueItem implements QueueingService.QueueItem {
		private String jobId;
		private String buildId;

		//  [YG] this constructor MUST be present, don't remove
		private BuildLogQueueItem() {
		}

		private BuildLogQueueItem(String jobId, String buildId) {
			this.jobId = jobId;
			this.buildId = buildId;
		}

		@Override
		public String toString() {
			return "'" + jobId + " #" + buildId + "'";
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
