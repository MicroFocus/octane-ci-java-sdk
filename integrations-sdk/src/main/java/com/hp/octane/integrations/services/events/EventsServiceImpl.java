/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.services.events;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.EventsService;
import com.hp.octane.integrations.api.RestService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventsList;
import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.hp.octane.integrations.api.RestService.ANALYTICS_CI_PATH_PART;
import static com.hp.octane.integrations.api.RestService.CONTENT_TYPE_HEADER;
import static com.hp.octane.integrations.api.RestService.SHARED_SPACE_INTERNAL_API_PATH_PART;
import static com.hp.octane.integrations.util.CIPluginSDKUtils.doWait;

/**
 * EventsService implementation
 */

public final class EventsServiceImpl extends OctaneSDK.SDKServiceBase implements EventsService {
	private static final Logger logger = LogManager.getLogger(EventsServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final ExecutorService worker = Executors.newSingleThreadExecutor(new EventsServiceWorkerThreadFactory());
	private final List<CIEvent> events = Collections.synchronizedList(new ArrayList<CIEvent>());
	private final WaitMonitor WAIT_MONITOR = new WaitMonitor();
	private final CIPluginServices pluginServices;
	private final RestService restService;

	private int MAX_SEND_RETRIES = 7;
	private int INITIAL_RETRY_PAUSE = 1739;
	private int DATA_SEND_INTERVAL = 1373;
	private int DATA_SEND_INTERVAL_IN_SUSPEND = 10 * 60 * 2;
	private int failedRetries;
	private int pauseInterval;

	public EventsServiceImpl(Object configurator, CIPluginServices pluginServices, RestService restService) {
		super(configurator);

		if (pluginServices == null) {
			throw new IllegalArgumentException("plugin services MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		this.pluginServices = pluginServices;
		this.restService = restService;
		startBackgroundWorker();
	}

	public void publishEvent(CIEvent event) {
		events.add(event);
	}

	//  this should be infallible everlasting worker
	private void startBackgroundWorker() {
		resetCounters();
		worker.execute(new Runnable() {
			public void run() {
				while (true) {
					try {
						if (events.size() > 0) {
							if (!sendData()) suspend();
						}
						doWait(DATA_SEND_INTERVAL);
					} catch (Throwable t) {
						logger.error("failed to send events", t);
					}
				}
			}
		});
	}

	private void suspend() {
		events.clear();
		failedRetries = MAX_SEND_RETRIES - 1;
		doBreakableWait(DATA_SEND_INTERVAL_IN_SUSPEND);
	}

	private void resetCounters() {
		failedRetries = 0;
		pauseInterval = INITIAL_RETRY_PAUSE;
		synchronized (WAIT_MONITOR) {
			if (worker != null) {
				WAIT_MONITOR.released = true;
				WAIT_MONITOR.notify();
			}
		}
	}

	private boolean sendData() {
		CIEventsList eventsSnapshot = dtoFactory.newDTO(CIEventsList.class)
				.setServer(pluginServices.getServerInfo())
				.setEvents(new ArrayList<>(events));
		boolean result = true;

		//  prepare some data for logging
		StringBuilder eventsSummary = new StringBuilder();
		for (CIEvent event : eventsSnapshot.getEvents()) {
			eventsSummary.append(event.getProject()).append(":").append(event.getBuildCiId()).append(":").append(event.getEventType());
			if (eventsSnapshot.getEvents().indexOf(event) < eventsSnapshot.getEvents().size() - 1) {
				eventsSummary.append(", ");
			}
		}
		String targetOctane = "UNKNOWN!?";
		OctaneConfiguration octaneConfig = pluginServices.getOctaneConfiguration();
		if (octaneConfig != null) {
			targetOctane = octaneConfig.getUrl() + ", SP: " + octaneConfig.getSharedSpace();
		}

		try {
			logger.info("sending [" + eventsSummary + "] event/s to [" + targetOctane + "]...");
			OctaneRequest request = createEventsRequest(eventsSnapshot);
			OctaneResponse response;
			while (failedRetries < MAX_SEND_RETRIES) {
				response = restService.obtainClient().execute(request);
				if (response.getStatus() == 200) {
					events.removeAll(eventsSnapshot.getEvents());
					logger.info("... done, left to send " + events.size() + " events");
					resetCounters();
					break;
				} else {
					failedRetries++;

					if (failedRetries < MAX_SEND_RETRIES) {
						doBreakableWait(pauseInterval *= 2);
					}
				}
			}
			if (failedRetries == MAX_SEND_RETRIES) {
				logger.error("max number of retries reached");
				result = false;
			}
		} catch (Exception e) {
			logger.error("failed to send snapshot of " + eventsSnapshot.getEvents().size() + " events: " + e.getMessage() + "; dropping them all", e);
			events.removeAll(eventsSnapshot.getEvents());
		}
		return result;
	}

	private OctaneRequest createEventsRequest(CIEventsList events) {
		Map<String, String> headers = new HashMap<>();
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType());
		return dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.PUT)
				.setUrl(pluginServices.getOctaneConfiguration().getUrl() +
						SHARED_SPACE_INTERNAL_API_PATH_PART + pluginServices.getOctaneConfiguration().getSharedSpace() +
						ANALYTICS_CI_PATH_PART + "events")
				.setHeaders(headers)
				.setBody(dtoFactory.dtoToJson(events));
	}

	private void doBreakableWait(long timeout) {
		logger.info("entering waiting period of " + timeout + "ms");
		long waitStart = new Date().getTime();
		synchronized (WAIT_MONITOR) {
			WAIT_MONITOR.released = false;
			while (!WAIT_MONITOR.released && new Date().getTime() - waitStart < timeout) {
				try {
					WAIT_MONITOR.wait(timeout);
				} catch (InterruptedException ie) {
					logger.info("waiting period was interrupted", ie);
				}
			}
			if (WAIT_MONITOR.released) {
				logger.info("pause finished on demand");
			} else {
				logger.info("pause finished timely");
			}
		}
	}

	private static final class WaitMonitor {
		volatile boolean released;
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
