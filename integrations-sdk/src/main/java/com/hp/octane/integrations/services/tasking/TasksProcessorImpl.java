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

package com.hp.octane.integrations.services.tasking;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.*;
import com.hp.octane.integrations.dto.executor.CredentialsInfo;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIPluginSDKInfo;
import com.hp.octane.integrations.dto.general.CIProviderSummaryInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.exceptions.ErrorCodeBasedException;
import com.hp.octane.integrations.exceptions.SPIMethodNotImplementedException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Tasks routing service handles ALM Octane tasks, both coming from abridged logic as well as plugin's REST call delegation
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
	private static final String BUILDS = "builds";
	private static final String LATEST = "latest";
	private static final String EXECUTOR = "executor";
	private static final String INIT = "init";
	private static final String SUITE_RUN = "suite_run";
	private static final String TEST_CONN = "test_conn";
	private static final String CREDENTIALS_UPSERT = "credentials_upsert";

	private final OctaneSDK.SDKServicesConfigurer configurer;

	TasksProcessorImpl(OctaneSDK.SDKServicesConfigurer configurer) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
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
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "processing task '" + task.getId() + "': " + task.getMethod() + " " + task.getUrl());

		OctaneResultAbridged result = DTOFactory.getInstance().newDTO(OctaneResultAbridged.class);
		result.setId(task.getId());
		result.setStatus(HttpStatus.SC_OK);
		result.setHeaders(new HashMap<>());
		result.setServiceId(configurer.octaneConfiguration.getInstanceId());
		String[] path = pathTokenizer(task.getUrl());
		try {
			if (path.length == 1 && STATUS.equals(path[0])) {
				executeStatusRequest(result);
			} else if (path.length == 1 && SUSPEND_STATUS.equals(path[0])) {
				suspendCiEvents(result, task.getBody());
			} else if (path[0].startsWith(JOBS)) {
				if (path.length == 1) {
					Map<String, String> queryParams = new HashMap<>();
					String queryParamsStr = path[0].contains("?") ? path[0].substring(path[0].indexOf("?") + 1) : path[0];
					String[] queryParamsParts = queryParamsStr.split("&");
					for (String p : queryParamsParts) {
						String[] parts = p.split("=");
						if (parts.length == 2) {
							queryParams.put(parts[0], parts[1]);
						}
					}
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
					} else if (TEST_CONN.equalsIgnoreCase(path[1])) {
						TestConnectivityInfo testConnectivityInfo = dtoFactory.dtoFromJson(task.getBody(), TestConnectivityInfo.class);
						OctaneResponse connTestResult = configurer.pluginServices.checkRepositoryConnectivity(testConnectivityInfo);
						result.setStatus(connTestResult.getStatus());
						result.setBody(connTestResult.getBody());
					} else if (CREDENTIALS_UPSERT.equalsIgnoreCase(path[1])) {
						CredentialsInfo credentialsInfo = dtoFactory.dtoFromJson(task.getBody(), CredentialsInfo.class);
						executeUpsertCredentials(result, credentialsInfo);
					} else {
						result.setStatus(HttpStatus.SC_NOT_FOUND);
					}
				} else if (HttpMethod.DELETE.equals(task.getMethod()) && path.length == 2) {
					String id = path[1];
					configurer.pluginServices.deleteExecutor(id);
				}
			} else {
				result.setStatus(HttpStatus.SC_NOT_FOUND);
			}
		} catch (ErrorCodeBasedException pe) {
			logger.warn(configurer.octaneConfiguration.geLocationForLog() + "task execution failed; error: " + pe.getErrorCode());
			result.setStatus(pe.getErrorCode());
			result.setBody(String.valueOf(pe.getErrorCode()));
		} catch (SPIMethodNotImplementedException spimnie) {
			result.setStatus(HttpStatus.SC_NOT_IMPLEMENTED);
		} catch (Throwable e) {
			logger.error(configurer.octaneConfiguration.geLocationForLog() + "task execution failed", e);
			result.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);

			TaskProcessingErrorBody errorBody = dtoFactory.newDTO(TaskProcessingErrorBody.class)
					.setErrorMessage("Task " + task.getUrl() + " is failed. Server error message: " + e.getMessage());
			result.setBody(dtoFactory.dtoToJson(errorBody));
			result.getHeaders().put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
			logger.warn(configurer.octaneConfiguration.geLocationForLog() + "OctaneResultAbridged.execute failed : " + e.getMessage());
		}

		logger.info(configurer.octaneConfiguration.geLocationForLog() + "result for task '" + task.getId() + "' available with status " + result.getStatus());
		return result;
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
		CIJobsList content = configurer.pluginServices.getJobsList(includingParameters, workspaceId);
		if (content != null) {
			result.setBody(dtoFactory.dtoToJson(content));
		} else {
			TaskProcessingErrorBody errorMessage = dtoFactory.newDTO(TaskProcessingErrorBody.class)
					.setErrorMessage("'getJobsList' API is not implemented OR returns NULL, which contradicts API requirement (MAY be empty list)");
			result.setBody(dtoFactory.dtoToJson(errorMessage));
			result.setStatus(HttpStatus.SC_NOT_IMPLEMENTED);
		}
		result.getHeaders().put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
	}

	private void executePipelineRequest(OctaneResultAbridged result, String jobId) {
		PipelineNode content = configurer.pluginServices.getPipeline(jobId);
		if (content != null) {
			result.setBody(dtoFactory.dtoToJson(content));
			result.getHeaders().put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
		} else {
			result.setStatus(HttpStatus.SC_NOT_FOUND);
		}
	}

	private void executePipelineRunExecuteRequest(OctaneResultAbridged result, String jobId, String originalBody) {
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "RunExecute job " + jobId);
		configurer.pluginServices.runPipeline(jobId, originalBody);
		result.setStatus(HttpStatus.SC_CREATED);
	}

	private void executePipelineRunStopRequest(OctaneResultAbridged result, String jobId, String originalBody) {
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "RunStop job " + jobId);
		configurer.pluginServices.stopPipelineRun(jobId, originalBody);
		result.setStatus(HttpStatus.SC_OK);
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
}
