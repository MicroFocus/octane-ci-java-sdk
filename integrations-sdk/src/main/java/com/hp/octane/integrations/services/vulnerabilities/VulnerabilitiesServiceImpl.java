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

package com.hp.octane.integrations.services.vulnerabilities;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.rest.RestClient;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.services.queue.QueueService;
import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of vulnerabilities service
 */

final class VulnerabilitiesServiceImpl implements VulnerabilitiesService {
	private static final Logger logger = LogManager.getLogger(VulnerabilitiesServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final CIPluginServices pluginServices;
	private final RestService restService;

	private List<VulnerabilitiesQueueEntry> buildList = Collections.synchronizedList(new LinkedList<>());
	private int SERVICE_UNAVAILABLE_BREATHE_INTERVAL = 10000;
	private int LIST_EMPTY_INTERVAL = 3000;

	VulnerabilitiesServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
		if (configurer == null || configurer.pluginServices == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		this.pluginServices = configurer.pluginServices;
		this.restService = restService;

		logger.info("starting background worker...");
		Executors.newSingleThreadExecutor(new VulnerabilitiesPushWorkerThreadFactory())
				.execute(this::worker);
		logger.info("initialized SUCCESSFULLY");
	}


	@Override
	public OctaneResponse pushVulnerabilities(InputStream vulnerabilities, String jobId, String buildId) throws IOException {
		if (vulnerabilities == null) {
			throw new IllegalArgumentException("tests result MUST NOT be null");
		}

		RestClient restClient = restService.obtainClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(RestService.CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.POST)
				.setUrl(getVulnerabilitiesContextPath(pluginServices.getOctaneConfiguration().getUrl(), pluginServices.getOctaneConfiguration().getSharedSpace()) +
						"?instance-id='" + pluginServices.getServerInfo().getInstanceId() + "'&job-ci-id='" + jobId + "'&build-ci-id='" + buildId + "'")
				.setHeaders(headers)
				.setBody(vulnerabilities);
		OctaneResponse response = restClient.execute(request);
		logger.info("vulnerabilities pushed; status: " + response.getStatus() + ", response: " + response.getBody());
		return response;
	}

	@Override
	public void enqueuePushVulnerabilitiesScanResult(String jobCiId, String buildCiId) {
		buildList.add(new VulnerabilitiesQueueEntry(jobCiId, buildCiId));
	}

	@Override
	public boolean isVulnerabilitiesRelevant(String jobId, String buildId) throws IOException {
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build CI ID MUST NOT be null nor empty");
		}
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job CI ID MUST NOT be null nor empty");
		}

		OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setUrl(getVulnerabilitiesPreFlightContextPath(pluginServices.getOctaneConfiguration().getUrl(), pluginServices.getOctaneConfiguration().getSharedSpace()) +
						"?instance-id='" + pluginServices.getServerInfo().getInstanceId() + "'&job-ci-id='" + jobId + "'&build-ci-id='" + buildId + "'");

		OctaneResponse response = restService.obtainClient().execute(preflightRequest);
		return response.getStatus() == HttpStatus.SC_OK && String.valueOf(true).equals(response.getBody());
	}

	//  TODO: implement retries counter per item and strategy of discard
	//  TODO: distinct between the item's problem, server problem and env problem and retry strategy accordingly
	//  infallible everlasting background worker
	private void worker() {
		while (true) {
			if (!buildList.isEmpty()) {
				try {
					VulnerabilitiesQueueEntry vulnerabilitiesQueueEntry = buildList.get(0);
					InputStream vulnerabilitiesStream = pluginServices.getVulnerabilitiesScanResultStream(vulnerabilitiesQueueEntry.jobId, vulnerabilitiesQueueEntry.buildId);
					OctaneResponse response = pushVulnerabilities(vulnerabilitiesStream, vulnerabilitiesQueueEntry.jobId, vulnerabilitiesQueueEntry.buildId);
					if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
						logger.info("vulnerabilities push SUCCEED");
						buildList.remove(0);
					} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
						logger.info("vulnerabilities push FAILED, service unavailable; retrying after a breathe...");
						breathe(SERVICE_UNAVAILABLE_BREATHE_INTERVAL);
					} else {
						//  case of any other fatal error
						logger.error("vulnerabilities push FAILED, status " + response.getStatus() + "; dropping this item from the queue \n" + response.getBody());
						buildList.remove(0);
					}

				} catch (IOException e) {
					logger.error("vulnerabilities push failed; will retry after " + SERVICE_UNAVAILABLE_BREATHE_INTERVAL + "ms", e);
					breathe(SERVICE_UNAVAILABLE_BREATHE_INTERVAL);
				} catch (Throwable t) {
					logger.error("vulnerabilities push failed; dropping this item from the queue ", t);
					buildList.remove(0);
				}
			} else {
				breathe(LIST_EMPTY_INTERVAL);
			}
		}
	}

	//  TODO: turn to be breakable wait with timeout and notifier
	private void breathe(int period) {
		try {
			Thread.sleep(period);
		} catch (InterruptedException ie) {
			logger.error("interrupted while breathing", ie);
		}
	}

	private String getVulnerabilitiesContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.VULNERABILITIES;
	}

	private String getVulnerabilitiesPreFlightContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + RestService.SHARED_SPACE_API_PATH_PART + sharedSpaceId + RestService.VULNERABILITIES_PRE_FLIGHT;
	}

	private static final class VulnerabilitiesQueueEntry implements QueueService.QueueItem {
		private String jobId;
		private String buildId;

		//  [YG] this constructor MUST be present
		private VulnerabilitiesQueueEntry() {
		}

		private VulnerabilitiesQueueEntry(String jobId, String buildId) {
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
