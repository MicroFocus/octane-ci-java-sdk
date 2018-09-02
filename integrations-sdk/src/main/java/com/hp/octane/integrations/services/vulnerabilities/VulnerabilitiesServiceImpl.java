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
import com.hp.octane.integrations.spi.VulnerabilitiesStatus;
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
	private long sscPollingInterval = 10000;

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
		//setActualSSCPollingInterval();
		startBackgroundWorker();
		logger.info("initialized SUCCESSFULLY (backed by " + vulnerabilitiesQueue.getClass().getSimpleName() + ")");
	}

	private void setActualSSCPollingInterval() {


		if(pluginServices != null &&
				pluginServices.getServerInfo() != null) {
			long pollingIntervalSeconds = pluginServices.getServerInfo().getSSCPollingIntervalSeconds();

			if (pollingIntervalSeconds <= 0) {
				this.sscPollingInterval = LIST_EMPTY_INTERVAL;
				return;
			}
			this.sscPollingInterval = pollingIntervalSeconds * 1000;// Millies.
		}
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
	public void enqueuePushVulnerabilitiesScanResult(String jobId, String buildId,
													 String projectName, String projectVersion,
													 String outDir,
													 long startRunTime) {
		VulnerabilitiesQueueItem vulnerabilitiesQueueItem = new VulnerabilitiesQueueItem(jobId, buildId);
		vulnerabilitiesQueueItem.setTargetFolder(outDir);
		vulnerabilitiesQueueItem.setProjectName(projectName);
		vulnerabilitiesQueueItem.setProjectVersionSymbol(projectVersion);
		vulnerabilitiesQueueItem.setStartRunTime(startRunTime);
		vulnerabilitiesQueue.add(vulnerabilitiesQueueItem);
		logger.info(vulnerabilitiesQueueItem.buildId+"/"+vulnerabilitiesQueueItem.jobId+" was added to vulnerabilities queue");
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
				int i = 0;
				while (true) {
					i++;
					if(i == 10){
						i=0;
						setActualSSCPollingInterval();
					}
					if (vulnerabilitiesQueue.size() > 0) {
						VulnerabilitiesQueueItem vulnerabilitiesQueueItem = null;
						try {
							vulnerabilitiesQueueItem = vulnerabilitiesQueue.peek();
							//if this is the first time in the queue , check if vulnerabilities relevant to octane, and if not remove it from the queue.
							if(vulnerabilitiesQueueItem.getRetryTimes()%100==0 && !isVulnerabilitiesRelevant(vulnerabilitiesQueueItem.jobId,vulnerabilitiesQueueItem.buildId)){
								logger.info("vulnerabilities are not relevant, removing from the queue");
								vulnerabilitiesQueue.remove();
								logger.info(vulnerabilitiesQueueItem.buildId+"/"+vulnerabilitiesQueueItem.jobId+" was removed from vulnerabilities queue");
							}else{
								vulnerabilitiesQueueItem.increaseRetry();
							}
							VulnerabilitiesStatus scanResultStream = getVulnerabilitiesScanResultStream(vulnerabilitiesQueueItem);
							InputStream vulnerabilitiesStream = scanResultStream.issuesStream;
							if(vulnerabilitiesStream==null) {
								handleQueueItem(vulnerabilitiesQueueItem, scanResultStream.polling);
							}else{
								OctaneResponse response = pushVulnerabilities(vulnerabilitiesStream,vulnerabilitiesQueueItem.jobId, vulnerabilitiesQueueItem.buildId);
								if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
									logger.info("vulnerabilities push SUCCEED");
									vulnerabilitiesQueue.remove();
									logger.info(vulnerabilitiesQueueItem.buildId+"/"+vulnerabilitiesQueueItem.jobId+" was removed from vulnerabilities queue");
								} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
									logger.info("vulnerabilities push FAILED, service unavailable; retrying after a breathe...");
									breathe(SERVICE_UNAVAILABLE_BREATHE_INTERVAL);
									handleQueueItem(vulnerabilitiesQueueItem, scanResultStream.polling);
								} else {
									//  case of any other fatal error
									logger.error("vulnerabilities push FAILED, status " + response.getStatus() + "; dropping this item from the queue \n" + response.getBody());
									handleQueueItem(vulnerabilitiesQueueItem, scanResultStream.polling);
								}
								logger.debug("successfully processed " + vulnerabilitiesQueueItem);
							}
						} catch (TemporaryQueueItemException tque) {
							logger.error("temporary error on " + vulnerabilitiesQueueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
							handleQueueItem(vulnerabilitiesQueueItem, VulnerabilitiesStatus.Polling.ContinuePolling);
							breathe(TEMPORARY_ERROR_BREATHE_INTERVAL);
						} catch (PermanentQueueItemException pqie) {
							logger.error("permanent error on " + vulnerabilitiesQueueItem + ", passing over", pqie);
							handleQueueItem(vulnerabilitiesQueueItem, VulnerabilitiesStatus.Polling.ContinuePolling);
						} catch (Throwable t) {
							logger.error("unexpected error on build log item '" + vulnerabilitiesQueueItem + "', passing over", t);
							handleQueueItem(vulnerabilitiesQueueItem, VulnerabilitiesStatus.Polling.ContinuePolling);
						}
					} else {
						breathe(getPollingIntervalMillies());
					}
				}
			}
			private void handleQueueItem(VulnerabilitiesQueueItem vulnerabilitiesQueueItem, VulnerabilitiesStatus.Polling polling) {
				Long timePass = System.currentTimeMillis() - vulnerabilitiesQueueItem.getStartTime();
				vulnerabilitiesQueue.remove();
				if(timePass<TIME_OUT_FOR_QUEUE_ITEM && polling.equals(VulnerabilitiesStatus.Polling.ContinuePolling)) {
					vulnerabilitiesQueue.add(vulnerabilitiesQueueItem);
				}else{
					logger.info(vulnerabilitiesQueueItem.buildId+"/"+vulnerabilitiesQueueItem.jobId+" was removed from queue after timeout in queue is over");
				}
				breathe(getPollingIntervalMillies());
			}
		});
	}

	public VulnerabilitiesStatus getVulnerabilitiesScanResultStream(VulnerabilitiesQueueItem vulnerabilitiesQueueItem){


		SSCHandler sscHandler = new SSCHandler(vulnerabilitiesQueueItem,this.pluginServices.getServerInfo().getSSCURL(),
				this.pluginServices.getServerInfo().getSSCBaseAuthToken());
		//check connection to ssc server
		if(sscHandler!=null && !sscHandler.isConnected()){
			logger.warn("ssc is not connected, need to check all ssc configurations in order to continue with this task ");
			return new VulnerabilitiesStatus(VulnerabilitiesStatus.Polling.ContinuePolling, null);
		}
		//check if scan already exists
		InputStream result = null;

		result = tryGetVulnerabilitiesScanFile(vulnerabilitiesQueueItem.targetFolder);
		if(result!=null){
			return new VulnerabilitiesStatus(VulnerabilitiesStatus.Polling.ScanIsCompleted, result);
		}
		//if file not exists yet , check if scan is finished and handle accordingly
		VulnerabilitiesStatus.Polling scanFinishStatus = sscHandler.getScanFinishStatus();
		if(!scanFinishStatus.equals(VulnerabilitiesStatus.Polling.ScanIsCompleted)){
			return new VulnerabilitiesStatus(scanFinishStatus,null);
		}
		//scan finished :
		//process scan
		//save scan results inside build
		sscHandler.getLatestScan();
		return new VulnerabilitiesStatus(scanFinishStatus ,tryGetVulnerabilitiesScanFile(vulnerabilitiesQueueItem.targetFolder));
	}

	private InputStream tryGetVulnerabilitiesScanFile(String runRootDir) {
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
	public int getPollingIntervalMillies() {
		return (int)sscPollingInterval;
	}

	public static final class VulnerabilitiesQueueItem implements QueueService.QueueItem {
		public String jobId;
		public String buildId;
		public String projectName;
		public String projectVersionSymbol;
		public String targetFolder;

		public Long startTime;
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
		}

		@Override
		public String toString() {
			return "'" + jobId + " #" + buildId + "'";
		}

		public void setProjectName(String projectName) {
			this.projectName = projectName;
		}

		public void setProjectVersionSymbol(String projectVersionSymbol) {
			this.projectVersionSymbol = projectVersionSymbol;
		}

		public void setTargetFolder(String targetFolder) {
			this.targetFolder = targetFolder;
		}

		public void setStartRunTime(long startTime) {
			this.startTime = startTime;
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
