/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.WorkerPreflight;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.configuration.ConfigurationServiceImpl;
import com.hp.octane.integrations.services.configurationparameters.FortifySSCFetchTimeoutParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FodConnectionFactory;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of vulnerabilities service
 * com.hp.mqm.analytics.devops.insights.resources.DevopsInsightsSSAPublicApiResource#isVulnerabilitiesRelevant
 * com.hp.mqm.analytics.devops.insights.resources.DevopsInsightsSSAPublicApiResource#upsertVulnerabilities
 */

public class VulnerabilitiesServiceImpl implements VulnerabilitiesService {
	private static final Logger logger = LogManager.getLogger(VulnerabilitiesServiceImpl.class);
	private static final String VULNERABILITIES_QUEUE_FILE = "vulnerabilities-queue.dat";

	private final ExecutorService vulnerabilitiesProcessingExecutor = Executors.newSingleThreadExecutor(new VulnerabilitiesPushWorkerThreadFactory());
	private final ObjectQueue<VulnerabilitiesQueueItem> vulnerabilitiesQueue;
	protected final RestService restService;
	protected final ConfigurationService configurationService;
	protected final OctaneSDK.SDKServicesConfigurer configurer;
	private final Map<String, VulnerabilitiesToolService> vulnerabilitiesServices;
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 15000;

	private int SKIP_QUEUE_ITEM_INTERVAL = 5000;
	private Long DEFAULT_TIMEOUT_FOR_QUEUE_ITEM = FortifySSCFetchTimeoutParameter.DEFAULT_TIMEOUT * 60 * 60 * 1000L;
	private CompletableFuture<Boolean> workerExited;
	private final WorkerPreflight workerPreflight;

	public VulnerabilitiesServiceImpl(QueueingService queueingService, VulnerabilitiesToolService[] vulnerabilitiesToolServices,
										  OctaneSDK.SDKServicesConfigurer configurer, RestService restService, ConfigurationService configurationService) {

		if (queueingService == null) {
			throw new IllegalArgumentException("queue Service MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}
		if (configurer == null) {
			throw new IllegalArgumentException("configurer MUST NOT be null");
		}
		if (configurationService == null) {
			throw new IllegalArgumentException("configuration service MUST NOT be null");
		}


		this.restService = restService;
		this.configurationService = configurationService;
		this.configurer = configurer;
		this.workerPreflight = new WorkerPreflight(this, configurationService, logger);
		FodConnectionFactory.setConfigurer(this.configurer);

		vulnerabilitiesServices = new HashMap<>();
		for (VulnerabilitiesToolService vulnerabilitiesToolService : vulnerabilitiesToolServices) {
			vulnerabilitiesServices.put(vulnerabilitiesToolService.getVulnerabilitiesToolKey(), vulnerabilitiesToolService);
		}

		if (queueingService.isPersistenceEnabled()) {
			vulnerabilitiesQueue = queueingService.initFileQueue(VULNERABILITIES_QUEUE_FILE, VulnerabilitiesQueueItem.class);
		} else {
			vulnerabilitiesQueue = queueingService.initMemoQueue();
		}

		logger.info(configurer.octaneConfiguration.getLocationForLog() + "starting background worker...");
		vulnerabilitiesProcessingExecutor.execute(this::worker);
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "initialized SUCCESSFULLY (backed by " + vulnerabilitiesQueue.getClass().getSimpleName() + ")");
	}

	@Override
	public void addVulnerabilitiesToolService(VulnerabilitiesToolService vulnerabilitiesToolService) {
		this.vulnerabilitiesServices.put(vulnerabilitiesToolService.getVulnerabilitiesToolKey(), vulnerabilitiesToolService);
	}

	@Override
	public void enqueueRetrieveAndPushVulnerabilities(String jobId, String buildId, ToolType toolType, long startRunTime, long queueItemTimeout, Map<String, String> additionalProperties, String rootJobId) {
		this.enqueueRetrieveAndPushVulnerabilities(jobId, buildId, toolType.name(), startRunTime, queueItemTimeout, additionalProperties, rootJobId);
	}

	@Override
	public void enqueueRetrieveAndPushVulnerabilities(String jobId,
													  String buildId,
													  String toolType,
													  long startRunTime,
													  long queueItemTimeout,
													  Map<String, String> additionalProperties,
													  String rootJobId) {
		if (this.configurer.octaneConfiguration.isDisabled()) {
			return;
		}
		if (!((ConfigurationServiceImpl) configurationService).isRelevantForOctane(rootJobId)) {
			return;
		}

		VulnerabilitiesQueueItem vulnerabilitiesQueueItem = new VulnerabilitiesQueueItem(jobId, buildId);
		vulnerabilitiesQueueItem.setStartTime(startRunTime);
		vulnerabilitiesQueueItem.setTimeout(queueItemTimeout <= 0 ? DEFAULT_TIMEOUT_FOR_QUEUE_ITEM : queueItemTimeout * 60 * 60 * 1000);
		vulnerabilitiesQueueItem.setToolType(toolType);
		vulnerabilitiesQueueItem.setAdditionalProperties(additionalProperties);
		vulnerabilitiesQueue.add(vulnerabilitiesQueueItem);
		logger.info(configurer.octaneConfiguration.getLocationForLog() + vulnerabilitiesQueueItem.getJobId() + ":" + vulnerabilitiesQueueItem.getBuildId() + " was added to vulnerabilities queue, currently : "+vulnerabilitiesQueue.size()+" items in queue");

		workerPreflight.itemAddedToQueue();
	}

	@Override
	public void shutdown() {
		workerExited = new CompletableFuture<>();
		vulnerabilitiesProcessingExecutor.shutdown();
	}

	@Override
	public boolean isShutdown() {
		return vulnerabilitiesProcessingExecutor.isShutdown();
	}

	//  TODO: implement retries counter per item and strategy of discard
	//  TODO: consider moving the overall queue managing logic to some generic location
	//  infallible everlasting background worker
	private void worker() {
		while (!vulnerabilitiesProcessingExecutor.isShutdown()) {
			if(!workerPreflight.preflight()){
				continue;
			}

			VulnerabilitiesQueueItem queueItem = null;
			try {
				queueItem = vulnerabilitiesQueue.peek();
				if (processPushVulnerabilitiesQueueItem(queueItem)) {
					vulnerabilitiesQueueItemCleanUp(queueItem);
					vulnerabilitiesQueue.remove();
				} else {
					reEnqueueItem(queueItem);
				}
			} catch (TemporaryException tque) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "temporary error on " + queueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
				if (queueItem != null) {
					reEnqueueItem(queueItem);
				}
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pqie) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "permanent error on " + queueItem + ", passing over", pqie);
				vulnerabilitiesQueueItemCleanUp(queueItem);
				vulnerabilitiesQueue.remove();
			} catch (Throwable t) {
				logger.error(configurer.octaneConfiguration.getLocationForLog() + "unexpected error on build log item '" + queueItem + "', passing over", t);
				vulnerabilitiesQueueItemCleanUp(queueItem);
				vulnerabilitiesQueue.remove();
			}
		}
		workerExited.complete(true);
	}


	private boolean processPushVulnerabilitiesQueueItem(VulnerabilitiesQueueItem queueItem) {

		try {
			//  if this is the first time in the queue , check if vulnerabilities relevant to octane, and if not remove it from the queue.
			if (!queueItem.isRelevant()) {

				Date relevant =  vulnerabilitiesPreflightRequest(queueItem.getJobId(), queueItem.getBuildId());
				if (relevant != null) {
					logger.debug(configurer.octaneConfiguration.getLocationForLog() + queueItem.toString() + " , Relevant:" + relevant);
					//  set queue item value relevancy to true and continue
					queueItem.setRelevant(true);
					//for backward compatibility with Octane - if baselineDate is 2000-01-01 it means that we didn't get it from octane and we need to discard it
					if (relevant.compareTo(DateUtils.getDateFromUTCString("2000-01-01", "yyyy-MM-dd")) > 0) {
						queueItem.setBaselineDate(relevant);
					}
				} else {
					//  return with true to silently proceed to the next item
					return true;
				}
			}

			InputStream vulnerabilitiesStream = getToolService(queueItem).getVulnerabilitiesScanResultStream(queueItem);

			if (vulnerabilitiesStream == null) {
				return false;
			} else {
				pushVulnerabilities(vulnerabilitiesStream, queueItem.getJobId(), queueItem.getBuildId());
				return true;
			}
		} catch (IOException e) {
			throw new PermanentException(e);
		}
	}

	private VulnerabilitiesToolService getToolService(VulnerabilitiesQueueItem queueItem) {
		return Optional.ofNullable(vulnerabilitiesServices.get(queueItem.getToolType()))
				.orElseThrow(() -> new OctaneSDKGeneralException("Vulnerability tool service with name \"" + queueItem.getToolType() + "\" is not registered."));
	}

	private boolean isEncodeBase64() {
		return ConfigurationParameterFactory.isEncodeCiJobBase64(configurer.octaneConfiguration);
	}

	private void pushVulnerabilities(InputStream vulnerabilities, String jobId, String buildId) throws IOException {
		OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());

		boolean base64 = isEncodeBase64();
		String encodedJobId = base64 ? CIPluginSDKUtils.urlEncodeBase64(jobId) : CIPluginSDKUtils.urlEncodePathParam(jobId);
		String encodedBuildId = CIPluginSDKUtils.urlEncodePathParam(buildId);

		String url = getVulnerabilitiesContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
				"?instance-id=" + configurer.octaneConfiguration.getInstanceId() + "&job-ci-id=" + encodedJobId + "&build-ci-id=" + encodedBuildId;

		if (base64) {
			url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
		}

		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.POST)
				.setUrl(url)
				.setHeaders(headers)
				.setBody(vulnerabilities);

		OctaneResponse response = octaneRestClient.execute(request);
		logger.info(configurer.octaneConfiguration.getLocationForLog() + "vulnerabilities pushed; status: " + response.getStatus() + ", response: " + response.getBody());
		if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
			logger.info(configurer.octaneConfiguration.getLocationForLog() + "vulnerabilities push SUCCEED for " + jobId + " #" + buildId);
		} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
			throw new TemporaryException("vulnerabilities push FAILED, service unavailable");
		} else {
			throw new PermanentException("vulnerabilities push FAILED, status " + response.getStatus() + "; dropping this item from the queue \n" + response.getBody());
		}
	}

	private Date vulnerabilitiesPreflightRequest(String jobId, String buildId) throws IOException {

		OctaneResponse response = getBaselineDateFromOctane(jobId, buildId);

		if (response.getStatus() == HttpStatus.SC_OK) {
			if (response.getBody()==null || "".equals(response.getBody())) {
				logger.info(configurer.octaneConfiguration.getLocationForLog() + "vulnerabilities data of " + jobId + " #" + buildId + " is not relevant to Octane");
				return null;
			}else{
				logger.info(configurer.octaneConfiguration.getLocationForLog() + "vulnerabilities data of " + jobId + " #" + buildId + " found to be relevant to Octane");
				boolean forTest = false;
				//backward compatibility with Octane
				if("true".equals(response.getBody()) || forTest){
					return DateUtils.getDateFromUTCString("2000-01-01", "yyyy-MM-dd");
				}
				return DateUtils.getDateFromUTCString(response.getBody(), DateUtils.octaneFormat);
			}
		}
		if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
			throw new TemporaryException("vulnerabilities preflight request FAILED, service unavailable");
		} else {
			throw new PermanentException("vulnerabilities preflight request FAILED with " + response.getStatus() + "");
		}
	}


	private void reEnqueueItem(VulnerabilitiesQueueItem vulnerabilitiesQueueItem) {
		Long timePass = System.currentTimeMillis() - vulnerabilitiesQueueItem.getStartTime();
		vulnerabilitiesQueue.remove();
		if (timePass < vulnerabilitiesQueueItem.getTimeout()) {
			vulnerabilitiesQueue.add(vulnerabilitiesQueueItem);
		} else {
			logger.info(configurer.octaneConfiguration.getLocationForLog() + vulnerabilitiesQueueItem.getBuildId() + "/" + vulnerabilitiesQueueItem.getJobId() + " was removed from queue after timeout in queue is over");
		}
		CIPluginSDKUtils.doWait(SKIP_QUEUE_ITEM_INTERVAL);
	}

	private OctaneResponse getBaselineDateFromOctane(String jobId, String buildId) throws IOException {
		boolean base64 = isEncodeBase64();
		String encodedJobId = base64 ? CIPluginSDKUtils.urlEncodeBase64(jobId) : CIPluginSDKUtils.urlEncodeQueryParam(jobId);
		String encodedBuildId = CIPluginSDKUtils.urlEncodeQueryParam(buildId);
		String url = getVulnerabilitiesPreFlightContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
				"?instance-id=" + configurer.octaneConfiguration.getInstanceId() + "&job-ci-id=" + encodedJobId + "&build-ci-id=" + encodedBuildId;
		if (base64) {
			url = CIPluginSDKUtils.addParameterEncode64ToUrl(url);
		}

		OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class).setMethod(HttpMethod.GET).setUrl(url);
		return restService.obtainOctaneRestClient().execute(preflightRequest);
	}

	private boolean vulnerabilitiesQueueItemCleanUp(VulnerabilitiesQueueItem queueItem){
		if (queueItem == null) {
			return true;
		}
		return getToolService(queueItem).vulnerabilitiesQueueItemCleanUp(queueItem);
	}

	@Override
	public long getQueueSize() {
		return vulnerabilitiesQueue.size();
	}

	@Override
	public void clearQueue() {
		while (vulnerabilitiesQueue.size() > 0) {
			vulnerabilitiesQueue.remove();
		}
	}

	@Override
	public Map<String, Object> getMetrics() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("queueSize", this.getQueueSize());
		workerPreflight.addMetrics(map);
		return map;
	}

	private static final class VulnerabilitiesPushWorkerThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("VulnerabilitiesPushWorker-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}

	private String getVulnerabilitiesContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.VULNERABILITIES;
	}

	private String getVulnerabilitiesPreFlightContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.VULNERABILITIES_PRE_FLIGHT;
	}


}
