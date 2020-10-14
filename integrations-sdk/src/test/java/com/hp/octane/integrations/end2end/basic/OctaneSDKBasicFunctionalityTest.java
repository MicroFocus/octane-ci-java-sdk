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
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.hp.octane.integrations.dto.coverage.CoverageReportType;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.CIEventsList;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMType;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.testhelpers.GeneralTestUtils;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
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

	@Test(timeout = 60000)
	public void testE2EFunctional() throws ExecutionException, InterruptedException {
		Map<String, OctaneSPEndpointSimulator> simulators = null;
		Map<String, List<CIEventsList>> eventsCollectors = new LinkedHashMap<>();
		Map<String, List<TestsResult>> testResultsCollectors = new LinkedHashMap<>();
		Map<String, List<String>> logsCollectors = new LinkedHashMap<>();
		Map<String, List<String>> coverageCollectors = new LinkedHashMap<>();
		OctaneClient clientA = null;
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
					logsCollectors,
					coverageCollectors);


			//
			//  I
			//  add one client and verify it works okay
			//
			System.out.println("Scenario 1 - add one client and verify it works okay");
			clientA = OctaneSDK.addClient(
					new OctaneConfigurationBasicFunctionalityTest(
							clientAInstanceId,
							OctaneSPEndpointSimulator.getSimulatorUrl(),
							spIdA,
							"client_SP_A",
							"secret_SP_A"
					),
					PluginServicesBasicFunctionalityTest.class);
			clientA.getConfigurationService().getOctaneConnectivityStatus();
			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();
			simulatePushCoverageAllClients();

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

			//  validate coverage
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (coverageCollectors.containsKey(spIdA) && coverageCollectors.get(spIdA).size() == 2) {
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
			System.out.println("Scenario 2 - add one more client and verify they are both works okay");
			OctaneClient clientB = OctaneSDK.addClient(
					new OctaneConfigurationBasicFunctionalityTest(
							clientBInstanceId,
							OctaneSPEndpointSimulator.getSimulatorUrl(),
							spIdB,
							"client_SP_B",
							"secret_SP_B"
					),
					PluginServicesBasicFunctionalityTest.class);
			clientA.getConfigurationService().getOctaneConnectivityStatus();
			eventsCollectors.get(spIdA).clear();
			testResultsCollectors.get(spIdA).clear();
			logsCollectors.get(spIdA).clear();
			coverageCollectors.get(spIdA).clear();
			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();
			simulatePushCoverageAllClients();

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

			//  validate coverages
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (coverageCollectors.containsKey(spIdA) && coverageCollectors.get(spIdA).size() == 2 &&
						coverageCollectors.containsKey(spIdB) && coverageCollectors.get(spIdB).size() == 2) {
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
			System.out.println("Scenario 3 - remove one client and verify it is shut indeed and the second continue to work okay");
			OctaneSDK.removeClient(clientA);
			eventsCollectors.get(spIdA).clear();
			eventsCollectors.get(spIdB).clear();
			testResultsCollectors.get(spIdA).clear();
			testResultsCollectors.get(spIdB).clear();
			logsCollectors.get(spIdA).clear();
			logsCollectors.get(spIdB).clear();
			coverageCollectors.get(spIdA).clear();
			coverageCollectors.get(spIdB).clear();
			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();
			simulatePushCoverageAllClients();

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

			//  validate coverages
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (coverageCollectors.containsKey(spIdB) && coverageCollectors.get(spIdB).size() == 2) {
					Assert.assertTrue(coverageCollectors.get(spIdA).isEmpty());
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
			System.out.println("Scenario 4 - remove second client and ensure no interactions anymore");
			OctaneSDK.removeClient(clientB);
			eventsCollectors.get(spIdB).clear();
			testResultsCollectors.get(spIdB).clear();
			logsCollectors.get(spIdB).clear();
			coverageCollectors.get(spIdB).clear();

			//  events, tests, logs
			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();
			simulatePushCoverageAllClients();

			CIPluginSDKUtils.doWait(4000);

			Assert.assertTrue(eventsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(eventsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(testResultsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(testResultsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(logsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(logsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(coverageCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(coverageCollectors.get(spIdB).isEmpty());

			//
			//  V
			//  add clientA in with deactivated Mode
			//
			System.out.println("Scenario 5 - add clientA in with deactivated Mode");
			clientA = OctaneSDK.addClient(
					new OctaneConfigurationBasicFunctionalityTest(
							clientAInstanceId,
							OctaneSPEndpointSimulator.getSimulatorUrl(),
							spIdA,
							"client_SP_A",
							"secret_SP_A"
					),
					PluginServicesBasicFunctionalityTest.class);
			clientA.getConfigurationService().getConfiguration().setSuspended(true);
			clientA.getConfigurationService().getOctaneConnectivityStatus();
			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();
			simulatePushCoverageAllClients();

			CIPluginSDKUtils.doWait(4000);

			Assert.assertTrue(eventsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(eventsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(testResultsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(testResultsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(logsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(logsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(coverageCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(coverageCollectors.get(spIdB).isEmpty());
			OctaneSDK.removeClient(clientA);

			//
			//  6
			//  add client with parameter OCTANE_ROOTS_CACHE_ALLOWED=true with no pipeline root
			//
			System.out.println("Scenario 6 - add client with parameter OCTANE_ROOTS_CACHE_ALLOWED=true with no pipeline root");
			OctaneConfiguration tempConf = new OctaneConfigurationBasicFunctionalityTest(
					clientAInstanceId,
					OctaneSPEndpointSimulator.getSimulatorUrl(),
					spIdA,
					"client_SP_A",
					"secret_SP_A");

			ConfigurationParameterFactory.addParameter(tempConf, "OCTANE_ROOTS_CACHE_ALLOWED", "true");
			clientA = OctaneSDK.addClient(tempConf, PluginServicesBasicFunctionalityTest.class);
			clientA.getConfigurationService().getOctaneConnectivityStatus();

			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();
			simulatePushCoverageAllClients();

			CIPluginSDKUtils.doWait(4000);

			Assert.assertTrue(eventsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(eventsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(testResultsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(testResultsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(logsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(logsCollectors.get(spIdB).isEmpty());
			Assert.assertTrue(coverageCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(coverageCollectors.get(spIdB).isEmpty());


			//
			//  7
			//  add client with parameter OCTANE_ROOTS_CACHE_ALLOWED=true with one root
			//
			System.out.println("Scenario 7 - add client with parameter OCTANE_ROOTS_CACHE_ALLOWED=true ith one root");
			clientA.getConfigurationService().addToOctaneRootsCache("job-a");

			simulateEventsCycleAllClients();
			simulatePushTestResultsCycleAllClients();
			simulatePushLogsCycleAllClients();
			simulatePushCoverageAllClients();

			CIPluginSDKUtils.doWait(4000);

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

			//  validate coverage
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (coverageCollectors.containsKey(spIdA) && coverageCollectors.get(spIdA).size() == 2) {
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

		} finally {
			//  remove clients
			OctaneSDK.getClients().forEach(OctaneSDK::removeClient);

			//  remove simulators
			if (simulators != null) removeSPEPSimulators(simulators.values());
		}
	}

	private Map<String, OctaneSPEndpointSimulator> initSPEPSimulators(
			Set<String> spIDs,
			Map<String, List<CIEventsList>> eventsCollectors,
			Map<String, List<TestsResult>> testResultsCollectors,
			Map<String, List<String>> logsCollectors,
			Map<String, List<String>> coverageCollectors) {
		Map<String, OctaneSPEndpointSimulator> result = new LinkedHashMap<>();

		for (String spID : spIDs) {
			OctaneSPEndpointSimulator simulator = OctaneSPEndpointSimulator.addInstance(spID);
			simulator.setOctaneVersion("15.1.8");//for octane roots
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
					//  [YG] below validations are done to ensure NEW API (via query params) aligned with an OLD API (data within XML)
					//  [YG] in the future we'll remove OLD API and this validation should be done differently
					request.mergeQueryParameters("", request.getQueryString(), false);
					Assert.assertEquals(request.getQueryParameters().getString("instance-id"), testsResult.getBuildContext().getServerId());
					Assert.assertEquals(request.getQueryParameters().getString("job-ci-id"), testsResult.getBuildContext().getJobId());
					Assert.assertEquals(request.getQueryParameters().getString("build-ci-id"), testsResult.getBuildContext().getBuildId());
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

			//  logs/coverage preflight API
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

			//  coverage preflight API
			//  no need to configure, since it's the same API as for logs, see above

			//  coverage push API
			simulator.installApiHandler(HttpMethod.PUT, "^.*coverage$", request -> {
				try {
					String rawCoverageBody = CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(request.getInputStream()));
					coverageCollectors
							.computeIfAbsent(spID, sp -> new LinkedList<>())
							.add(rawCoverageBody);
					request.getResponse().setStatus(HttpStatus.SC_OK);
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
			});

			//  get roots
			simulator.installApiHandler(HttpMethod.GET, "^.*pipeline-roots$", request -> {
				try {
					request.getResponse().setStatus(HttpStatus.SC_OK);
					request.getResponse().getWriter().write("[]");
					request.getResponse().getWriter().flush();
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
						.setCauses(Collections.singletonList(dtoFactory.newDTO(CIEventCause.class).setType(CIEventCauseType.USER) ))
						.setStartTime(System.currentTimeMillis());
				octaneClient.getEventsService().publishEvent(event);
				event = dtoFactory.newDTO(CIEvent.class)
						.setEventType(CIEventType.SCM)
						.setProject("job-a")
						.setCauses(Collections.singletonList(dtoFactory.newDTO(CIEventCause.class).setType(CIEventCauseType.USER) ))
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
						.setCauses(Collections.singletonList(dtoFactory.newDTO(CIEventCause.class).setType(CIEventCauseType.USER) ))
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
		OctaneSDK.getClients().forEach(octaneClient -> octaneClient.getTestsService().enqueuePushTestsResult("job-a", "1", "job-a"));
	}

	private void simulatePushLogsCycleAllClients() {
		OctaneSDK.getClients().forEach(octaneClient -> octaneClient.getLogsService().enqueuePushBuildLog("job-a", "1", "job-a"));
	}

	private void simulatePushCoverageAllClients() {
		OctaneSDK.getClients().forEach(octaneClient -> octaneClient.getCoverageService().enqueuePushCoverage("job-a", "1", CoverageReportType.JACOCOXML, "jacoco-coverage.xml", "job-a"));
		OctaneSDK.getClients().forEach(octaneClient -> octaneClient.getCoverageService().enqueuePushCoverage("job-a", "1", CoverageReportType.LCOV, "coverage-report.lcov", "job-a"));
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
