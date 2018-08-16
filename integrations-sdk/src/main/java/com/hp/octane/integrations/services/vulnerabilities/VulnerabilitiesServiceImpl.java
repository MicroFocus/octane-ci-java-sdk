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
import com.hp.octane.integrations.api.RestClient;
import com.hp.octane.integrations.api.RestService;
import com.hp.octane.integrations.api.VulnerabilitiesService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.services.queue.PermanentQueueItemException;
import com.hp.octane.integrations.services.queue.QueueService;
import com.hp.octane.integrations.services.queue.TemporaryQueueItemException;
import com.squareup.tape.ObjectQueue;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.api.RestService.*;

/**
 * Default implementation of tests service
 */

public final class VulnerabilitiesServiceImpl extends OctaneSDK.SDKServiceBase implements VulnerabilitiesService {
	private static final Logger logger = LogManager.getLogger(VulnerabilitiesServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final ExecutorService worker = Executors.newSingleThreadExecutor(new VulnerabilitiesPushWorkerThreadFactory());
	private final RestService restService;
	private static final String VULNERABILITIES_QUEUE_FILE = "vulnerabilities-queue.dat";

	private final ObjectQueue<VulnerabilitiesQueueItem> vulnerabilitiesQueue;

	private int SERVICE_UNAVAILABLE_BREATHE_INTERVAL = 10000;
	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 10000;
	private int LIST_EMPTY_INTERVAL = 10000;
	private int SKIP_QUEUE_ITEM_INTERVAL = 5000;
	private Long TIME_OUT_FOR_QUEUE_ITEM = 3*60*60*1000L; //3 hours

	public VulnerabilitiesServiceImpl(Object internalUsageValidator,QueueService queueService, RestService restService) {
		super(internalUsageValidator);

		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		if (queueService.isPersistenceEnabled()) {
			vulnerabilitiesQueue = queueService.initFileQueue(VULNERABILITIES_QUEUE_FILE, VulnerabilitiesQueueItem.class);
		} else {
			vulnerabilitiesQueue = queueService.initMemoQueue();
		}

		this.restService = restService;

		logger.info("starting background worker...");
		startBackgroundWorker();
		logger.info("initialized SUCCESSFULLY (backed by " + vulnerabilitiesQueue.getClass().getSimpleName() + ")");
	}


	@Override
	public OctaneResponse pushVulnerabilities(InputStream vulnerabilities, String jobId, String buildId) throws IOException {
		if (vulnerabilities == null) {
			throw new IllegalArgumentException("tests result MUST NOT be null");
		}

		RestClient restClient = restService.obtainClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.POST)
				.setUrl(getVulnerabilitiesContextPath(pluginServices.getOctaneConfiguration().getUrl(), pluginServices.getOctaneConfiguration().getSharedSpace()) +
						"?instance-id='"+pluginServices.getServerInfo().getInstanceId()+"'&job-ci-id='"+jobId+"'&build-ci-id='"+buildId+"'")
				.setHeaders(headers)
				.setBody(vulnerabilities);
		OctaneResponse response = restClient.execute(request);
		logger.info("vulnerabilities pushed; status: " + response.getStatus() + ", response: " + response.getBody());
		return response;
	}

	@Override
	public void enqueuePushVulnerabilitiesScanResult(String jobCiId, String buildCiId) {
		vulnerabilitiesQueue.add(new VulnerabilitiesQueueItem(jobCiId, buildCiId));
	}

	@Override
	public boolean isVulnerabilitiesRelevant( String jobId, String buildId) throws IOException {
		if (buildId == null || buildId.isEmpty()) {
			throw new IllegalArgumentException("build CI ID MUST NOT be null nor empty");
		}
		if (jobId == null || jobId.isEmpty()) {
			throw new IllegalArgumentException("job CI ID MUST NOT be null nor empty");
		}

		OctaneRequest preflightRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.GET)
				.setUrl(getVulnerabilitiesPreFlightContextPath(pluginServices.getOctaneConfiguration().getUrl(), pluginServices.getOctaneConfiguration().getSharedSpace()) +
						"?instance-id='"+pluginServices.getServerInfo().getInstanceId()+"'&job-ci-id='"+jobId+"'&build-ci-id='"+buildId+"'");

		OctaneResponse response = restService.obtainClient().execute(preflightRequest);
		return response.getStatus() == HttpStatus.SC_OK && String.valueOf(true).equals(response.getBody());
	}
	//  TODO: implement retries counter per item and strategy of discard
	//  TODO: distinct between the item's problem, server problem and env problem and retry strategy accordingly
	//  TODO: consider moving the overall queue managing logic to some generic location
	//  this should be infallible everlasting worker
	private void startBackgroundWorker() {
		worker.execute(new Runnable() {
			public void run() {
				while (true) {
					if (vulnerabilitiesQueue.size() > 0) {
						VulnerabilitiesServiceImpl.VulnerabilitiesQueueItem vulnerabilitiesQueueItem = null;
						try {
							vulnerabilitiesQueueItem = vulnerabilitiesQueue.peek();
							//if this is the first time in the queue , check if vulnerabilities relevant to octane, and if not remove it from the queue.
							if(vulnerabilitiesQueueItem.getRetryTimes()%100==0 && !isVulnerabilitiesRelevant(vulnerabilitiesQueueItem.jobId,vulnerabilitiesQueueItem.buildId)){
								logger.info("vulnerabilities are not relevant, removing from the queue");
								vulnerabilitiesQueue.remove();
								System.out.println(vulnerabilitiesQueueItem.buildId+"/"+vulnerabilitiesQueueItem.jobId+" was removed from queue 1");
							}else{
								vulnerabilitiesQueueItem.increaseRetry();
							}
							InputStream vulnerabilitiesStream = pluginServices.getVulnerabilitiesScanResultStream(vulnerabilitiesQueueItem.jobId, vulnerabilitiesQueueItem.buildId);
							if(vulnerabilitiesStream==null) {
								handleQueueItem(vulnerabilitiesQueueItem);
							}else{
								OctaneResponse response = pushVulnerabilities(vulnerabilitiesStream,vulnerabilitiesQueueItem.jobId, vulnerabilitiesQueueItem.buildId);
								if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
									logger.info("vulnerabilities push SUCCEED");
									vulnerabilitiesQueue.remove();
									System.out.println(vulnerabilitiesQueueItem.buildId+"/"+vulnerabilitiesQueueItem.jobId+" was removed from queue 2");
								} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
									logger.info("vulnerabilities push FAILED, service unavailable; retrying after a breathe...");
									breathe(SERVICE_UNAVAILABLE_BREATHE_INTERVAL);
									handleQueueItem(vulnerabilitiesQueueItem);
								} else {
									//  case of any other fatal error
									logger.error("vulnerabilities push FAILED, status " + response.getStatus() + "; dropping this item from the queue \n"+response.getBody());
									handleQueueItem(vulnerabilitiesQueueItem);
								}
								logger.debug("successfully processed " + vulnerabilitiesQueueItem);
							}
						} catch (TemporaryQueueItemException tque) {
							logger.error("temporary error on " + vulnerabilitiesQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
							handleQueueItem(vulnerabilitiesQueueItem);
							breathe(TEMPORARY_ERROR_BREATHE_INTERVAL);
						} catch (PermanentQueueItemException pqie) {
							logger.error("permanent error on " + vulnerabilitiesQueueItem + ", passing over", pqie);
							handleQueueItem(vulnerabilitiesQueueItem);
						} catch (Throwable t) {
							logger.error("unexpected error on build log item '" + vulnerabilitiesQueueItem + "', passing over", t);
							handleQueueItem(vulnerabilitiesQueueItem);
						}
					} else {
						breathe(LIST_EMPTY_INTERVAL);
					}
				}
			}

			private void handleQueueItem(VulnerabilitiesQueueItem vulnerabilitiesQueueItem) {
				Long timePass = System.currentTimeMillis() - vulnerabilitiesQueueItem.getStartTime();
				vulnerabilitiesQueue.remove();
				System.out.println(vulnerabilitiesQueueItem.buildId+"/"+vulnerabilitiesQueueItem.jobId+" was removed from queue 3");
				if(timePass<TIME_OUT_FOR_QUEUE_ITEM) {
					vulnerabilitiesQueue.add(vulnerabilitiesQueueItem);
					System.out.println(vulnerabilitiesQueueItem.buildId+"/"+vulnerabilitiesQueueItem.jobId+" was Added to queue 3");
				}
				breathe(SKIP_QUEUE_ITEM_INTERVAL);
			}
		});
	}

	private static final class VulnerabilitiesQueueItem implements QueueService.QueueItem {
		private String jobId;
		private String buildId;
		private Long startTime;
		private int retryTimes=0;

		public int getRetryTimes() {
			return retryTimes;
		}

		public void setRetryTimes(int retryTimes) {
			this.retryTimes = retryTimes;
		}

		public void increaseRetry(){
			this.retryTimes++;
		}

		public Long getStartTime() {
			return startTime;
		}

		//  [YG] this constructor MUST be present, don't remove
		private VulnerabilitiesQueueItem() {
		}

		private VulnerabilitiesQueueItem(String jobId, String buildId) {
			this.jobId = jobId;
			this.buildId = buildId;
			this.startTime = System.currentTimeMillis();
		}

		@Override
		public String toString() {
			return "'" + jobId + " #" + buildId + "'";
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
		return octaneBaseUrl + SHARED_SPACE_API_PATH_PART + sharedSpaceId + VULNERABILITIES;
	}

	private String getVulnerabilitiesPreFlightContextPath(String octaneBaseUrl, String sharedSpaceId) {
		return octaneBaseUrl + SHARED_SPACE_API_PATH_PART + sharedSpaceId + VULNERABILITIES_PRE_FLIGHT;
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
