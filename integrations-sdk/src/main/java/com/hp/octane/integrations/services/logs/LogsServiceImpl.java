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
 *
 */

package com.hp.octane.integrations.services.logs;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.LogsService;
import com.hp.octane.integrations.api.RestService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.services.queue.QueueService;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.util.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.api.RestService.ANALYTICS_CI_PATH_PART;
import static com.hp.octane.integrations.api.RestService.SHARED_SPACE_INTERNAL_API_PATH_PART;

/**
 * Default implementation of build logs dispatching service
 */

public final class LogsServiceImpl extends OctaneSDK.SDKServiceBase implements LogsService {
	private static final Logger logger = LogManager.getLogger(LogsServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String BUILD_LOG_QUEUE_FILE = "build-logs-queue.dat";

	private final ObjectQueue<BuildLogQueueItem> buildLogsQueue;
	private final RestService restService;

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;
	private int LIST_EMPTY_INTERVAL = 3000;

	public LogsServiceImpl(Object internalUsageValidator, QueueService queueService, RestService restService) {
		super(internalUsageValidator);

		if (queueService == null) {
			throw new IllegalArgumentException("queue service MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		if (queueService.isPersistenceEnabled()) {
			buildLogsQueue = queueService.initFileQueue(BUILD_LOG_QUEUE_FILE, BuildLogQueueItem.class);
		} else {
			buildLogsQueue = queueService.initMemoQueue();
		}

		this.restService = restService;

		logger.info("starting background worker...");
		Executors
				.newSingleThreadExecutor(new BuildLogsPushWorkerThreadFactory())
				.execute(this::worker);
		logger.info("initialized SUCCESSFULLY (backed by " + buildLogsQueue.getClass().getSimpleName() + ")");
	}

	@Override
	public void enqueuePushBuildLog(String jobId, String buildId) {
		buildLogsQueue.add(new BuildLogQueueItem(jobId, buildId));
	}

	//  TODO: implement retries counter per item and strategy of discard
	//  TODO: distinct between the item's problem, server problem and env problem and retry strategy accordingly
	//  TODO: consider moving the overall queue managing logic to some generic location
	//  this should be infallible everlasting worker
	private void worker() {
		while (true) {
			if (buildLogsQueue.size() > 0) {
				BuildLogQueueItem buildLogQueueItem = null;
				try {
					buildLogQueueItem = buildLogsQueue.peek();
					pushBuildLog(pluginServices.getServerInfo().getInstanceId(), buildLogQueueItem);
					logger.debug("successfully processed " + buildLogQueueItem);
					buildLogsQueue.remove();
				} catch (TemporaryException tque) {
					logger.error("temporary error on " + buildLogQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
					breathe(TEMPORARY_ERROR_BREATHE_INTERVAL);
				} catch (PermanentException pqie) {
					logger.error("permanent error on " + buildLogQueueItem + ", passing over", pqie);
					buildLogsQueue.remove();
				} catch (Throwable t) {
					logger.error("unexpected error on build log item '" + buildLogQueueItem + "', passing over", t);
					buildLogsQueue.remove();
				}
			} else {
				breathe(LIST_EMPTY_INTERVAL);
			}
		}
	}

	private void pushBuildLog(String serverId, BuildLogQueueItem queueItem) {
		OctaneConfiguration octaneConfiguration = pluginServices.getOctaneConfiguration();
		if (octaneConfiguration == null || !octaneConfiguration.isValid()) {
			logger.warn("no (valid) Octane configuration found, bypassing " + queueItem);
			return;
		}

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
					.setUrl(octaneConfiguration.getUrl() + SHARED_SPACE_INTERNAL_API_PATH_PART + octaneConfiguration.getSharedSpace() +
							"/workspaces/" + workspaceId + ANALYTICS_CI_PATH_PART +
							encodedServerId + "/" + encodedJobId + "/" + encodedBuildId + "/logs");
			try {
				log = pluginServices.getBuildLog(queueItem.jobId, queueItem.buildId);
				if (log == null) {
					logger.info("no log for " + queueItem + " found, abandoning");
					break;
				}
				request.setBody(log);
				response = restService.obtainClient().execute(request);
				if (response.getStatus() == HttpStatus.SC_OK) {
					logger.info("successfully pushed log of " + queueItem + " to WS " + workspaceId);
				} else {
					logger.error("failed to push log of " + queueItem + " to WS " + workspaceId + ", status: " + response.getStatus());
				}
			} catch (IOException ioe) {
				logger.error("failed to push log of " + queueItem + " to WS " + workspaceId + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying one more time due to IOException", ioe);
				breathe(TEMPORARY_ERROR_BREATHE_INTERVAL);
				log = pluginServices.getBuildLog(queueItem.jobId, queueItem.buildId);
				if (log == null) {
					logger.info("no log for " + queueItem + " found, abandoning");
					break;
				}
				request.setBody(log);
				try {
					response = restService.obtainClient().execute(request);
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
					.setUrl(getAnalyticsContextPath(octaneConfiguration.getUrl(), octaneConfiguration.getSharedSpace()) +
							"servers/" + serverId + "/jobs/" + jobId + "/workspaceId");
			response = restService.obtainClient().execute(preflightRequest);
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

	private void breathe(int period) {
		try {
			Thread.sleep(period);
		} catch (InterruptedException ie) {
			logger.error("interrupted while breathing", ie);
		}
	}

	private String getAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + ANALYTICS_CI_PATH_PART;
	}

	private static final class BuildLogQueueItem implements QueueService.QueueItem {
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
