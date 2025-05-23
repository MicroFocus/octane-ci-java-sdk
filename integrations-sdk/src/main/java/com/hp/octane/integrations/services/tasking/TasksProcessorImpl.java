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
package com.hp.octane.integrations.services.tasking;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.*;
import com.hp.octane.integrations.dto.executor.CredentialsInfo;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.general.*;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameters;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.exceptions.ErrorCodeBasedException;
import com.hp.octane.integrations.exceptions.SPIMethodNotImplementedException;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.utils.SdkConstants;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Tasks routing service handles ALM Octane tasks, both coming from abridged logic as well as plugin's REST call delegation
 *
 * Sent from octane : CIServersServiceImpl, CIExecutorsServiceImpl
 */

final class TasksProcessorImpl implements TasksProcessor {
	private static final Logger logger = LogManager.getLogger(TasksProcessorImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private static final String NGA_API = "nga/api/v1";
	private static final String STATUS = "status";
	private static final String SUSPEND_STATUS = "suspend_status";
	private static final String JOBS = "jobs";
	private static final String RUN = "run";
	private static final String STOP = "stop";
	private static final String BUILD_STATUS = "build_status";
	private static final String BRANCHES = "branches";
	private static final String BUILDS = "builds";
	private static final String EXECUTOR = "executor";
	private static final String INIT = "init";
	private static final String UPDATE = "update";
	private static final String TEST_CONN = "test_conn";
	private static final String CREDENTIALS_UPSERT = "credentials_upsert";
	private static final String CREDENTIALS = "credentials";
	private static final String SYNC_NOW = "sync_now";

	private ExecutorService jobListCacheExecutor = Executors.newSingleThreadExecutor();
	private CacheItem jobListCacheItem;

	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final ConfigurationService configurationService;

	TasksProcessorImpl(OctaneSDK.SDKServicesConfigurer configurer, ConfigurationService configurationService) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if(configurationService == null){
			throw new IllegalArgumentException("configurationService shouldnot be null");
		}

		this.configurationService = configurationService;
		this.configurer = configurer;
	}

	@Override
	public OctaneResultAbridged execute(OctaneTaskAbridged task) {
		if (task == null) {
			throw new IllegalArgumentException("task MUST NOT be null");
		}
		if (task.getUrl() == null || task.getUrl().isEmpty()) {
			throw new IllegalArgumentException("task 'URL' MUST NOT be null nor empty");
		}
		if (!task.getUrl().contains(NGA_API)) {
			throw new IllegalArgumentException("task 'URL' expected to contain '" + NGA_API + "'; wrong handler call?");
		}
		long startTime = System.currentTimeMillis();
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "processing task '" + task.getId() + "': " + task.getMethod() + " " + task.getUrl());

		OctaneResultAbridged result = DTOFactory.getInstance().newDTO(OctaneResultAbridged.class);
		result.setId(task.getId());
		result.setStatus(HttpStatus.SC_OK);
		result.setHeaders(new HashMap<>());
		result.setServiceId(configurer.octaneConfiguration.getInstanceId());
		String[] path = pathTokenizer(task.getUrl());
		try {
			if(task.getHeaders() != null && !task.getHeaders().isEmpty()) {
				logger.info(configurer.octaneConfiguration.getLocationForLog() + "headers are not empty! passing to plugin");
				configurer.pluginServices.setCorrelationId(task.getHeaders());
			}
			if (path.length == 1 && STATUS.equals(path[0])) {
				executeStatusRequest(result);
			} else if (path.length == 1 && SUSPEND_STATUS.equals(path[0])) {
				suspendCiEvents(result, task.getBody());
			} else if (path[0].startsWith(JOBS)) {
				if (path.length == 1) {
					Map<String, String> queryParams = getQueryParamsMap(path[0]);

					boolean includingParameters = !"false".equals(queryParams.get("parameters"));
					Long workspaceId = queryParams.containsKey("workspaceId") ? Long.parseLong(queryParams.get("workspaceId")) : null;
					executeJobsListRequest(result, includingParameters, workspaceId);
				} else if (path.length == 2) {
					executePipelineRequest(result, path[1]);
				} else if (path.length == 3 && RUN.equals(path[2])) {
					executePipelineRunExecuteRequest(result, path[1], task.getBody());
				} else if (path.length == 3 && STOP.equals(path[2])) {
					executePipelineRunStopRequest(result, path[1], task.getBody());
				} else {
					result.setStatus(HttpStatus.SC_NOT_FOUND);
				}
			} else if (BUILD_STATUS.equalsIgnoreCase(path[0])) {
				executeGetBulkBuildStatusRequest(result, task.getBody());
			} else if (path.length == 2 && path[0].startsWith(BRANCHES)) {
				Map<String, String> queryParams = getQueryParamsMap(path[1]);

				String jobCiId = path[1].substring(0, path[1].indexOf("?"));
				String filterBranchName = queryParams.getOrDefault("filterBranchName", null);

				executeBranchesListRequest(result, jobCiId, filterBranchName);
			} else if (EXECUTOR.equalsIgnoreCase(path[0])) {
				if (HttpMethod.POST.equals(task.getMethod()) && path.length == 2) {
					if (INIT.equalsIgnoreCase(path[1])) {
						DiscoveryInfo discoveryInfo = dtoFactory.dtoFromJson(task.getBody(), DiscoveryInfo.class);
						discoveryInfo.setConfigurationId(configurer.octaneConfiguration.getInstanceId());
						configurer.pluginServices.runTestDiscovery(discoveryInfo);
						PipelineNode node = configurer.pluginServices.createExecutor(discoveryInfo);
						if (node != null) {
							result.setBody(dtoFactory.dtoToJson(node));
							result.getHeaders().put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
						}
						result.setStatus(HttpStatus.SC_OK);
					} else if (UPDATE.equalsIgnoreCase(path[1])) {
						DiscoveryInfo discoveryInfo = dtoFactory.dtoFromJson(task.getBody(), DiscoveryInfo.class);
						discoveryInfo.setConfigurationId(configurer.octaneConfiguration.getInstanceId());
						configurer.pluginServices.updateExecutor(discoveryInfo);
						result.setStatus(HttpStatus.SC_OK);
					} else if (TEST_CONN.equalsIgnoreCase(path[1])) {
						TestConnectivityInfo testConnectivityInfo = dtoFactory.dtoFromJson(task.getBody(), TestConnectivityInfo.class);
						OctaneResponse connTestResult = configurer.pluginServices.checkRepositoryConnectivity(testConnectivityInfo);
						result.setStatus(connTestResult.getStatus());
						result.setBody(connTestResult.getBody());
					} else if (CREDENTIALS_UPSERT.equalsIgnoreCase(path[1])) {
						CredentialsInfo credentialsInfo = dtoFactory.dtoFromJson(task.getBody(), CredentialsInfo.class);
						executeUpsertCredentials(result, credentialsInfo);
					} else if (SYNC_NOW.equalsIgnoreCase(path[1])) {
						DiscoveryInfo discoveryInfo = dtoFactory.dtoFromJson(task.getBody(), DiscoveryInfo.class);
						discoveryInfo.setConfigurationId(configurer.octaneConfiguration.getInstanceId());
						configurer.pluginServices.syncNow(discoveryInfo);
					} else {
						result.setStatus(HttpStatus.SC_NOT_FOUND);
					}
				} else if (HttpMethod.DELETE.equals(task.getMethod()) && path.length == 2) {
					String id = path[1];
					configurer.pluginServices.deleteExecutor(id);
				} else if (HttpMethod.GET.equals(task.getMethod()) && path.length == 2) {
					if (CREDENTIALS.equalsIgnoreCase(path[1])) {
						List<CredentialsInfo> credentials = configurer.pluginServices.getCredentials();
						String json = dtoFactory.dtoCollectionToJson(credentials);
						result.setBody(json);
					}
				}
			} else {
				result.setStatus(HttpStatus.SC_NOT_FOUND);
			}
		} catch (ErrorCodeBasedException pe) {
			logger.warn(configurer.octaneConfiguration.getLocationForLog() + "task execution failed; error: " + pe.getErrorCode() +", message : " + pe.getMessage());
			result.setStatus(pe.getErrorCode());
			result.setBody(String.valueOf(pe.getErrorCode()));
		} catch (SPIMethodNotImplementedException spimnie) {
			result.setStatus(HttpStatus.SC_NOT_IMPLEMENTED);
		} catch (Throwable e) {
			logger.error(configurer.octaneConfiguration.getLocationForLog() + "task execution failed", e);
			result.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);

			TaskProcessingErrorBody errorBody = dtoFactory.newDTO(TaskProcessingErrorBody.class)
					.setErrorMessage("Task " + task.getUrl() + " is failed. Server error message: " + e.getMessage());
			result.setBody(dtoFactory.dtoToJson(errorBody));
			result.getHeaders().put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
			logger.warn(configurer.octaneConfiguration.getLocationForLog() + "OctaneResultAbridged.execute failed : " + e.getMessage());
		}

		logger.info(configurer.octaneConfiguration.getLocationForLog() + "result for task '" + task.getId() + "' available with status " + result.getStatus() +", processing time is " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
		return result;
	}

	private void executeBranchesListRequest(OctaneResultAbridged result, String jobCiId, String filterBranchName) {
		result.getHeaders().put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

		CIBranchesList content = configurer.pluginServices.getBranchesList(jobCiId, filterBranchName);
		result.setBody(dtoFactory.dtoToJson(content));
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "executeBranchesListRequest: found " +
				content.getBranches().size() + " branches, body size is " + result.getBody().length());
	}

	@Override
	public Future<Boolean> resetJobListCache() {
		if (ConfigurationParameterFactory.jobListCacheAllowed(configurer.octaneConfiguration) && !configurer.octaneConfiguration.isDisabled()) {
			return jobListCacheExecutor.submit(() -> {
				logger.info(configurer.octaneConfiguration.getLocationForLog() + "resetJobListCache submitted");
				try {
					long startTime = System.currentTimeMillis();
					CIJobsList content = configurer.pluginServices.getJobsList(true, null);
					if (content != null) {
						jobListCacheItem = CacheItem.create(content);
						logger.info(configurer.octaneConfiguration.getLocationForLog() + "resetJobListCache: cache is reset, found " + content.getJobs().length + " jobs, processing time is " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
						return true;
					} else {
						logger.info(configurer.octaneConfiguration.getLocationForLog() + "resetJobListCache: failed to update cache. Content is empty.");
						return false;
					}
				} catch (Exception e) {
					logger.info(configurer.octaneConfiguration.getLocationForLog() + "Failed to resetJobListCache : " + e.getMessage());
					return false;
				}
			});
		} else {
			if (jobListCacheItem != null) {
				logger.info(configurer.octaneConfiguration.getLocationForLog() + "resetJobListCache : cache is cleared");
			}
			jobListCacheItem = null;
			return CompletableFuture.completedFuture(false);
		}
	}

	private String[] pathTokenizer(String url) {
		Map<Integer, String> params = new HashMap<>();
		String[] path = Pattern.compile("^.*" + NGA_API + "/?").matcher(url).replaceFirst("").split("/");
		params.put(0, path[0]);
		for (int i = 1; i < path.length; i++) {
			if ((path[i].equals(BUILDS) || path[i].equals(RUN) || path[i].equals(STOP)) && i == path.length - 1) { // last token
				params.put(2, path[i]);
			} else if (path[i].equals(BUILDS) && i == path.length - 2) {        // one before last token
				params.put(2, path[i]);
				params.put(3, path[i + 1]);
				break;
			} else {
				if (params.get(1) == null) {
					params.put(1, path[i]);
				} else {
					params.put(1, params.get(1) + "/" + path[i]);
				}
			}
		}
		// converting to an array
		List<String> listAsArray = new ArrayList<>();
		for (int i = 0; i < params.size(); i++) {
			listAsArray.add(i, params.get(i));
		}
		return listAsArray.toArray(new String[0]);
	}

	private void executeStatusRequest(OctaneResultAbridged result) {
		CIPluginSDKInfo sdkInfo = dtoFactory.newDTO(CIPluginSDKInfo.class)
				.setApiVersion(OctaneSDK.API_VERSION)
				.setSdkVersion(OctaneSDK.SDK_VERSION);
		CIServerInfo serverInfo = configurer.pluginServices.getServerInfo();
		serverInfo.setInstanceId(configurer.octaneConfiguration.getInstanceId());
		CIProviderSummaryInfo status = dtoFactory.newDTO(CIProviderSummaryInfo.class)
				.setServer(serverInfo)
				.setPlugin(configurer.pluginServices.getPluginInfo())
				.setSdk(sdkInfo);
		result.setBody(dtoFactory.dtoToJson(status));
		result.getHeaders().put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
	}

	private void executeJobsListRequest(OctaneResultAbridged result, boolean includingParameters, Long workspaceId) {
		result.getHeaders().put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

		//try get from cache
		boolean cacheAllowed = ConfigurationParameterFactory.jobListCacheAllowed(configurer.octaneConfiguration);
		boolean cacheIsUsed = false;
		if (cacheAllowed) {

			CacheItem myJobListCacheItem = jobListCacheItem;//save instance because jobListCacheItem might be cleaned
			if (myJobListCacheItem != null) {
				long currentTime = System.currentTimeMillis();
				long hours = (currentTime - myJobListCacheItem.time) / (1000 * 60 * 5);
				if (hours >= 1) {//if exceed hour, refresh the cache data
					try {//give upto 10 sec to try to refresh, if not - old item will be used
						if(resetJobListCache().get(10, TimeUnit.SECONDS)){
							myJobListCacheItem = jobListCacheItem;//update myJobListCacheItem
						}
					} catch (InterruptedException|ExecutionException|TimeoutException e) {
						//do nothing, use previous cache data
					}
				}
				CIJobsList content = myJobListCacheItem.value;
				result.setBody(dtoFactory.dtoToJson(content));
				logger.info(configurer.octaneConfiguration.getLocationForLog() + "executeJobsListRequest: cache is used, found " +
						content.getJobs().length + " jobs, body size is " + result.getBody().length());
				cacheIsUsed = true;
			}
		}

		if (!cacheIsUsed) {
			Long myWorkspaceId = cacheAllowed ? null : workspaceId;//workspaceId is not support for cache
			Boolean myIncludingParameters = cacheAllowed ? true : includingParameters;//myIncludingParameters should be true is cache is allowed
			logger.info("Starting to get jobs without cache");
			long startGetJobList = System.currentTimeMillis();
			CIJobsList content = configurer.pluginServices.getJobsList(myIncludingParameters, myWorkspaceId);
			logger.info("Finish get job content without cache took {} ms",System.currentTimeMillis() -startGetJobList);
			if (content != null) {
				result.setBody(dtoFactory.dtoToJson(content));
				if (cacheAllowed) {
					jobListCacheItem = CacheItem.create(content);
				}
				logger.info(configurer.octaneConfiguration.getLocationForLog() + "executeJobsListRequest: found " +
						content.getJobs().length + " jobs, body size is " + result.getBody().length());
			} else {
				TaskProcessingErrorBody errorMessage = dtoFactory.newDTO(TaskProcessingErrorBody.class)
						.setErrorMessage("'getJobsList' API is not implemented OR returns NULL, which contradicts API requirement (MAY be empty list)");
				result.setBody(dtoFactory.dtoToJson(errorMessage));
				result.setStatus(HttpStatus.SC_NOT_IMPLEMENTED);
			}
		}
	}

	/**
	 * this method is called by octane during adding new pipeline
	 * @param result
	 * @param jobId
	 */
	private void executePipelineRequest(OctaneResultAbridged result, String jobId) {
		PipelineNode content = configurer.pluginServices.getPipeline(jobId);
		if (content != null) {
			result.setBody(dtoFactory.dtoToJson(content));
			result.getHeaders().put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
			configurationService.addToOctaneRootsCache(jobId);//update cache that new pipeline root is added
		} else {
			result.setStatus(HttpStatus.SC_NOT_FOUND);
		}
	}

	private void executePipelineRunExecuteRequest(OctaneResultAbridged result, String jobId, String originalBody) {
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "RunExecute job " + jobId);
		configurationService.addToOctaneRootsCache(jobId);//test runner started from here, so it will be added to cache
		CIParameters ciParameters = originalBody != null ? DTOFactory.getInstance().dtoFromJson(originalBody, CIParameters.class) : null;
		if (ciParameters == null) {
			ciParameters = dtoFactory.newDTO(CIParameters.class);
			ciParameters.setParameters(new ArrayList<>());
		}
		CIParameter ciParameter = dtoFactory.newDTO(CIParameter.class);
		ciParameter.setName(SdkConstants.JobParameters.OCTANE_CONFIG_ID_PARAMETER_NAME)
				.setValue(configurer.octaneConfiguration.getInstanceId());
		ciParameters.getParameters().add(ciParameter);

		CIParameter octaneUrlParameter = dtoFactory.newDTO(CIParameter.class);
		octaneUrlParameter.setName(SdkConstants.JobParameters.OCTANE_URL_PARAMETER_NAME)
				.setValue(configurer.octaneConfiguration.getUrl());
		ciParameters.getParameters().add(octaneUrlParameter);

		configurer.pluginServices.runPipeline(jobId, ciParameters);
		result.setStatus(HttpStatus.SC_CREATED);
	}

	private void executePipelineRunStopRequest(OctaneResultAbridged result, String jobId, String originalBody) {
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "RunStop job " + jobId);
		CIParameters ciParameters = originalBody != null ? DTOFactory.getInstance().dtoFromJson(originalBody, CIParameters.class) : null;
		configurer.pluginServices.stopPipelineRun(jobId, ciParameters);
		result.setStatus(HttpStatus.SC_OK);
	}

	private void executeGetBulkBuildStatusRequest(OctaneResultAbridged result, String originalBody) {
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "BulkBuildStatus ");
		CIBuildStatusInfo[] statuses = originalBody != null ? DTOFactory.getInstance().dtoCollectionFromJson(originalBody, CIBuildStatusInfo[].class) : new CIBuildStatusInfo[0];
		List<CIBuildStatusInfo> output = new ArrayList<>();
		for (CIBuildStatusInfo statusInfo : statuses) {
			try {
				CIBuildStatusInfo myStatus = configurer.pluginServices.getJobBuildStatus(statusInfo.getJobCiId(), statusInfo.getParamName(), statusInfo.getParamValue());
				output.add(myStatus);
			} catch (SPIMethodNotImplementedException notImplemented) {
				result.setStatus(HttpStatus.SC_NOT_IMPLEMENTED);
				return;
			} catch (ErrorCodeBasedException ex) {
				statusInfo.setExceptionMessage(ex.getMessage());
				statusInfo.setExceptionCode(ex.getErrorCode());
				output.add(statusInfo);
			} catch (Exception e) {
				statusInfo.setExceptionMessage(e.getMessage());
				statusInfo.setExceptionCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				output.add(statusInfo);
			}
		}

		result.setBody(dtoFactory.dtoCollectionToJson(output));
		result.setStatus(HttpStatus.SC_OK);
	}

	private Map<String, String> getQueryParamsMap(String path) {
		Map<String, String> queryParams = new HashMap<>();
		String queryParamsStr = path.contains("?") ? path.substring(path.indexOf("?") + 1) : path;
		String[] queryParamsParts = queryParamsStr.split("&");
		for (String p : queryParamsParts) {
			String[] parts = p.split("=");
			if (parts.length == 2) {
				queryParams.put(parts[0], parts[1]);
			}
		}
		return queryParams;
	}

	private void suspendCiEvents(OctaneResultAbridged result, String suspend) {
		boolean toSuspend = Boolean.parseBoolean(suspend);
		configurer.pluginServices.suspendCIEvents(toSuspend);
		result.setStatus(HttpStatus.SC_CREATED);
	}

	private void executeUpsertCredentials(OctaneResultAbridged result, CredentialsInfo credentialsInfo) {
		OctaneResponse response = configurer.pluginServices.upsertCredentials(credentialsInfo);
		result.setBody(response.getBody());
		result.setStatus(response.getStatus());
	}

	@Override
	public void shutdown() {
		jobListCacheExecutor.shutdown();
	}

	@Override
	public boolean isShutdown() {
		return jobListCacheExecutor.isShutdown();
	}

	@Override
	public Map<String, Object> getMetrics() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("jobListCacheAllowed", ConfigurationParameterFactory.jobListCacheAllowed(configurer.octaneConfiguration));
		if(jobListCacheItem != null){
			map.put("jobListCache_jobCount", jobListCacheItem.value.getJobs().length);
			map.put("jobListCache_time",  new Date(jobListCacheItem.time));
		}
		return map;
	}

	private static class CacheItem {
		long time;
		CIJobsList value;

		public static CacheItem create(CIJobsList value){
			CacheItem ci = new CacheItem();
			ci.value = value;
			ci.time = System.currentTimeMillis();
			return ci;
		}
	}

}
