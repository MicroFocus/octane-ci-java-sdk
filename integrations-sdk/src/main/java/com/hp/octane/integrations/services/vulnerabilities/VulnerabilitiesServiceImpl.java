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

package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.rest.OctaneRestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of vulnerabilities service
 */

final class VulnerabilitiesServiceImpl implements VulnerabilitiesService {
	private static final Logger logger = LogManager.getLogger(VulnerabilitiesServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String VULNERABILITIES_QUEUE_FILE = "vulnerabilities-queue.dat";

	private final ExecutorService vulnerabilitiesProcessingExecutor = Executors.newSingleThreadExecutor(new VulnerabilitiesPushWorkerThreadFactory());
	private final Object NO_VULNERABILITIES_RESULTS_MONITOR = new Object();
	private final ObjectQueue<VulnerabilitiesQueueItem> vulnerabilitiesQueue;
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;

	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 10000;
	private int LIST_EMPTY_INTERVAL = 10000;
	private int SKIP_QUEUE_ITEM_INTERVAL = 5000;
	private Long DEFAULT_TIME_OUT_FOR_QUEUE_ITEM = 12 * 60 * 60 * 1000L;

	VulnerabilitiesServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, QueueingService queueingService, RestService restService) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (queueingService == null) {
			throw new IllegalArgumentException("queue Service MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		if (queueingService.isPersistenceEnabled()) {
			vulnerabilitiesQueue = queueingService.initFileQueue(VULNERABILITIES_QUEUE_FILE, VulnerabilitiesQueueItem.class);
		} else {
			vulnerabilitiesQueue = queueingService.initMemoQueue();
		}

		this.configurer = configurer;
		this.restService = restService;

		logger.info("starting background worker...");
		vulnerabilitiesProcessingExecutor.execute(this::worker);
		logger.info("initialized SUCCESSFULLY (backed by " + vulnerabilitiesQueue.getClass().getSimpleName() + ")");
	}

	@Override
	public void enqueueRetrieveAndPushVulnerabilities(String jobId,
	                                                  String buildId,
	                                                  long startRunTime,
	                                                  long queueItemTimeout) {
		VulnerabilitiesQueueItem vulnerabilitiesQueueItem = new VulnerabilitiesQueueItem(jobId, buildId);
		vulnerabilitiesQueueItem.startTime = startRunTime;
		vulnerabilitiesQueueItem.timeout = queueItemTimeout <= 0 ? DEFAULT_TIME_OUT_FOR_QUEUE_ITEM : queueItemTimeout * 60 * 60 * 1000;
		vulnerabilitiesQueue.add(vulnerabilitiesQueueItem);
		logger.info(vulnerabilitiesQueueItem.buildId + "/" + vulnerabilitiesQueueItem.jobId + " was added to vulnerabilities queue");

		synchronized (NO_VULNERABILITIES_RESULTS_MONITOR) {
			NO_VULNERABILITIES_RESULTS_MONITOR.notify();
		}
	}

	@Override
	public void shutdown() {
		vulnerabilitiesProcessingExecutor.shutdown();
	}

	//  TODO: implement retries counter per item and strategy of discard
	//  TODO: distinct between the item's problem, server problem and env problem and retry strategy accordingly
	//  TODO: consider moving the overall queue managing logic to some generic location
	//  infallible everlasting background worker
	private void worker() {
		while (!vulnerabilitiesProcessingExecutor.isShutdown()) {
			if (vulnerabilitiesQueue.size() == 0) {
				CIPluginSDKUtils.doBreakableWait(LIST_EMPTY_INTERVAL, NO_VULNERABILITIES_RESULTS_MONITOR);
				continue;
			}

			VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem vulnerabilitiesQueueItem = null;
			try {
				vulnerabilitiesQueueItem = vulnerabilitiesQueue.peek();
				if (processPushVulnerabilitiesQueueItem(vulnerabilitiesQueueItem)) {
					vulnerabilitiesQueue.remove();
				} else {
					reEnqueueItem(vulnerabilitiesQueueItem);
				}
			} catch (TemporaryException tque) {
				logger.error("temporary error on " + vulnerabilitiesQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
				if (vulnerabilitiesQueueItem != null) {
					reEnqueueItem(vulnerabilitiesQueueItem);
				}
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pqie) {
				logger.error("permanent error on " + vulnerabilitiesQueueItem + ", passing over", pqie);
				vulnerabilitiesQueue.remove();
			} catch (Throwable t) {
				logger.error("unexpected error on build log item '" + vulnerabilitiesQueueItem + "', passing over", t);
				vulnerabilitiesQueue.remove();
			}
		}
	}

	private boolean preflightRequest(String jobId, String buildId) throws IOException {
		String encodedJobId = CIPluginSDKUtils.urlEncodePathParam(jobId);
		String encodedBuildId = CIPluginSDKUtils.urlEncodePathParam(buildId);

		OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setUrl(getVulnerabilitiesPreFlightContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
						"?instance-id=" + configurer.octaneConfiguration.getInstanceId() + "&job-ci-id=" + encodedJobId + "&build-ci-id=" + encodedBuildId);

		OctaneResponse response = restService.obtainOctaneRestClient().execute(preflightRequest);
		if (response.getStatus() == HttpStatus.SC_OK) {
			if ("true".equals(response.getBody())) {
				logger.info("vulnerabilities data of " + jobId + " #" + buildId + " found to be relevant to Octane");
				return true;
			} else {
				logger.info("vulnerabilities data of " + jobId + " #" + buildId + " is not relevant to Octane");
				return false;
			}
		}
		if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
			throw new TemporaryException("vulnerabilities preflight request FAILED, service unavailable");
		} else {
			throw new PermanentException("vulnerabilities preflight request FAILED with " + response.getStatus() + "");
		}
	}

	private void pushVulnerabilities(InputStream vulnerabilities, String jobId, String buildId) throws IOException {
		OctaneRestClient octaneRestClient = restService.obtainOctaneRestClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		String encodedJobId = CIPluginSDKUtils.urlEncodePathParam(jobId);
		String encodedBuildId = CIPluginSDKUtils.urlEncodePathParam(buildId);
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.POST)
				.setUrl(getVulnerabilitiesContextPath(configurer.octaneConfiguration.getUrl(), configurer.octaneConfiguration.getSharedSpace()) +
						"?instance-id=" + configurer.octaneConfiguration.getInstanceId() + "&job-ci-id=" + encodedJobId + "&build-ci-id=" + encodedBuildId)
				.setHeaders(headers)
				.setBody(vulnerabilities);

		OctaneResponse response = octaneRestClient.execute(request);
		logger.info("vulnerabilities pushed; status: " + response.getStatus() + ", response: " + response.getBody());
		if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
			logger.info("vulnerabilities push SUCCEED for " + jobId + " #" + buildId);
		} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
			throw new TemporaryException("vulnerabilities push FAILED, service unavailable");
		} else {
			throw new PermanentException("vulnerabilities push FAILED, status " + response.getStatus() + "; dropping this item from the queue \n" + response.getBody());
		}
	}

	private boolean processPushVulnerabilitiesQueueItem(VulnerabilitiesQueueItem vulnerabilitiesQueueItem) {
		try {
			SSCProjectConfiguration sscProjectConfiguration = configurer.pluginServices.getSSCProjectConfiguration(vulnerabilitiesQueueItem.jobId, vulnerabilitiesQueueItem.buildId);
			if (sscProjectConfiguration == null || !sscProjectConfiguration.isValid()) {
				logger.debug("SSC project configurations is missing or not valid, skipping processing for " + vulnerabilitiesQueueItem.jobId + " #" + vulnerabilitiesQueueItem.buildId);
				return true;
			}

			//  if this is the first time in the queue , check if vulnerabilities relevant to octane, and if not remove it from the queue.
			if (!vulnerabilitiesQueueItem.isRelevant) {
				boolean relevant = preflightRequest(vulnerabilitiesQueueItem.jobId, vulnerabilitiesQueueItem.buildId);
				if (relevant) {
					//  set queue item value relevancy to true and continue
					vulnerabilitiesQueueItem.isRelevant = true;
				} else {
					//  return with true to silently proceed to the next item
					return true;
				}
			}
			InputStream vulnerabilitiesStream = getVulnerabilitiesScanResultStream(vulnerabilitiesQueueItem, sscProjectConfiguration);
			if (vulnerabilitiesStream == null) {
				return false;
			} else {
				pushVulnerabilities(vulnerabilitiesStream, vulnerabilitiesQueueItem.jobId, vulnerabilitiesQueueItem.buildId);
				return true;
			}
		} catch (IOException e) {
			throw new PermanentException(e);
		}
	}

	private void reEnqueueItem(VulnerabilitiesQueueItem vulnerabilitiesQueueItem) {
		Long timePass = System.currentTimeMillis() - vulnerabilitiesQueueItem.startTime;
		vulnerabilitiesQueue.remove();
		if (timePass < vulnerabilitiesQueueItem.timeout) {
			vulnerabilitiesQueue.add(vulnerabilitiesQueueItem);
		} else {
			logger.info(vulnerabilitiesQueueItem.buildId + "/" + vulnerabilitiesQueueItem.jobId + " was removed from queue after timeout in queue is over");
		}
		CIPluginSDKUtils.doWait(SKIP_QUEUE_ITEM_INTERVAL);
	}

	private InputStream getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem vulnerabilitiesQueueItem, SSCProjectConfiguration sscProjectConfiguration) {
		String targetDir = getTargetDir(vulnerabilitiesQueueItem);
		InputStream result = getCachedScanResult(targetDir);
		if (result != null) {
			return result;
		}
		SSCHandler sscHandler = new SSCHandler(
				vulnerabilitiesQueueItem,
				sscProjectConfiguration,
				targetDir,
				this.restService.obtainSSCRestClient());
		return sscHandler.getLatestScan();
	}

	private String getTargetDir(VulnerabilitiesQueueItem vulnerabilitiesQueueItem) {
		File allowedOctaneStorage = configurer.pluginServices.getAllowedOctaneStorage();
		if (allowedOctaneStorage == null) {
			logger.info("hosting plugin does not provide storage, vulnerabilities won't be cached");
			return null;
		}
		return allowedOctaneStorage.getPath() + File.separator + vulnerabilitiesQueueItem.jobId + File.separator + vulnerabilitiesQueueItem.buildId;
	}

	private InputStream getCachedScanResult(String runRootDir) {
		if (runRootDir == null) {
			return null;
		}
		InputStream result = null;
		String vulnerabilitiesScanFilePath = runRootDir + File.separator + "securityScan.json";
		File vulnerabilitiesScanFile = new File(vulnerabilitiesScanFilePath);
		if (!vulnerabilitiesScanFile.exists()) {
			return null;
		}
		try {
			result = new FileInputStream(vulnerabilitiesScanFilePath);
		} catch (IOException ioe) {
			logger.error("failed to obtain  vulnerabilities Scan File in " + runRootDir);
		}
		return result;
	}

	private String getVulnerabilitiesPreFlightContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.VULNERABILITIES_PRE_FLIGHT;
	}

	private String getVulnerabilitiesContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.VULNERABILITIES;
	}

	public static final class VulnerabilitiesQueueItem implements QueueingService.QueueItem {
		public String jobId;
		public String buildId;
		public Long startTime;
		public Long timeout;
		public boolean isRelevant = false;

		//  [YG] this constructor MUST be present, don't remove
		private VulnerabilitiesQueueItem() {
		}

		private VulnerabilitiesQueueItem(String jobId, String buildId) {
			if (jobId == null || jobId.isEmpty())
				throw new IllegalArgumentException("job ID MUST NOT be null nor empty");
			if (buildId == null || buildId.isEmpty())
				throw new IllegalArgumentException("build ID MUST NOT be null nor empty");

			this.jobId = jobId;
			this.buildId = buildId;
		}

		@Override
		public String toString() {
			return "'" + jobId + " #" + buildId + "'";
		}
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
}
