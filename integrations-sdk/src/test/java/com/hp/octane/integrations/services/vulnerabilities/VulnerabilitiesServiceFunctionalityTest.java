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

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEventsList;
import com.hp.octane.integrations.dto.tests.TestsResult;
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

public class VulnerabilitiesServiceFunctionalityTest {
	private static final Logger logger = LogManager.getLogger(VulnerabilitiesServiceFunctionalityTest.class);
	private static DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test(timeout = 10000)
	public void testA() {
		Map<String, OctaneSPEndpointSimulator> simulators = null;

		try {
			String spIdA = UUID.randomUUID().toString();
			String spIdB = UUID.randomUUID().toString();
			String clientAInstanceId = UUID.randomUUID().toString();
			String clientBInstanceId = UUID.randomUUID().toString();

			//  init 2 shared space endpoints simulators
//			simulators = initSPEPSimulators(
//					Stream.of(spIdA, spIdB).collect(Collectors.toSet()),
//					eventsCollectors,
//					testResultsCollectors,
//					logsCollectors,
//					coverageCollectors);

			//
			//  I
			//  add one client and verify it works okay
			//
			OctaneConfiguration configA = new OctaneConfiguration(clientAInstanceId, OctaneSPEndpointSimulator.getSimulatorUrl(), spIdA);
			OctaneClient clientA = OctaneSDK.addClient(configA, VulnerabilitiesServicePluginServicesTest.class);
			VulnerabilitiesService vulnerabilitiesService = clientA.getVulnerabilitiesService();

			//
			//  II
			//  add one more client and verify they are both works okay
			//
			OctaneConfiguration configB = new OctaneConfiguration(clientBInstanceId, OctaneSPEndpointSimulator.getSimulatorUrl(), spIdB);
			OctaneClient clientB = OctaneSDK.addClient(configB, VulnerabilitiesServicePluginServicesTest.class);

			//
			//  III
			//  remove one client and verify it is shut indeed and the second continue to work okay
			//
			OctaneSDK.removeClient(clientA);

			//
			//  IV
			//  remove second client and ensure no interactions anymore
			//
			OctaneSDK.removeClient(clientB);

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

			result.put(spID, simulator);
		}

		return result;
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
