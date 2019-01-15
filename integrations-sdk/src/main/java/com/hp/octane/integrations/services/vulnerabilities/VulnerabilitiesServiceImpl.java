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

import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.queueing.QueueingService;
import com.hp.octane.integrations.services.sonar.SonarService;
import com.hp.octane.integrations.services.vulnerabilities.ssc.SSCService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.squareup.tape.ObjectQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Default implementation of vulnerabilities service
 */

public class VulnerabilitiesServiceImpl implements VulnerabilitiesService {
	private static final Logger logger = LogManager.getLogger(VulnerabilitiesServiceImpl.class);
	private static final String VULNERABILITIES_QUEUE_FILE = "vulnerabilities-queue.dat";

	private final ExecutorService vulnerabilitiesProcessingExecutor = Executors.newSingleThreadExecutor(new VulnerabilitiesPushWorkerThreadFactory());
	private final Object NO_VULNERABILITIES_RESULTS_MONITOR = new Object();
	private final ObjectQueue<VulnerabilitiesQueueItem> vulnerabilitiesQueue;
	Map<ToolType,VulnerabilitiesToolService> vulnerabilitiesToolServiceMap = new HashMap<>();
	protected final OctaneVulnerabilitiesService octaneVulnerabilitiesService;


	private int TEMPORARY_ERROR_BREATHE_INTERVAL = 10000;
	private int LIST_EMPTY_INTERVAL = 10000;
	private int SKIP_QUEUE_ITEM_INTERVAL = 5000;
	private Long DEFAULT_TIME_OUT_FOR_QUEUE_ITEM = 12 * 60 * 60 * 1000L;
	private CompletableFuture<Boolean> workerExited;


	public VulnerabilitiesServiceImpl(QueueingService queueingService, SSCService sscService, SonarService sonarService, OctaneVulnerabilitiesService octaneVulnerabilitiesService) {

		if (queueingService == null) {
			throw new IllegalArgumentException("queue Service MUST NOT be null");
		}
		if (octaneVulnerabilitiesService == null) {
			throw new IllegalArgumentException("octane Vulnerabilities Service service MUST NOT be null");
		}
		if (sonarService == null) {
			throw new IllegalArgumentException("sonar service MUST NOT be null");
		}
		if (sscService == null) {
			throw new IllegalArgumentException("ssc service MUST NOT be null");
		}


		if (queueingService.isPersistenceEnabled()) {
			vulnerabilitiesQueue = queueingService.initFileQueue(VULNERABILITIES_QUEUE_FILE, VulnerabilitiesQueueItem.class);
		} else {
			vulnerabilitiesQueue = queueingService.initMemoQueue();
		}

		vulnerabilitiesToolServiceMap.put(ToolType.SSC,sscService);
		vulnerabilitiesToolServiceMap.put(ToolType.SONAR,sonarService);


		this.octaneVulnerabilitiesService = octaneVulnerabilitiesService;

		logger.info("starting background worker...");
		vulnerabilitiesProcessingExecutor.execute(this::worker);
		logger.info("initialized SUCCESSFULLY (backed by " + vulnerabilitiesQueue.getClass().getSimpleName() + ")");
	}

	@Override
	public void enqueueRetrieveAndPushVulnerabilities(String jobId,
	                                                  String buildId,
	                                                  ToolType toolType,
	                                                  long startRunTime,
	                                                  long queueItemTimeout,
													  Map<String,Object> additionalProperties) {
		VulnerabilitiesQueueItem vulnerabilitiesQueueItem = new VulnerabilitiesQueueItem(jobId, buildId);
		vulnerabilitiesQueueItem.setStartTime(startRunTime);
		vulnerabilitiesQueueItem.setTimeout(queueItemTimeout <= 0 ? DEFAULT_TIME_OUT_FOR_QUEUE_ITEM : queueItemTimeout * 60 * 60 * 1000);
		vulnerabilitiesQueueItem.setToolType(toolType);
		vulnerabilitiesQueueItem.setAdditionalProperties(additionalProperties);
		vulnerabilitiesQueue.add(vulnerabilitiesQueueItem);
		logger.info(vulnerabilitiesQueueItem.getBuildId() + "/" + vulnerabilitiesQueueItem.getJobId() + " was added to vulnerabilities queue");

		synchronized (NO_VULNERABILITIES_RESULTS_MONITOR) {
			NO_VULNERABILITIES_RESULTS_MONITOR.notify();
		}
	}

	@Override
	public void shutdown() {
		workerExited = new CompletableFuture<>();
		vulnerabilitiesProcessingExecutor.shutdown();
		try {
			NO_VULNERABILITIES_RESULTS_MONITOR.notify();
			workerExited.get(3000, TimeUnit.SECONDS);
		} catch (Exception e) {
			logger.warn("interrupted while waiting for the worker SHUT DOWN");
		}
	}

	//  TODO: implement retries counter per item and strategy of discard
	//  TODO: consider moving the overall queue managing logic to some generic location
	//  infallible everlasting background worker
	private void worker() {
		while (!vulnerabilitiesProcessingExecutor.isShutdown()) {
			if (vulnerabilitiesQueue.size() == 0) {
				CIPluginSDKUtils.doBreakableWait(LIST_EMPTY_INTERVAL, NO_VULNERABILITIES_RESULTS_MONITOR);
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
				logger.error("temporary error on " + queueItem + ", breathing " + TEMPORARY_ERROR_BREATHE_INTERVAL + "ms and retrying", tque);
				if (queueItem != null) {
					reEnqueueItem(queueItem);
				}
				CIPluginSDKUtils.doWait(TEMPORARY_ERROR_BREATHE_INTERVAL);
			} catch (PermanentException pqie) {
				logger.error("permanent error on " + queueItem + ", passing over", pqie);
				vulnerabilitiesQueueItemCleanUp(queueItem);
				vulnerabilitiesQueue.remove();
			} catch (Throwable t) {
				logger.error("unexpected error on build log item '" + queueItem + "', passing over", t);
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

				Date relevant = octaneVulnerabilitiesService.vulnerabilitiesPreflightRequest(queueItem.getJobId(), queueItem.getBuildId());
				if (relevant != null) {
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

			VulnerabilitiesToolService vulnerabilitiesToolService = vulnerabilitiesToolServiceMap.get(queueItem.getToolType());
			InputStream vulnerabilitiesStream = vulnerabilitiesToolService.getVulnerabilitiesScanResultStream(queueItem);

			if (vulnerabilitiesStream == null) {
				return false;
			} else {
				 octaneVulnerabilitiesService.pushVulnerabilities(vulnerabilitiesStream, queueItem.getJobId(), queueItem.getBuildId());
				return true;
			}
		} catch (IOException e) {
			throw new PermanentException(e);
		}
	}


	private void reEnqueueItem(VulnerabilitiesQueueItem vulnerabilitiesQueueItem) {
		Long timePass = System.currentTimeMillis() - vulnerabilitiesQueueItem.getStartTime();
		vulnerabilitiesQueue.remove();
		if (timePass < vulnerabilitiesQueueItem.getTimeout()) {
			vulnerabilitiesQueue.add(vulnerabilitiesQueueItem);
		} else {
			logger.info(vulnerabilitiesQueueItem.getBuildId() + "/" + vulnerabilitiesQueueItem.getJobId() + " was removed from queue after timeout in queue is over");
		}
		CIPluginSDKUtils.doWait(SKIP_QUEUE_ITEM_INTERVAL);
	}

	private boolean vulnerabilitiesQueueItemCleanUp(VulnerabilitiesQueueItem queueItem){
		return  vulnerabilitiesToolServiceMap.get(queueItem.getToolType()).vulnerabilitiesQueueItemCleanUp(queueItem);
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
