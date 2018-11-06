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

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventsList;
import com.hp.octane.integrations.exceptions.PermanentException;
import com.hp.octane.integrations.exceptions.TemporaryException;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.services.rest.RestService.ANALYTICS_CI_PATH_PART;
import static com.hp.octane.integrations.services.rest.RestService.CONTENT_TYPE_HEADER;
import static com.hp.octane.integrations.services.rest.RestService.SHARED_SPACE_INTERNAL_API_PATH_PART;

/**
 * EventsService implementation
 */

final class EventsServiceImpl implements EventsService {
	private static final Logger logger = LogManager.getLogger(EventsServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final ExecutorService eventsPushExecutor = Executors.newSingleThreadExecutor(new EventsServiceWorkerThreadFactory());
	private final OctaneSDK.SDKServicesConfigurer configurer;
	private final RestService restService;
	private final List<CIEvent> events = Collections.synchronizedList(new LinkedList<>());

	private final Object NO_EVENTS_MONITOR = new Object();
	private final int EVENTS_CHUNK_SIZE = System.getProperty("octane.sdk.events.chunk-size") != null ? Integer.parseInt(System.getProperty("octane.sdk.events.chunk-size")) : 10;
	private final int MAX_EVENTS_TO_KEEP = System.getProperty("octane.sdk.events.max-to-keep") != null ? Integer.parseInt(System.getProperty("octane.sdk.events.max-to-keep")) : 3000;
	private final long NO_EVENTS_PAUSE = System.getProperty("octane.sdk.events.empty-list-pause") != null ? Integer.parseInt(System.getProperty("octane.sdk.events.empty-list-pause")) : 15000;
	private final long TEMPORARY_FAILURE_PAUSE = System.getProperty("octane.sdk.events.temp-fail-pause") != null ? Integer.parseInt(System.getProperty("octane.sdk.events.temp-fail-pause")) : 15000;

	EventsServiceImpl(OctaneSDK.SDKServicesConfigurer configurer, RestService restService) {
		if (configurer == null || configurer.pluginServices == null || configurer.octaneConfiguration == null) {
			throw new IllegalArgumentException("invalid configurer");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		this.configurer = configurer;
		this.restService = restService;

		logger.info("starting background worker...");
		eventsPushExecutor.execute(this::worker);
		logger.info("initialized SUCCESSFULLY");
	}

	@Override
	public void publishEvent(CIEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("event MUST NOT be null");
		}

		events.add(event);
		int eventsSize = events.size();
		if (eventsSize > MAX_EVENTS_TO_KEEP) {
			logger.warn("reached MAX amount of events to keep in queue (max - " + MAX_EVENTS_TO_KEEP + ", found - " + eventsSize + "), capping the head");
			while (events.size() > MAX_EVENTS_TO_KEEP) {        //  in this case we need to read the real-time size of the list
				events.remove(0);
			}
		}
		synchronized (NO_EVENTS_MONITOR) {
			NO_EVENTS_MONITOR.notify();
		}
	}

	@Override
	public void shutdown() {
		eventsPushExecutor.shutdown();
	}

	private void removeEvents(List<CIEvent> eventsToRemove) {
		if (eventsToRemove != null && !eventsToRemove.isEmpty()) {
			events.removeAll(eventsToRemove);
		}
	}

	//  infallible everlasting worker function
	private void worker() {
		while (!eventsPushExecutor.isShutdown()) {
			//  have any events to send?
			if (events.isEmpty()) {
				CIPluginSDKUtils.doBreakableWait(NO_EVENTS_PAUSE, NO_EVENTS_MONITOR);
				continue;
			}

			//  build events list to be sent
			List<CIEvent> eventsChunk = null;
			CIEventsList eventsSnapshot;
			try {
				eventsChunk = new ArrayList<>(events.subList(0, Math.min(events.size(), EVENTS_CHUNK_SIZE)));
				CIServerInfo serverInfo = configurer.pluginServices.getServerInfo();
				serverInfo.setInstanceId(configurer.octaneConfiguration.getInstanceId());
				eventsSnapshot = dtoFactory.newDTO(CIEventsList.class)
						.setServer(serverInfo)
						.setEvents(eventsChunk);
			} catch (Throwable t) {
				logger.error("failed to serialize chunk of " + (eventsChunk != null ? eventsChunk.size() : "[NULL]") + " events, dropping them off (if any) and continue");
				removeEvents(eventsChunk);
				continue;
			}

			//  send the data to Octane
			try {
				logEventsToBeSent(configurer.octaneConfiguration, eventsSnapshot);
				sendEventsData(configurer.octaneConfiguration, eventsSnapshot);
				removeEvents(eventsChunk);
				logger.info("... done, left to send " + events.size() + " events");
			} catch (TemporaryException tqie) {
				logger.error("failed to send events with temporary error, breathing " + TEMPORARY_FAILURE_PAUSE + "ms and continue", tqie);
				CIPluginSDKUtils.doWait(TEMPORARY_FAILURE_PAUSE);
			} catch (PermanentException pqie) {
				logger.error("failed to send events with permanent error, dropping this chunk and continue", pqie);
				removeEvents(eventsChunk);
			} catch (Throwable t) {
				logger.error("failed to send events with unexpected error, dropping this chunk and continue", t);
				removeEvents(eventsChunk);
			}
		}
	}

	private void logEventsToBeSent(OctaneConfiguration configuration, CIEventsList eventsList) {
		try {
			String targetOctane = configuration.getUrl() + ", SP: " + configuration.getSharedSpace();
			List<String> eventsStringified = new LinkedList<>();
			for (CIEvent event : eventsList.getEvents()) {
				eventsStringified.add(event.getProject() + ":" + event.getBuildCiId() + ":" + event.getEventType());
			}
			logger.info("sending [" + String.join(", ", eventsStringified) + "] event/s to [" + targetOctane + "]...");
		} catch (Exception e) {
			logger.error("failed to log events to be sent", e);
		}
	}

	private void sendEventsData(OctaneConfiguration configuration, CIEventsList eventsList) {
		Map<String, String> headers = new HashMap<>();
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		OctaneRequest octaneRequest = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.PUT)
				.setUrl(configuration.getUrl() +
						SHARED_SPACE_INTERNAL_API_PATH_PART + configuration.getSharedSpace() +
						ANALYTICS_CI_PATH_PART + "events?ci_server_identity=" + configurer.octaneConfiguration.getInstanceId())
				.setHeaders(headers)
				.setBody(dtoFactory.dtoToJsonStream(eventsList));
		OctaneResponse octaneResponse;
		try {
			octaneResponse = restService.obtainOctaneRestClient().execute(octaneRequest);
		} catch (IOException ioe) {
			throw new TemporaryException(ioe);
		}
		if (octaneResponse.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
			throw new TemporaryException("PUT events failed with status " + octaneResponse.getStatus());
		} else if (octaneResponse.getStatus() != HttpStatus.SC_OK) {
			throw new PermanentException("PUT events failed with status " + octaneResponse.getStatus());
		}
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
