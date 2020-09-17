package com.hp.octane.integrations.services.tasking;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneConfigurationIntern;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.OctaneResultAbridged;
import com.hp.octane.integrations.dto.connectivity.OctaneTaskAbridged;
import com.hp.octane.integrations.testhelpers.GeneralTestUtils;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;

public class TaskingServiceE2ETests {
	private static final Logger logger = LogManager.getLogger(TaskingServiceE2ETests.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String APIPrefix = "/nga/api/v1";

	private static String inId = UUID.randomUUID().toString();
	private static String sspId = UUID.randomUUID().toString();
	private static OctaneClient client;
	private static final BlockingQueue<OctaneTaskAbridged> tasks = new ArrayBlockingQueue<>(10);
	private static final Map<String, OctaneResultAbridged> results = new HashMap<>();

	@BeforeClass
	public static void setupEnvironment() {
		//  setup Octane simulator
		OctaneSPEndpointSimulator octaneSPEndpointSimulator = setupOctaneEPSimulator(sspId);
		Assert.assertNotNull(octaneSPEndpointSimulator);

		//  setup Octane client
		OctaneConfiguration configuration = new OctaneConfigurationIntern(inId, OctaneSPEndpointSimulator.getSimulatorUrl(), sspId);
		client = OctaneSDK.addClient(configuration, TaskingTestPluginServicesTest.class);
	}

	@AfterClass
	public static void cleanupEnvironment() {
		OctaneSDK.removeClient(client);
		OctaneSPEndpointSimulator.removeInstance(sspId);
	}

	@Test
	public void taskingE2ETest() {
		OctaneTaskAbridged task;

		//  push task 1 - status
		String statusTaskId = UUID.randomUUID().toString();
		task = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(statusTaskId)
				.setServiceId(inId)
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/status");
		tasks.add(task);

		//  push task 2 - jobs list
		String jobsTaskId = UUID.randomUUID().toString();
		task = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(jobsTaskId)
				.setServiceId(inId)
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/jobs");
		tasks.add(task);

		//  wait for at least 2 tasks to arrive in results map
		GeneralTestUtils.waitAtMostFor(10000, () -> {
			if (results.size() < 3) {
				return null;
			} else {
				return true;
			}
		});

		//  verify status task cycle
		Assert.assertTrue(results.containsKey(statusTaskId));
		OctaneResultAbridged statusResult = results.get(statusTaskId);
		Assert.assertNotNull(statusResult);
		Assert.assertEquals(inId, statusResult.getServiceId());
		Assert.assertEquals(HttpStatus.SC_OK, statusResult.getStatus());

		//  verify jobs task cycle
		Assert.assertTrue(results.containsKey(jobsTaskId));
		OctaneResultAbridged jobsResult = results.get(jobsTaskId);
		Assert.assertNotNull(jobsResult);
		Assert.assertEquals(inId, jobsResult.getServiceId());
		Assert.assertEquals(HttpStatus.SC_OK, jobsResult.getStatus());
	}

	private static OctaneSPEndpointSimulator setupOctaneEPSimulator(String sspId) {
		OctaneSPEndpointSimulator result = OctaneSPEndpointSimulator.addInstance(sspId);

		//  remove default NOOP GET tasks API handler
		result.removeApiHandler(HttpMethod.GET, "^.*tasks$");

		//  install GET tasks API handler
		result.installApiHandler(HttpMethod.GET, "^.*tasks$", request -> {
			try {
				Response response = request.getResponse();
				OctaneTaskAbridged task = tasks.poll(500, TimeUnit.MILLISECONDS);
				if (task != null) {
					logger.info("got task to dispatch to CI Server - " + task.getUrl() + " - " + task.getId() + "...");
					response.setStatus(HttpStatus.SC_OK);
					response.addHeader(CONTENT_TYPE, "application/json");
					response.getWriter().write(dtoFactory.dtoCollectionToJson(Collections.singletonList(task)));
					response.flushBuffer();
					logger.info("... task dispatched");
				} else {
					results.put("timeout_flow_verification_part", null);
					response.setStatus(HttpStatus.SC_REQUEST_TIMEOUT);
				}
			} catch (Exception e) {
				logger.error("failed during simulation of Octane EP - GET tasks", e);
			}
		});

		//  install PUT results API handler
		result.installApiHandler(HttpMethod.PUT, "^.*result$", request -> {
			try {
				String rawBody = CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(request.getInputStream()));
				OctaneResultAbridged taskResult = dtoFactory.dtoFromJson(rawBody, OctaneResultAbridged.class);
				logger.info("received and parsed result for task " + taskResult.getId());
				Assert.assertNotNull(taskResult);
				Assert.assertNotNull(taskResult.getId());
				results.put(taskResult.getId(), taskResult);
			} catch (Exception e) {
				logger.error("failed during simulation of Octane EP - PUT results", e);
			}
		});
		return result;
	}
}
