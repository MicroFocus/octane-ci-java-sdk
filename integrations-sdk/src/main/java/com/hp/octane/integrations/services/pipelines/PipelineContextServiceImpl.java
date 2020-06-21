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

package com.hp.octane.integrations.services.pipelines;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.entities.OctaneBulkExceptionData;
import com.hp.octane.integrations.dto.entities.OctaneRestExceptionData;
import com.hp.octane.integrations.dto.pipelines.PipelineContext;
import com.hp.octane.integrations.dto.pipelines.PipelineContextList;
import com.hp.octane.integrations.exceptions.OctaneBulkException;
import com.hp.octane.integrations.exceptions.OctaneRestException;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.hp.octane.integrations.services.rest.RestService.ACCEPT_HEADER;
import static com.hp.octane.integrations.services.rest.RestService.CONTENT_TYPE_HEADER;

/**
 * Default implementation of tests service
 */

final class PipelineContextServiceImpl implements PipelineContextService {
	private static final Logger logger = LogManager.getLogger(PipelineContextServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;


	PipelineContextServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}
		this.configurer = configurer;
		this.restService = restService;
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "initialized SUCCESSFULLY");
	}

	private String getSharedspaceAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + RestService.ANALYTICS_CI_PATH_PART;
	}

	private String getWorkspaceAnalyticsContextPath(String octaneBaseUrl, String sharedSpaceId, long workspaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_INTERNAL_API_PATH_PART + sharedSpaceId + "/workspaces/" + workspaceId + RestService.ANALYTICS_CI_PATH_PART;
	}

	private String getConfigurationUrl(String serverIdentity, String jobName) {
		boolean base64 = isEncodeBase64();
		String jobNameEncoded = base64 ? CIPluginSDKUtils.urlEncodeBase64(jobName) : CIPluginSDKUtils.urlEncodePathParam(jobName);
		String url = getSharedspaceAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
				String.format("servers/%s/jobs/%s/configuration",
						CIPluginSDKUtils.urlEncodePathParam(serverIdentity),
						jobNameEncoded);
		if (base64) {
			url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
			logger.info("Using base64, " + url);
		}
		return url;
	}

	private boolean isEncodeBase64() {
		return ConfigurationParameterFactory.isEncodeCiJobBase64(configurer.octaneConfiguration);
	}

	@Override
	public PipelineContextList getJobConfiguration(String serverIdentity, String jobName) throws IOException {
		String url = getConfigurationUrl(serverIdentity, jobName);

		OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(ACCEPT_HEADER, ContentType.APPLICATION_JSON.getMimeType());

		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setUrl(url)
				.setHeaders(headers);
		OctaneResponse response = octaneRestClient.execute(request);
		validateResponse(HttpStatus.SC_OK, response);

		PipelineContextList result = dtoFactory.dtoFromJson(response.getBody(), PipelineContextList.class);
		return result;
	}


	@Override
	public PipelineContext updatePipeline(String serverIdentity, String jobName, PipelineContext pipelineContext) throws IOException {
		String url = getConfigurationUrl(serverIdentity, jobName);
		validateReleaseAndMilestone(pipelineContext);

		OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(ACCEPT_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

		PipelineContextList list = dtoFactory.newDTO(PipelineContextList.class).setData(Arrays.asList(pipelineContext));
		String jsonData = dtoFactory.dtoToJson(list);

		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.PUT)
				.setUrl(url)
				.setBody(jsonData)
				.setHeaders(headers);
		OctaneResponse response = octaneRestClient.execute(request);
		validateResponse(HttpStatus.SC_OK, response);

		PipelineContextList resultList = dtoFactory.dtoFromJson(response.getBody(), PipelineContextList.class);

		//we might receive several pipeline context from other workspaces.
		//find updated context by id
		PipelineContext result = resultList.getData().stream().filter(p -> p.getContextEntityId() == pipelineContext.getContextEntityId()).findFirst().get();
		return result;
	}

	@Override
	public PipelineContext createPipeline(String serverIdentity, String jobName, PipelineContext pipelineContext) throws IOException {

		String url = getConfigurationUrl(serverIdentity, jobName);

		validateReleaseAndMilestone(pipelineContext);

		OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(ACCEPT_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

		String jsonData = dtoFactory.dtoToJson(pipelineContext);

		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.POST)
				.setUrl(url)
				.setBody(jsonData)
				.setHeaders(headers);
		OctaneResponse response = octaneRestClient.execute(request);
		validateResponse(HttpStatus.SC_CREATED, response);
		PipelineContextList list = dtoFactory.dtoFromJson(response.getBody(), PipelineContextList.class);

		//we might receive several pipeline context from other workspaces.
		//find updated context by workspace id
		PipelineContext result = list.getData().stream().filter(p -> p.getWorkspaceId() == pipelineContext.getWorkspaceId()).findFirst().get();
		return result;
	}

	private void validateReleaseAndMilestone(PipelineContext pipelineContext) {
		if (pipelineContext.getReleaseId() != null && pipelineContext.getReleaseId() == -1L) {
			pipelineContext.setReleaseId(null);
		}

		if(pipelineContext.getMilestoneId() != null && pipelineContext.getMilestoneId() == -1L){
			pipelineContext.setMilestoneId(null);
		}
	}

	@Override
	public void deleteTestsFromPipelineNodes(String jobName, long pipelineId, long workspaceId) throws IOException {
		String url = getWorkspaceAnalyticsContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace(), workspaceId) +
				String.format("pipelines/%s/jobs/%s/tests",
						pipelineId,
						CIPluginSDKUtils.urlEncodePathParam(jobName));

		OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(ACCEPT_HEADER, ContentType.APPLICATION_JSON.getMimeType());

		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.DELETE)
				.setUrl(url)
				.setHeaders(headers);
		OctaneResponse response = octaneRestClient.execute(request);

		validateResponse(HttpStatus.SC_OK, response);
		//Object result = response.getBody();
		//return;
	}

	private static void validateResponse(int expectedResult, OctaneResponse response) {
		if (response.getStatus() != expectedResult) {
			try {
				String body = response.getBody();
				if (body.contains("exceeds_total_count")) {
					OctaneBulkExceptionData data = dtoFactory.dtoFromJson(body, OctaneBulkExceptionData.class);
					throw new OctaneBulkException(response.getStatus(), data);
				} else {
					OctaneRestExceptionData data = dtoFactory.dtoFromJson(body, OctaneRestExceptionData.class);
					throw new OctaneRestException(response.getStatus(), data);
				}
			} catch (OctaneRestException | OctaneBulkException ex1) {
				throw ex1;
			} catch (Exception ex2) {
				throw new RuntimeException("The request failed : " + response.getBody(), ex2);
			}
		}
	}

}
