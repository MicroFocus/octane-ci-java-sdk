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

package com.hp.octane.integrations.services.bridge;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.RestClient;
import com.hp.octane.integrations.api.RestService;
import com.hp.octane.integrations.api.TasksProcessor;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.*;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.api.RestService.ACCEPT_HEADER;
import static com.hp.octane.integrations.api.RestService.ANALYTICS_CI_PATH_PART;
import static com.hp.octane.integrations.api.RestService.CONTENT_TYPE_HEADER;
import static com.hp.octane.integrations.api.RestService.SHARED_SPACE_INTERNAL_API_PATH_PART;
import static com.hp.octane.integrations.util.CIPluginSDKUtils.doWait;
import static com.hp.octane.integrations.util.CIPluginSDKUtils.urlEncodeQueryParam;

/**
 * Bridge Service meant to provide an abridged connection functionality
 */

public final class BridgeServiceImpl extends OctaneSDK.SDKServiceBase {
	private static final Logger logger = LogManager.getLogger(BridgeServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final ExecutorService connectivityExecutors = Executors.newFixedThreadPool(5, new AbridgedConnectivityExecutorsFactory());
	private final ExecutorService taskProcessingExecutors = Executors.newFixedThreadPool(30, new AbridgedTasksExecutorsFactory());

	private final RestService restService;
	private final TasksProcessor tasksProcessor;

	public BridgeServiceImpl(Object internalUsageValidator, RestService restService, TasksProcessor tasksProcessor) {
		super(internalUsageValidator);

		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}
		if (tasksProcessor == null) {
			throw new IllegalArgumentException("task processor MUST NOT be null");
		}

		this.restService = restService;
		this.tasksProcessor = tasksProcessor;

		logger.info("Starting background worker...");
		startBackgroundWorker();
	}

	//  this should be infallible everlasting worker
	private void startBackgroundWorker() {
		connectivityExecutors.execute(new Runnable() {
			public void run() {
				String tasksJSON;
				CIServerInfo serverInfo = pluginServices.getServerInfo();
				CIPluginInfo pluginInfo = pluginServices.getPluginInfo();
				String apiKey = pluginServices.getOctaneConfiguration() == null ? "" : pluginServices.getOctaneConfiguration().getApiKey();

				logger.info("Executing getAbridgedTasks...");

				try {
					//  get tasks, wait if needed and return with task or timeout or error
					tasksJSON = getAbridgedTasks(
							serverInfo.getInstanceId(),
							serverInfo.getType(),
							serverInfo.getUrl(),
							OctaneSDK.API_VERSION,
							OctaneSDK.SDK_VERSION,
							pluginInfo == null ? "" : pluginInfo.getVersion(),
							apiKey,
							serverInfo.getImpersonatedUser() == null ? "" : serverInfo.getImpersonatedUser());

					//  regardless of response - reconnect again to keep the light on
					startBackgroundWorker();


					//  now can process the received tasks - if any
					if (tasksJSON != null && !tasksJSON.isEmpty()) {
						handleTasks(tasksJSON);
					}
					logger.info("Finished backgroundWorker");
				} catch (Throwable t) {
					logger.error("connection to Octane Server temporary failed", t);
					doWait(1000);
					startBackgroundWorker();
				}
			}
		});
	}

	private String getAbridgedTasks(String selfIdentity, String selfType, String selfUrl, Integer apiVersion, String sdkVersion, String pluginVersion, String octaneUser, String ciServerUser) {
		String responseBody = null;
		RestClient restClient = restService.obtainClient();
		OctaneConfiguration octaneConfiguration = pluginServices.getOctaneConfiguration();
		if (octaneConfiguration != null && octaneConfiguration.isValid()) {
			Map<String, String> headers = new HashMap<>();
			headers.put(ACCEPT_HEADER, ContentType.APPLICATION_JSON.getMimeType());
			OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
					.setMethod(HttpMethod.GET)
					.setUrl(octaneConfiguration.getUrl() +
							SHARED_SPACE_INTERNAL_API_PATH_PART + octaneConfiguration.getSharedSpace() +
							ANALYTICS_CI_PATH_PART + "servers/" + selfIdentity + "/tasks?self-type=" + urlEncodeQueryParam(selfType) +
							"&self-url=" + urlEncodeQueryParam(selfUrl) +
							"&api-version=" + apiVersion +
							"&sdk-version=" + urlEncodeQueryParam(sdkVersion) +
							"&plugin-version=" + urlEncodeQueryParam(pluginVersion) +
							"&client-id=" + urlEncodeQueryParam(octaneUser) +
							"&ci-server-user=" + urlEncodeQueryParam(ciServerUser))
					.setHeaders(headers);
			try {
				OctaneResponse octaneResponse = restClient.execute(octaneRequest);
				if (octaneResponse.getStatus() == HttpStatus.SC_OK) {
					responseBody = octaneResponse.getBody();
				} else {
					if (octaneResponse.getStatus() == HttpStatus.SC_REQUEST_TIMEOUT) {
						logger.debug("expected timeout disconnection on retrieval of abridged tasks");
					} else if (octaneResponse.getStatus() == HttpStatus.SC_UNAUTHORIZED) {
						logger.error("connection to Octane failed: authentication error");
						doWait(5000);
					} else if (octaneResponse.getStatus() == HttpStatus.SC_FORBIDDEN) {
						logger.error("connection to Octane failed: authorization error");
						doWait(5000);
					} else if (octaneResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
						logger.error("connection to Octane failed: 404, API changes? version problem?");
						doWait(20000);
					} else {
						logger.error("unexpected response from Octane; status: " + octaneResponse.getStatus() + ", content: " + octaneResponse.getBody());
						doWait(2000);
					}
				}
			} catch (Exception e) {
				logger.error("failed to retrieve abridged tasks", e);
				doWait(2000);
			}
			return responseBody;
		} else {
			logger.info("Octane is not configured on this plugin or the configuration is not valid, breathing before next retry");
			doWait(5000);
			return null;
		}
	}

	private void handleTasks(String tasksJSON) {
		try {
			OctaneTaskAbridged[] tasks = dtoFactory.dtoCollectionFromJson(tasksJSON, OctaneTaskAbridged[].class);
			logger.info("going to process " + tasks.length + " tasks");
			for (final OctaneTaskAbridged task : tasks) {
				taskProcessingExecutors.execute(new Runnable() {
					public void run() {
						OctaneResultAbridged result = tasksProcessor.execute(task);
						int submitStatus = putAbridgedResult(
								pluginServices.getServerInfo().getInstanceId(),
								result.getId(),
								dtoFactory.dtoToJson(result));
						logger.info("result for task '" + result.getId() + "' submitted with status " + submitStatus);
					}
				});
			}
		} catch (Exception e) {
			logger.error("failed to process tasks", e);
		}
	}

	private int putAbridgedResult(String selfIdentity, String taskId, String contentJSON) {
		RestClient restClientImpl = restService.obtainClient();
		OctaneConfiguration octaneConfiguration = pluginServices.getOctaneConfiguration();
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.PUT)
				.setUrl(octaneConfiguration.getUrl() +
						SHARED_SPACE_INTERNAL_API_PATH_PART + octaneConfiguration.getSharedSpace() +
						ANALYTICS_CI_PATH_PART + "servers/" + selfIdentity + "/tasks/" + taskId + "/result")
				.setHeaders(headers)
				.setBody(contentJSON);
		try {
			OctaneResponse octaneResponse = restClientImpl.execute(octaneRequest);
			return octaneResponse.getStatus();
		} catch (IOException ioe) {
			logger.error("failed to submit abridged task's result", ioe);
			return 0;
		}
	}

	private static final class AbridgedConnectivityExecutorsFactory implements ThreadFactory {
		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("AbridgedConnectivityWorker-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}

	private static final class AbridgedTasksExecutorsFactory implements ThreadFactory {
		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("AbridgedTasksWorker-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}
}
