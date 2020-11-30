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

package com.hp.octane.integrations.services.events;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.CIEventsList;
import com.hp.octane.integrations.dto.events.MultiBranchType;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.RequestTimeoutException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.services.WorkerPreflight;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.services.rest.RestService.*;

/**
 * EventsService implementation
 * Handled by :
 * com.hp.mqm.analytics.common.resources.CIAnalyticsCommonSSAResource#handleEvents
 * com.hp.mqm.analytics.devops.insights.services.liveview.LiveSnapshotsServiceOnEventImpl#processEvent
 */

final class EventsServiceImpl implements EventsService {
	private static final Logger logger = LogManager.getLogger(EventsServiceImpl.class);
	Marker eventsMarker = MarkerManager.getMarker("EVENTS");
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final ExecutorService eventsPushExecutor = Executors.newSingleThreadExecutor(new EventsServiceWorkerThreadFactory());
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;
	private final ConfigurationService configurationService;
	private final List<CIEvent> events = Collections.synchronizedList(new LinkedList<>());


	private final int EVENTS_CHUNK_SIZE = System.getProperty("octane.sdk.events.chunk-size") != null ? Integer.parseInt(System.getProperty("octane.sdk.events.chunk-size")) : 10;
	private final int MAX_EVENTS_TO_KEEP = System.getProperty("octane.sdk.events.max-to-keep") != null ? Integer.parseInt(System.getProperty("octane.sdk.events.max-to-keep")) : 3000;
	private final long TEMPORARY_FAILURE_PAUSE = System.getProperty("octane.sdk.events.temp-fail-pause") != null ? Integer.parseInt(System.getProperty("octane.sdk.events.temp-fail-pause")) : 15000;

	//Metrics
	private long requestTimeoutCount = 0;
	private long lastRequestTimeoutTime = 0;
	private final WorkerPreflight workerPreflight;

	EventsServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService, ConfigurationService configurationService) {
		if (configurer == null || configurer.pluginServices == null || configurer.octaneConfiguration == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}
		if (configurationService == null) {
			throw new IllegalArgumentException("configuration service MUST NOT be null");
		}

		this.configurer = configurer;
		this.restService = restService;
		this.configurationService = configurationService;
		this.workerPreflight = new WorkerPreflight(this, configurationService, logger);
		workerPreflight.setWaitAfterConnection(false);

		logger.info(configurer.octaneConfiguration.geLocationForLog() + "starting background worker...");
		eventsPushExecutor.execute(this::worker);
		logger.info(configurer.octaneConfiguration.geLocationForLog() + "initialized SUCCESSFULLY");
	}

	@Override
	public void publishEvent(CIEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("event MUST NOT be null");
		}

		if (this.configurer.octaneConfiguration.isDisabled()) {
			return;
		}


		if (ConfigurationParameterFactory.octaneRootsCacheAllowed(configurer.octaneConfiguration)) {
			Set<String> parents = new HashSet<>();
			CIPluginSDKUtils.getRootJobCiIds(event.getProject(), event.getCauses(), parents);
			if (!configurationService.isRelevantForOctane(parents)) {
				if (CIEventType.STARTED.equals(event.getEventType())) {
					String eventStr = event.getProject() + ":" + event.getBuildCiId() + ":" + event.getEventType() + ", parents : " + parents;
					logger.info(configurer.octaneConfiguration.geLocationForLog() + "Event is ignored : " + eventStr);
				}
				return;
			}
		}

		events.add(event);
		int eventsSize = events.size();
		if (eventsSize > MAX_EVENTS_TO_KEEP) {
			logger.warn(configurer.octaneConfiguration.geLocationForLog() + "reached MAX amount of events to keep in queue (max - " + MAX_EVENTS_TO_KEEP + ", found - " + eventsSize + "), capping the head");
			while (events.size() > MAX_EVENTS_TO_KEEP) {        //  in this case we need to read the real-time size of the list
				events.remove(0);
			}
		}
		workerPreflight.itemAddedToQueue();
	}

	@Override
	public long getQueueSize() {
		return events.size();
	}

	@Override
	public void clearQueue() {
		events.clear();
	}

	@Override
	public void shutdown() {
		eventsPushExecutor.shutdown();
	}

	@Override
	public boolean isShutdown() {
		return eventsPushExecutor.isShutdown();
	}

	private void removeEvents(List<CIEvent> eventsToRemove) {
		if (eventsToRemove != null && !eventsToRemove.isEmpty()) {
			events.removeAll(eventsToRemove);
		}
	}

	//  infallible everlasting worker function
	private void worker() {
		while (!eventsPushExecutor.isShutdown()) {
			if(!workerPreflight.preflight()){
				continue;
			}

			//  build events list to be sent
			List<CIEvent> eventsChunk = null;
			CIEventsList eventsSnapshot;
			try {
				eventsChunk = getEventsChunk();

				CIServerInfo serverInfo = configurer.pluginServices.getServerInfo();
				serverInfo.setInstanceId(configurer.octaneConfiguration.getInstanceId());
				eventsSnapshot = dtoFactory.newDTO(CIEventsList.class)
						.setServer(serverInfo)
						.setEvents(eventsChunk);
			} catch (Throwable t) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() +"failed to serialize chunk of " + (eventsChunk != null ? eventsChunk.size() : "[NULL]") + " events, dropping them off (if any) and continue");
				removeEvents(eventsChunk);
				continue;
			}

			//  send the data to Octane
			try {
				String correlationId = CIPluginSDKUtils.getNextCorrelationId();
				logEventsToBeSent(eventsSnapshot, correlationId);
				sendEventsData(eventsSnapshot, correlationId);
				removeEvents(eventsChunk);
				if (events.size() > 0) {
					logger.info(configurer.octaneConfiguration.geLocationForLog() + "left to send " + events.size() + " events");
				}
			} catch (RequestTimeoutException rte){
				requestTimeoutCount++;
				lastRequestTimeoutTime = System.currentTimeMillis();
				logger.info(configurer.octaneConfiguration.geLocationForLog() + rte.getMessage());
				CIPluginSDKUtils.doWait(TEMPORARY_FAILURE_PAUSE);
			} catch (TemporaryException tqie) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() + "failed to send events with temporary error, breathing " + TEMPORARY_FAILURE_PAUSE + "ms and continue", tqie);
				CIPluginSDKUtils.doWait(TEMPORARY_FAILURE_PAUSE);
			} catch (PermanentException pqie) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() + "failed to send events with permanent error, dropping this chunk and continue", pqie);
				removeEvents(eventsChunk);
			} catch (Throwable t) {
				logger.error(configurer.octaneConfiguration.geLocationForLog() + "failed to send events with unexpected error, dropping this chunk and continue", t);
				removeEvents(eventsChunk);
			}
		}
	}

	private List<CIEvent> getEventsChunk() {
		int maxInBulk = ConfigurationParameterFactory.isSendEventsInBulk(configurer.octaneConfiguration) ? EVENTS_CHUNK_SIZE : 1;
		List<CIEvent> eventsChunk = new ArrayList<>(events.subList(0, Math.min(events.size(), maxInBulk)));

		// - octane generate multibranch child pipeline on the fly
		// - multibranch child may trigger another job
		// - if multibranch child pipeline still didn't created, and multibranch child start event comes along with
		//    downstream job start event, the latest event is thrown in PipelinesServiceImpl#shouldProcessEvent.
		//    So first run of pipeline might be partial (without structure,tests,commits)
		// - if in iteration we encounter multibranch child start event - no other event is allowed to be after it and will be pushed in next bulk
		if (eventsChunk.size() > 1) {
			for (int i = 0; i < eventsChunk.size(); i++) {
				CIEvent ciEvent = eventsChunk.get(i);
				if (CIEventType.STARTED.equals(ciEvent.getEventType()) && MultiBranchType.MULTI_BRANCH_CHILD.equals(ciEvent.getMultiBranchType()) && i + 1 < eventsChunk.size()) {
					eventsChunk = new ArrayList<>(eventsChunk.subList(0, i + 1));
					break;
				}
			}
		}
		return eventsChunk;
	}

	private void logEventsToBeSent(CIEventsList eventsList, String correlationId) {
		try {
			List<String> eventsStringified = new LinkedList<>();
			for (CIEvent event : eventsList.getEvents()) {
				String str = event.getProject() + ":" + event.getBuildCiId() + ":" + event.getEventType();
				if (CIEventType.FINISHED.equals(event.getEventType()) && event.getTestResultExpected() != null && event.getTestResultExpected() == true) {
					str += "(tests=true)";
				}
				eventsStringified.add(str);

			}
			logger.info(configurer.octaneConfiguration.geLocationForLog() + "sending [" + String.join(", ", eventsStringified) + "] event/s. Correlation ID - " + correlationId);

			if (ConfigurationParameterFactory.isLogEvents(configurer.octaneConfiguration)) {
				for (CIEvent event : eventsList.getEvents()) {
					String str = String.format("%s%s:%s:%s %s", configurer.octaneConfiguration.geLocationForLog(), event.getProject(), event.getBuildCiId(),
							event.getEventType(), dtoFactory.dtoToJson(event));
					logger.info(eventsMarker, str);
				}
			}
		} catch (Exception e) {
			logger.error(configurer.octaneConfiguration.geLocationForLog() + "failed to log events to be sent", e);
		}
	}

	private void sendEventsData(CIEventsList eventsList, String correlationId) {
		Map<String, String> headers = new HashMap<>();
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		headers.put(CORRELATION_ID_HEADER, correlationId);
		OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.PUT)
				.setUrl(configurer.octaneConfiguration.getUrl() +
						SHARED_SPACE_INTERNAL_API_PATH_PART + configurer.octaneConfiguration.getSharedSpace() +
						ANALYTICS_CI_PATH_PART + "events?ci_server_identity=" + configurer.octaneConfiguration.getInstanceId())
				.setHeaders(headers)
				.setTimeoutSec(60)
				.setBody(dtoFactory.dtoToJsonStream(eventsList));
		OctaneResponse octaneResponse;
		try {
			octaneResponse = restService.obtainOctaneRestClient().execute(octaneRequest);
		} catch (InterruptedIOException ie) {
			String msg = "!!!!!!!!!!!!!!!!!!! request timeout" + ie.getClass().getCanonicalName() + " - " + ie.getMessage();
			throw new RequestTimeoutException(msg);
		} catch (IOException ioe) {
			throw new TemporaryException(ioe);
		}
		if (octaneResponse.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE || octaneResponse.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
			throw new TemporaryException("PUT events failed with status " + octaneResponse.getStatus());
		} else if (octaneResponse.getStatus() == HttpStatus.SC_UNAUTHORIZED || octaneResponse.getStatus() == HttpStatus.SC_FORBIDDEN) {
			CIPluginSDKUtils.doWait(30000);
			throw new PermanentException("PUT events failed with status " + octaneResponse.getStatus());
		} else if (octaneResponse.getStatus() != HttpStatus.SC_OK) {
			if (CIPluginSDKUtils.isServiceTemporaryUnavailable(octaneResponse.getBody())) {
				throw new TemporaryException("Saas service is temporary unavailable.");
			}
			throw new PermanentException("PUT events failed with status " + octaneResponse.getStatus());
		}
	}

	@Override
	public Map<String, Object> getMetrics() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("isShutdown", this.isShutdown());
		map.put("queueSize", this.getQueueSize());
		map.put("requestTimeoutCount", this.requestTimeoutCount);
		if (lastRequestTimeoutTime > 0) {
			map.put("lastRequestTimeoutTime", new Date(lastRequestTimeoutTime));
		}
		workerPreflight.addMetrics(map);
		return map;
	}

	private static final class EventsServiceWorkerThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable);
			result.setName("EventsServiceWorker-" + result.getId());
			result.setDaemon(true);
			return result;
		}
	}
}
