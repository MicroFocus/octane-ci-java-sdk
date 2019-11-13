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

package com.hp.octane.integrations.services.bridge;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.*;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.general.CIServerTypes;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.tasking.TasksProcessor;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Bridge Service meant to provide an abridged connection functionality
 */

final class BridgeServiceImpl implements BridgeService {
	private static final Logger logger = LogManager.getLogger(BridgeServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final ExecutorService connectivityExecutors = Executors.newFixedThreadPool(5, new AbridgedConnectivityExecutorsFactory());
	private final ExecutorService taskProcessingExecutors = Executors.newFixedThreadPool(30, new AbridgedTasksExecutorsFactory());

	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;
	private final TasksProcessor tasksProcessor;
	private long lastLogTime = 0;
	private final static long MILLI_TO_HOUR = 1000 * 60 * 60;

	private long  lastRequestToOctaneTime = 0;
	private ServiceState serviceState = ServiceState.Initial;
	private long stateStartTime = 0;

	BridgeServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService, TasksProcessor tasksProcessor) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}
		if (tasksProcessor == null) {
			throw new IllegalArgumentException("task processor MUST NOT be null");
		}

		this.configurer = configurer;
		this.restService = restService;
		this.tasksProcessor = tasksProcessor;

		logger.info(configurer.octaneConfiguration.geLocationForLog() + "starting background worker...");
		connectivityExecutors.execute(this::worker);
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "initialized SUCCESSFULLY");
	}

	public Map<String,Object>getMetrics(){
		Map<String,Object> map = new HashMap<>();
		map.put("state", serviceState.name());
		map.put("stateStartTime", new Date(stateStartTime));
		map.put("lastRequestToOctaneTime", new Date(lastRequestToOctaneTime));
		return map;
	}

	@Override
	public void shutdown() {
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "shutdown");
		connectivityExecutors.shutdown();
		taskProcessingExecutors.shutdown();
		changeServiceState(ServiceState.Closed);
	}

	//  infallible everlasting background worker
	private void worker() {
		try {
			String tasksJSON;
			CIServerInfo serverInfo = configurer.pluginServices.getServerInfo();
			CIPluginInfo pluginInfo = configurer.pluginServices.getPluginInfo();
			String client = configurer.octaneConfiguration.getClient();

			// add log about activity once a hour
			if (hoursDifference(System.currentTimeMillis(), lastLogTime) >= 1) {
				logger.info(configurer.octaneConfiguration.geLocationForLog() + "task polling is active");
				lastLogTime = System.currentTimeMillis();
			}

			changeServiceState(ServiceState.WaitingToOctane);

			//  get tasks, wait if needed and return with task or timeout or error
			tasksJSON = getAbridgedTasks(
					configurer.octaneConfiguration.getInstanceId(),
					serverInfo.getType() == null ? CIServerTypes.UNKNOWN.value() : serverInfo.getType(),
					serverInfo.getUrl() == null ? "" : serverInfo.getUrl(),
					pluginInfo == null || pluginInfo.getVersion() == null ? "" : pluginInfo.getVersion(),
					client == null ? "" : client,
					serverInfo.getImpersonatedUser() == null ? "" : serverInfo.getImpersonatedUser());

			//  regardless of response - reconnect again to keep the light on
			if (!connectivityExecutors.isShutdown()) {
				connectivityExecutors.execute(this::worker);
			} else {
				changeServiceState(ServiceState.StopProcessing);
				logger.info(configurer.octaneConfiguration.geLocationForLog() + "Shutdown flag is up - stop task processing");
			}

			//  now can process the received tasks - if any
			if (tasksJSON != null && !tasksJSON.isEmpty()) {
				changeServiceState(ServiceState.HandleTask);
				handleTasks(tasksJSON);
			}
		} catch (Throwable t) {
			logger.error(configurer.octaneConfiguration.geLocationForLog() + "getting tasks from Octane Server temporary failed. Breathing 2 secs.", t);
			CIPluginSDKUtils.doWait(2000);
			if (!connectivityExecutors.isShutdown()) {
				connectivityExecutors.execute(this::worker);
			} else {
				changeServiceState(ServiceState.StopProcessing);
				logger.info(configurer.octaneConfiguration.geLocationForLog() + "Shutdown flag is up - stop task processing");
			}
		}
	}

	private void changeServiceState(ServiceState newState){
		serviceState = newState;
		stateStartTime = System.currentTimeMillis();
		if(newState.equals(ServiceState.WaitingToOctane)){
			lastRequestToOctaneTime = System.currentTimeMillis();
		}
		//logger.info(configurer.octaneConfiguration.geLocationForLog() + "State changed to " + newState);
	}

	private String getAbridgedTasks(String selfIdentity, String selfType, String selfUrl, String pluginVersion, String octaneUser, String ciServerUser) {
		String responseBody = null;
		OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(RestService.ACCEPT_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setUrl(configurer.octaneConfiguration.getUrl() +
						RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + configurer.octaneConfiguration.getSharedSpace() +
						RestService.ANALYTICS_CI_PATH_PART + "servers/" + selfIdentity + "/tasks?self-type=" + CIPluginSDKUtils.urlEncodeQueryParam(selfType) +
						"&self-url=" + CIPluginSDKUtils.urlEncodeQueryParam(selfUrl) +
						"&api-version=" + OctaneSDK.API_VERSION +
						"&sdk-version=" + CIPluginSDKUtils.urlEncodeQueryParam(OctaneSDK.SDK_VERSION) +
						"&plugin-version=" + CIPluginSDKUtils.urlEncodeQueryParam(pluginVersion) +
						"&client-id=" + CIPluginSDKUtils.urlEncodeQueryParam(octaneUser) +
						"&ci-server-user=" + CIPluginSDKUtils.urlEncodeQueryParam(ciServerUser))
				.setHeaders(headers);
		try {
			OctaneResponse octaneResponse = octaneRestClient.execute(octaneRequest);
			if (octaneResponse.getStatus() == HttpStatus.SC_OK) {
				responseBody = octaneResponse.getBody();

				if (isServiceTemporaryUnavailable(responseBody)) {
					logger.error("Saas service is temporary unavailable. Breathing 180 secs.");
					changeServiceState(ServiceState.PostponingOnException);
					CIPluginSDKUtils.doWait(180000);
					responseBody = null;
				}

			} else {
				if (octaneResponse.getStatus() == HttpStatus.SC_NO_CONTENT) {
					logger.debug(configurer.octaneConfiguration.geLocationForLog() + "no tasks found on server");
				} else if (octaneResponse.getStatus() == HttpStatus.SC_REQUEST_TIMEOUT) {
					logger.debug(configurer.octaneConfiguration.geLocationForLog() + "expected timeout disconnection on retrieval of abridged tasks, reconnecting immediately...");
				} else if (octaneResponse.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || octaneResponse.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
					logger.error(configurer.octaneConfiguration.geLocationForLog() + "Octane service is unavailable, breathing 30 secs.");
					changeServiceState(ServiceState.PostponingOnException);
					CIPluginSDKUtils.doWait(30000);
				} else if (octaneResponse.getStatus() == HttpStatus.SC_UNAUTHORIZED) {
					logger.error(configurer.octaneConfiguration.geLocationForLog() + "connection to Octane failed: authentication error. Breathing 30 secs.");
					changeServiceState(ServiceState.PostponingOnException);
					CIPluginSDKUtils.doWait(30000);
				} else if (octaneResponse.getStatus() == HttpStatus.SC_FORBIDDEN) {
					logger.error(configurer.octaneConfiguration.geLocationForLog() + "connection to Octane failed: authorization error. Breathing 30 secs.");
					changeServiceState(ServiceState.PostponingOnException);
					CIPluginSDKUtils.doWait(30000);
				} else if (octaneResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
					logger.error(configurer.octaneConfiguration.geLocationForLog() + "connection to Octane failed: 404, API changes? version problem?. Breathing 180 secs.");
					changeServiceState(ServiceState.PostponingOnException);
					CIPluginSDKUtils.doWait(180000);
				} else if (octaneResponse.getStatus() == HttpStatus.SC_TEMPORARY_REDIRECT) {
					logger.error(configurer.octaneConfiguration.geLocationForLog() + "task polling request is redirected. Possibly Octane service is unavailable now. Breathing 30 secs.");
					changeServiceState(ServiceState.PostponingOnException);
					CIPluginSDKUtils.doWait(30000);
				} else {
					String output = octaneResponse.getBody() == null ? "" : octaneResponse.getBody().substring(0, Math.min(octaneResponse.getBody().length(), 2000));//don't print more that 2000 characters
					logger.error(configurer.octaneConfiguration.geLocationForLog() + "unexpected response from Octane; status: " + octaneResponse.getStatus() + ", content: " + output +". Breathing 10 secs.");
					changeServiceState(ServiceState.PostponingOnException);
					CIPluginSDKUtils.doWait(10000);
				}
			}
		} catch (SocketException | UnknownHostException e) {
			logger.error(configurer.octaneConfiguration.geLocationForLog() + "failed to retrieve abridged tasks. ALM Octane Server is not accessible : " + e.getClass().getCanonicalName() + " - " + e.getMessage() + ". Breathing 30 secs.");
			changeServiceState(ServiceState.PostponingOnException);
			CIPluginSDKUtils.doWait(30000);
		} catch (IOException ioe) {
			logger.error(configurer.octaneConfiguration.geLocationForLog() + "failed to retrieve abridged tasks. Breathing 30 secs.", ioe);
			changeServiceState(ServiceState.PostponingOnException);
			CIPluginSDKUtils.doWait(30000);
		} catch (Throwable t) {
			logger.error(configurer.octaneConfiguration.geLocationForLog() + "unexpected error during retrieval of abridged tasks. Breathing 30 secs.", t);
			changeServiceState(ServiceState.PostponingOnException);
			CIPluginSDKUtils.doWait(30000);
		}
		return responseBody;
	}

	private void handleTasks(String tasksJSON) {
		changeServiceState(ServiceState.HandleTask);
		try {
			logger.info(configurer.octaneConfiguration.geLocationForLog() + "parsing tasks...");
			OctaneTaskAbridged[] tasks = dtoFactory.dtoCollectionFromJson(tasksJSON, OctaneTaskAbridged[].class);
			logger.info(configurer.octaneConfiguration.geLocationForLog() + "parsed " + tasks.length + " tasks, processing...");
			for (final OctaneTaskAbridged task : tasks) {
				if (taskProcessingExecutors.isShutdown()) {
					break;
				}
				taskProcessingExecutors.execute(() -> {
					OctaneResultAbridged result = tasksProcessor.execute(task);
					int submitStatus = putAbridgedResult(
							configurer.octaneConfiguration.getInstanceId(),
							result.getId(),
							dtoFactory.dtoToJsonStream(result));
					logger.info(configurer.octaneConfiguration.geLocationForLog() + "result for task '" + result.getId() + "' submitted with status " + submitStatus);
				});
			}
		} catch (Exception e) {
			logger.error("failed to process tasks", e);
		}
	}

	private boolean isServiceTemporaryUnavailable(String tasksJSON) {
		return tasksJSON != null && tasksJSON.contains("Service Temporary Unavailable");
	}

	private int putAbridgedResult(String selfIdentity, String taskId, InputStream contentJSON) {
		OctaneRestClient octaneRestClientImpl = restService.obtainOctaneRestClient();
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.PUT)
				.setUrl(configurer.octaneConfiguration.getUrl() +
						RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + configurer.octaneConfiguration.getSharedSpace() +
						RestService.ANALYTICS_CI_PATH_PART + "servers/" + selfIdentity + "/tasks/" + taskId + "/result")
				.setHeaders(headers)
				.setBody(contentJSON);
		try {
			OctaneResponse octaneResponse = octaneRestClientImpl.execute(octaneRequest);
			return octaneResponse.getStatus();
		} catch (IOException ioe) {
			logger.error(configurer.octaneConfiguration.geLocationForLog() + "failed to submit abridged task's result", ioe);
			return 0;
		}
	}

	public long getLastRequestToOctaneTime() {
		return lastRequestToOctaneTime;
	}

	public ServiceState getServiceState() {
		return serviceState;
	}

	public long getStateStartTime() {
		return stateStartTime;
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

	private static long hoursDifference(long date1, long date2) {
		return (date1 - date2) / MILLI_TO_HOUR;
	}
}
