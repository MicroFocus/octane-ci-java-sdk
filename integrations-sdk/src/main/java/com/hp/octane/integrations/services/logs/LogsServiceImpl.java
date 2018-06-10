/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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
import com.hp.octane.integrations.services.queue.QueueService;
import com.hp.octane.integrations.util.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
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

	private final ExecutorService worker = Executors.newSingleThreadExecutor(new BuildLogsPushWorkerThreadFactory());
	private final ObjectQueue<BuildLogQueueItem> buildLogsQueue;
	private final RestService restService;

	private int SERVICE_UNAVAILABLE_BREATHE_INTERVAL = 10000;
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
		startBackgroundWorker();
	}

	@Override
	public void enqueuePushBuildLog(String jobId, String buildId) {
		buildLogsQueue.add(new BuildLogQueueItem(jobId, buildId));
	}

	//  TODO: implement retries counter per item and strategy of discard
	//  TODO: distinct between the item's problem, server problem and env problem and retry strategy accordingly
	//  this should be infallible everlasting worker
	private void startBackgroundWorker() {
		worker.execute(new Runnable() {
			public void run() {
				while (true) {
					if (buildLogsQueue.size() > 0) {
						try {
							BuildLogQueueItem buildLogQueueItem = buildLogsQueue.peek();
							InputStream log = pluginServices.getBuildLog(buildLogQueueItem.jobId, buildLogQueueItem.buildId);
							OctaneResponse response = pushBuildLog(
									pluginServices.getServerInfo().getInstanceId(),
									buildLogQueueItem.jobId,
									buildLogQueueItem.buildId,
									log);
							if (response != null && response.getStatus() == HttpStatus.SC_OK) {
								logger.info("build log dispatch round SUCCEED");
								buildLogsQueue.remove();
							} else if (response != null && response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
								logger.info("build log dispatch round FAILED, service unavailable; retrying after a breathe...");
								breathe(SERVICE_UNAVAILABLE_BREATHE_INTERVAL);
							} else {
								//  case of any other fatal error
								logger.error("build log dispatch round FAILED, status " + (response != null ? response.getStatus() : " - failed to get response") + "; dropping this item from the queue");
								buildLogsQueue.remove();
							}

						} catch (IOException e) {
							logger.error("build log dispatch round FAILED; will retry after " + SERVICE_UNAVAILABLE_BREATHE_INTERVAL + "ms", e);
							breathe(SERVICE_UNAVAILABLE_BREATHE_INTERVAL);
						} catch (Throwable t) {
							logger.error("build log dispatch round FAILED; dropping this item from the queue ", t);
							buildLogsQueue.remove();
						}
					} else {
						breathe(LIST_EMPTY_INTERVAL);
					}
				}
			}
		});
	}

	private OctaneResponse pushBuildLog(String serverId, String jobId, String buildId, InputStream log) throws IOException {
		OctaneConfiguration octaneConfiguration = pluginServices.getOctaneConfiguration();
		String encodedServerId = CIPluginSDKUtils.urlEncodePathParam(serverId);
		String encodedJobId = CIPluginSDKUtils.urlEncodePathParam(jobId);
		String encodedBuildId = CIPluginSDKUtils.urlEncodePathParam(buildId);
		OctaneResponse preflightResponse = preflight(octaneConfiguration, encodedServerId, encodedJobId);
		String[] workspaceIDs;
		if (preflightResponse.getStatus() == HttpStatus.SC_OK) {
			try {
				workspaceIDs = CIPluginSDKUtils.getObjectMapper().readValue(preflightResponse.getBody(), String[].class);
				if (workspaceIDs == null || workspaceIDs.length == 0) {
					logger.debug("no relevant workspaces found, moving to the next task");
					return preflightResponse;
				}
			} catch (Exception e) {
				throw new RuntimeException("failed to parse workspace IDs, won't retry");
			}
		} else {
			return preflightResponse;
		}

		//  submit log for each workspace returned by the 'preflight' API
		//  [YG] TODO: the code below should and will be refactored to a simpler state once Octane's APIs will be adjusted
		//  meanwhile - if any of the workspaces got successful response, this will be the response back to queue iteration
		//          - if not - the last erroneous response will be passed back to the queue iteration to decide, retry or not
		OctaneResponse anySuccessResponse = null,
				lastResponse = null;
		for (String workspaceId : workspaceIDs) {
			try {
				OctaneRequest pushLogRequest = dtoFactory.newDTO(OctaneRequest.class)
						.setMethod(HttpMethod.POST)
						.setUrl(octaneConfiguration.getUrl() + SHARED_SPACE_INTERNAL_API_PATH_PART + octaneConfiguration.getSharedSpace() +
								"workspaces/" + workspaceId + ANALYTICS_CI_PATH_PART +
								encodedServerId + "/" + encodedJobId + "/" + encodedBuildId + "/logs")
						.setBody(log);
				lastResponse = restService.obtainClient().execute(pushLogRequest);
				if (lastResponse.getStatus() == HttpStatus.SC_OK) {
					anySuccessResponse = lastResponse;
				}
			} catch (IOException ioe) {
				logger.error("failed to post log to workspace " + workspaceId + ", retrying one more time due to IOException", ioe);
				OctaneRequest pushLogRequest = dtoFactory.newDTO(OctaneRequest.class)
						.setMethod(HttpMethod.POST)
						.setUrl(octaneConfiguration.getUrl() + SHARED_SPACE_INTERNAL_API_PATH_PART + octaneConfiguration.getSharedSpace() +
								"workspaces/" + workspaceId + ANALYTICS_CI_PATH_PART +
								encodedServerId + "/" + encodedJobId + "/" + encodedBuildId + "/logs")
						.setBody(log);
				lastResponse = restService.obtainClient().execute(pushLogRequest);
				if (lastResponse.getStatus() == HttpStatus.SC_OK) {
					anySuccessResponse = lastResponse;
				}
			} catch (Exception e) {
				logger.error("failed to dispatch log to workspace " + workspaceId + ", won't retry", e);
			}
		}
		return anySuccessResponse != null ? anySuccessResponse : lastResponse;
	}

	private OctaneResponse preflight(OctaneConfiguration octaneConfiguration, String serverId, String jobId) throws IOException {
		OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setUrl(getAnalyticsContextPath(octaneConfiguration.getUrl(), octaneConfiguration.getSharedSpace()) +
						"servers/" + serverId + "/jobs/" + jobId + "/workspaceId");
		return restService.obtainClient().execute(preflightRequest);
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

	private static final class BuildLogQueueItem {
		private final String jobId;
		private final String buildId;

		private BuildLogQueueItem(String jobId, String buildId) {
			this.jobId = jobId;
			this.buildId = buildId;
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
