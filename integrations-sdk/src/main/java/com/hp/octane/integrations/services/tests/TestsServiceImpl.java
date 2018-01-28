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

package com.hp.octane.integrations.services.tests;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.RestClient;
import com.hp.octane.integrations.api.RestService;
import com.hp.octane.integrations.api.TestsService;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneRequest;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.hp.octane.integrations.api.RestService.CONTENT_ENCODING_HEADER;
import static com.hp.octane.integrations.api.RestService.CONTENT_TYPE_HEADER;
import static com.hp.octane.integrations.api.RestService.GZIP_ENCODING;

/**
 * Default implementation of tests service
 */

public final class TestsServiceImpl extends OctaneSDK.SDKServiceBase implements TestsService {
	private static final Logger logger = LogManager.getLogger(TestsServiceImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	private final Object INIT_LOCKER = new Object();
	private final CIPluginServices pluginServices;
	private final RestService restService;

	private static List<TestsResultQueueEntry> buildList = Collections.synchronizedList(new LinkedList<TestsResultQueueEntry>());
	private int SERVICE_UNAVAILABLE_BREATHE_INTERVAL = 60000;
	private int LIST_EMPTY_INTERVAL = 3000;
	private Thread worker;

	public TestsServiceImpl(Object configurator, CIPluginServices pluginServices, RestService restService) {
		super(configurator);

		if (pluginServices == null) {
			throw new IllegalArgumentException("plugin services MUST NOT be null");
		}
		if (restService == null) {
			throw new IllegalArgumentException("rest service MUST NOT be null");
		}

		this.pluginServices = pluginServices;
		this.restService = restService;
		activate();
	}

	@Override
	public boolean isTestsResultRelevant(String serverCiId, String jobCiId) {
		return true;
	}

	public OctaneResponse pushTestsResult(TestsResult testsResult) throws IOException {
		if (testsResult == null) {
			throw new IllegalArgumentException("tests result MUST NOT be null");
		}

		RestClient restClientImpl = restService.obtainClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_XML.getMimeType());
		headers.put(CONTENT_ENCODING_HEADER, GZIP_ENCODING);
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.POST)
				.setUrl(pluginServices.getOctaneConfiguration().getUrl() + "/internal-api/shared_spaces/" +
						pluginServices.getOctaneConfiguration().getSharedSpace() + "/analytics/ci/test-results?skip-errors=false")
				.setHeaders(headers)
				.setBody(dtoFactory.dtoToXml(testsResult));
		OctaneResponse response = restClientImpl.execute(request);
		logger.info("tests result pushed; status: " + response.getStatus() + ", response: " + response.getBody());
		return response;
	}

	@Override
	public OctaneResponse pushTestsResult(InputStream testsResult) throws IOException {
		if (testsResult == null) {
			throw new IllegalArgumentException("tests result MUST NOT be null");
		}

		RestClient restClientImpl = restService.obtainClient();
		Map<String, String> headers = new HashMap<>();
		headers.put(CONTENT_TYPE_HEADER, ContentType.APPLICATION_XML.getMimeType());
		headers.put(CONTENT_ENCODING_HEADER, GZIP_ENCODING);
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(HttpMethod.POST)
				.setUrl(pluginServices.getOctaneConfiguration().getUrl() + "/internal-api/shared_spaces/" +
						pluginServices.getOctaneConfiguration().getSharedSpace() + "/analytics/ci/test-results?skip-errors=false")
				.setHeaders(headers)
				.setBodyAsStream(testsResult);
		OctaneResponse response = restClientImpl.execute(request);
		logger.info("tests result pushed; status: " + response.getStatus() + ", response: " + response.getBody());
		return response;
	}

	@Override
	public void enqueuePushTestsResult(String jobCiId, String buildCiId) {
		buildList.add(new TestsResultQueueEntry(jobCiId, buildCiId));
	}

	//  TODO: move thread to thread factory
	//  TODO: implement retries counter per item and strategy of discard
	//  TODO: distinct between the item's problem, server problem and env problem and retry strategy accordingly
	private void activate() {
		if (worker == null || !worker.isAlive()) {
			synchronized (INIT_LOCKER) {
				if (worker == null || !worker.isAlive()) {
					worker = new Thread(new Runnable() {
						public void run() {
							while (true) {
								if (!buildList.isEmpty()) {
									try {
										TestsResultQueueEntry testsResultQueueEntry = buildList.get(0);
										TestsResult testsResult = pluginServices.getTestsResult(testsResultQueueEntry.jobId, testsResultQueueEntry.buildId);
										OctaneResponse response = pushTestsResult(testsResult);
										if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
											logger.info("tests result push SUCCEED");
											buildList.remove(0);
										} else if (response.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
											logger.info("tests result push FAILED, service unavailable; retrying after a breathe...");
											breathe(SERVICE_UNAVAILABLE_BREATHE_INTERVAL);
										} else {
											//  case of any other fatal error
											logger.error("tests result push FAILED, status " + response.getStatus() + "; dropping this item from the queue");
											buildList.remove(0);
										}
									} catch (Throwable t) {
										logger.error("tests result push failed; will retry after " + SERVICE_UNAVAILABLE_BREATHE_INTERVAL + "ms", t);
										breathe(SERVICE_UNAVAILABLE_BREATHE_INTERVAL);
									}
								} else {
									breathe(LIST_EMPTY_INTERVAL);
								}
							}
						}
					});
					worker.setDaemon(true);
					worker.setName("TestPushWorker");
					worker.start();
				}
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

	private static final class TestsResultQueueEntry {
		private final String jobId;
		private final String buildId;

		private TestsResultQueueEntry(String jobId, String buildId) {
			this.jobId = jobId;
			this.buildId = buildId;
		}
	}
}
