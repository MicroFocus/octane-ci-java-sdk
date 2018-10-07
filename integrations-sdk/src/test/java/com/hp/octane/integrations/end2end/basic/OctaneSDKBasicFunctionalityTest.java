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

package com.hp.octane.integrations.end2end.basic;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.CIEventsList;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.testhelpers.GeneralTestUtils;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Octane SDK functional sanity test
 *
 * Basic functional sanity test, configuring 2 clients against 2 Octane shared spaces (servers)
 * Validating connectivity, events sent and tests / logs push (including pre-flights) - all this in isolated non-interfering fashion
 */

public class OctaneSDKBasicFunctionalityTest {
	private static final Logger logger = LogManager.getLogger(OctaneSDKBasicFunctionalityTest.class);
	private static DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test(timeout = 20000)
	public void testA() {
		Map<String, OctaneSPEndpointSimulator> simulators = null;
		Map<String, List<CIEventsList>> eventsCollectors = new LinkedHashMap<>();
		Map<String, List<TestsResult>> testResultsCollectors = new LinkedHashMap<>();
		Map<String, List<String>> logsCollectors = new LinkedHashMap<>();
		try {
			String spIdA = UUID.randomUUID().toString();
			String spIdB = UUID.randomUUID().toString();
			String clientAInstanceId = UUID.randomUUID().toString();
			String clientBInstanceId = UUID.randomUUID().toString();

			//  init 2 shared space endpoints simulators
			simulators = initSPEPSimulators(
					Stream.of(spIdA, spIdB).collect(Collectors.toSet()),
					eventsCollectors,
					testResultsCollectors,
					logsCollectors);

			//
			//  I
			//  add one client and verify it works okay
			//
			OctaneClient clientA = OctaneSDK.addClient(
					new OctaneConfigurationBasicFunctionalityTest(
							clientAInstanceId,
							OctaneSPEndpointSimulator.getSimulatorUrl(),
							spIdA,
							"client_SP_A",
							"secret_SP_A"
					),
					PluginServicesBasicFunctionalityTest.class);
			Assert.assertTrue(clientA.getConfigurationService().isConfigurationValid());
			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();

			//  validate events
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (eventsCollectors.containsKey(spIdA) && eventsCollectors.get(spIdA).stream().mapToInt(cil -> cil.getEvents().size()).sum() == 3) {
					eventsCollectors.get(spIdA).forEach(cil -> {
						Assert.assertNotNull(cil);
						Assert.assertNotNull(cil.getServer());
						Assert.assertEquals(clientAInstanceId, cil.getServer().getInstanceId());
						Assert.assertEquals("custom", cil.getServer().getType());
						Assert.assertEquals("1.1.1", cil.getServer().getVersion());
						Assert.assertEquals("http://localhost:9999", cil.getServer().getUrl());
					});
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//  validate tests
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (testResultsCollectors.containsKey(spIdA) && testResultsCollectors.get(spIdA).size() == 1) {
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//  validate logs
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (logsCollectors.containsKey(spIdA) && logsCollectors.get(spIdA).size() == 1) {
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//
			//  II
			//  add one more client and verify they are both works okay
			//
			OctaneClient clientB = OctaneSDK.addClient(
					new OctaneConfigurationBasicFunctionalityTest(
							clientBInstanceId,
							OctaneSPEndpointSimulator.getSimulatorUrl(),
							spIdB,
							"client_SP_B",
							"secret_SP_B"
					),
					PluginServicesBasicFunctionalityTest.class);
			Assert.assertTrue(clientA.getConfigurationService().isConfigurationValid());
			eventsCollectors.get(spIdA).clear();
			testResultsCollectors.get(spIdA).clear();
			logsCollectors.get(spIdA).clear();
			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();

			//  validate events
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (eventsCollectors.containsKey(spIdA) && eventsCollectors.get(spIdA).stream().mapToInt(cil -> cil.getEvents().size()).sum() == 3 &&
						eventsCollectors.containsKey(spIdB) && eventsCollectors.get(spIdA).stream().mapToInt(cil -> cil.getEvents().size()).sum() == 3) {
					//  client A
					eventsCollectors.get(spIdA).forEach(cil -> {
						Assert.assertNotNull(cil);
						Assert.assertNotNull(cil.getServer());
						Assert.assertEquals(clientAInstanceId, cil.getServer().getInstanceId());
						Assert.assertEquals("custom", cil.getServer().getType());
						Assert.assertEquals("1.1.1", cil.getServer().getVersion());
						Assert.assertEquals("http://localhost:9999", cil.getServer().getUrl());
					});

					//  client B
					eventsCollectors.get(spIdB).forEach(cil -> {
						Assert.assertNotNull(cil);
						Assert.assertNotNull(cil.getServer());
						Assert.assertEquals(clientBInstanceId, cil.getServer().getInstanceId());
						Assert.assertEquals("custom", cil.getServer().getType());
						Assert.assertEquals("1.1.1", cil.getServer().getVersion());
						Assert.assertEquals("http://localhost:9999", cil.getServer().getUrl());
					});

					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//  validate tests
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (testResultsCollectors.containsKey(spIdA) && testResultsCollectors.get(spIdA).size() == 1 &&
						testResultsCollectors.containsKey(spIdB) && testResultsCollectors.get(spIdB).size() == 1) {
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//  validate logs
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (logsCollectors.containsKey(spIdA) && logsCollectors.get(spIdA).size() == 1 &&
						logsCollectors.containsKey(spIdB) && logsCollectors.get(spIdB).size() == 1) {
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//
			//  III
			//  remove one client and verify it is shut indeed and the second continue to work okay
			//
			OctaneSDK.removeClient(clientA);
			eventsCollectors.get(spIdA).clear();
			eventsCollectors.get(spIdB).clear();
			testResultsCollectors.get(spIdA).clear();
			testResultsCollectors.get(spIdB).clear();
			logsCollectors.get(spIdA).clear();
			logsCollectors.get(spIdB).clear();
			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();

			//  validate events
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (eventsCollectors.containsKey(spIdB) && eventsCollectors.get(spIdB).stream().mapToInt(cil -> cil.getEvents().size()).sum() == 3) {
					Assert.assertTrue(eventsCollectors.get(spIdA).isEmpty());
					eventsCollectors.get(spIdB).forEach(cil -> {
						Assert.assertNotNull(cil);
						Assert.assertNotNull(cil.getServer());
						Assert.assertEquals(clientBInstanceId, cil.getServer().getInstanceId());
						Assert.assertEquals("custom", cil.getServer().getType());
						Assert.assertEquals("1.1.1", cil.getServer().getVersion());
						Assert.assertEquals("http://localhost:9999", cil.getServer().getUrl());
					});
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//  validate tests
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (testResultsCollectors.containsKey(spIdB) && testResultsCollectors.get(spIdB).size() == 1) {
					Assert.assertTrue(testResultsCollectors.get(spIdA).isEmpty());
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//  validate logs
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (logsCollectors.containsKey(spIdB) && logsCollectors.get(spIdB).size() == 1) {
					Assert.assertTrue(logsCollectors.get(spIdA).isEmpty());
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//
			//  IV
			//  remove second client and ensure no interactions anymore
			//
			OctaneSDK.removeClient(clientB);
			eventsCollectors.get(spIdB).clear();
			testResultsCollectors.get(spIdB).clear();

			//  events, tests, logs
			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();

			CIPluginSDKUtils.doWait(4000);

			Assert.assertTrue(eventsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(eventsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(testResultsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(testResultsCollectors.get(spIdB).isEmpty());

		} finally {
			//  remove simulators
			if (simulators != null) removeSPEPSimulators(simulators.values());
		}

	}

	private Map<String, OctaneSPEndpointSimulator> initSPEPSimulators(
			Set<String> spIDs,
			Map<String, List<CIEventsList>> eventsCollectors,
			Map<String, List<TestsResult>> testResultsCollectors,
			Map<String, List<String>> logsCollectors) {
		Map<String, OctaneSPEndpointSimulator> result = new LinkedHashMap<>();

		for (String spID : spIDs) {
			OctaneSPEndpointSimulator simulator = OctaneSPEndpointSimulator.addInstance(spID);

			//  events API
			simulator.installApiHandler(HttpMethod.PUT, "^.*events$", request -> {
				try {
					String rawEventsBody = CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(request.getInputStream()));
					CIEventsList eventsList = dtoFactory.dtoFromJson(rawEventsBody, CIEventsList.class);
					eventsCollectors
							.computeIfAbsent(spID, sp -> new LinkedList<>())
							.add(eventsList);
					request.getResponse().setStatus(HttpStatus.SC_OK);
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
			});

			//  test results preflight API
			simulator.installApiHandler(HttpMethod.GET, "^.*tests-result-preflight$", request -> {
				try {
					request.getResponse().setStatus(HttpStatus.SC_OK);
					request.getResponse().getWriter().write("true");
					request.getResponse().getWriter().flush();
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
			});

			//  test results push API
			simulator.installApiHandler(HttpMethod.POST, "^.*test-results$", request -> {
				try {
					String rawTestResultBody = CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(request.getInputStream()));
					TestsResult testsResult = dtoFactory.dtoFromXml(rawTestResultBody, TestsResult.class);
					testResultsCollectors
							.computeIfAbsent(spID, sp -> new LinkedList<>())
							.add(testsResult);
					request.getResponse().setStatus(HttpStatus.SC_ACCEPTED);
					request.getResponse().getWriter().write("{\"status\": \"queued\"}");
					request.getResponse().getWriter().flush();
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
			});

			//  logs preflight API
			simulator.installApiHandler(HttpMethod.GET, "^.*workspaceId$", request -> {
				try {
					request.getResponse().setStatus(HttpStatus.SC_OK);
					request.getResponse().getWriter().write("[\"1001\"]");
					request.getResponse().getWriter().flush();
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
			});

			//  logs push API
			simulator.installApiHandler(HttpMethod.POST, "^.*logs$", request -> {
				try {
					String rawLogBody = CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(request.getInputStream()));
					logsCollectors
							.computeIfAbsent(spID, sp -> new LinkedList<>())
							.add(rawLogBody);
					request.getResponse().setStatus(HttpStatus.SC_OK);
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
			});

			result.put(spID, simulator);
		}

		return result;
	}

	private void simulateEventsCycleAllClients() {
		OctaneSDK.getClients().forEach(octaneClient -> {
			try {
				CIEvent event = dtoFactory.newDTO(CIEvent.class)
						.setEventType(CIEventType.STARTED)
						.setProject("job-a")
						.setBuildCiId("1")
						.setStartTime(System.currentTimeMillis());
				octaneClient.getEventsService().publishEvent(event);
				event = dtoFactory.newDTO(CIEvent.class)
						.setEventType(CIEventType.SCM)
						.setProject("job-a")
						.setBuildCiId("1")
						.setScmData(dtoFactory.newDTO(SCMData.class)
								.setRepository(dtoFactory.newDTO(SCMRepository.class)
										.setType(SCMType.GIT)
										.setUrl("http://github.org")
										.setBranch("master")
								)
								.setBuiltRevId(UUID.randomUUID().toString())
						);
				octaneClient.getEventsService().publishEvent(event);
				event = dtoFactory.newDTO(CIEvent.class)
						.setEventType(CIEventType.FINISHED)
						.setProject("job-a")
						.setBuildCiId("1")
						.setDuration(3000L);
				octaneClient.getEventsService().publishEvent(event);
			} catch (Exception e) {
				logger.error("failed to dispatch events to " + octaneClient, e);
			}
		});
	}

	private void simulatePushTestResultsCycleAllClients() {
		OctaneSDK.getClients().forEach(octaneClient -> octaneClient.getTestsService().enqueuePushTestsResult("job-a", "1"));
	}

	private void simulatePushLogsCycleAllClients() {
		OctaneSDK.getClients().forEach(octaneClient -> octaneClient.getLogsService().enqueuePushBuildLog("job-a", "1"));
	}

	private void removeSPEPSimulators(Collection<OctaneSPEndpointSimulator> simulators) {
		for (OctaneSPEndpointSimulator simulator : simulators) {
			try {
				OctaneSPEndpointSimulator.removeInstance(simulator.getSharedSpaceId());
			} catch (Exception e) {
				logger.error("failed to remove Octane SharedSpace Simulator", e);
			}
		}
	}
}
