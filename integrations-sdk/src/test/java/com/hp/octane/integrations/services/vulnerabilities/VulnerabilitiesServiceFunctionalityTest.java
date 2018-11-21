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
import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
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

public class VulnerabilitiesServiceFunctionalityTest {
	private static final Logger logger = LogManager.getLogger(VulnerabilitiesServiceFunctionalityTest.class);

	@Test(timeout = 20000)
	public void testVulnerabilitiesFunctional() {
		Map<String, OctaneSPEndpointSimulator> simulators = null;

		try {
			String spIdA = UUID.randomUUID().toString();
			String spIdB = UUID.randomUUID().toString();
			String clientAInstanceId = UUID.randomUUID().toString();
			String clientBInstanceId = UUID.randomUUID().toString();
			Map<String, List<String>> preflightRequestCollectors = new LinkedHashMap<>();
			Map<String, List<String>> pushVulnerabilitiesCollectors = new LinkedHashMap<>();

			//  init 2 shared space endpoints simulators
			simulators = initSPEPSimulators(
					Stream.of(spIdA, spIdB).collect(Collectors.toSet()),
					preflightRequestCollectors,
					pushVulnerabilitiesCollectors);

			//
			//  I
			//  add one client and verify it works okay
			//
			OctaneConfiguration configA = new OctaneConfiguration(clientAInstanceId, OctaneSPEndpointSimulator.getSimulatorUrl(), spIdA);
			OctaneClient clientA = OctaneSDK.addClient(configA, VulnerabilitiesServicePluginServicesTest.class);
			VulnerabilitiesService vulnerabilitiesServiceA = clientA.getVulnerabilitiesService();
			vulnerabilitiesServiceA.enqueueRetrieveAndPushVulnerabilities("null-job", "null-build", System.currentTimeMillis(), 1);
			CIPluginSDKUtils.doWait(3000);
			Assert.assertFalse(preflightRequestCollectors.containsKey(spIdA));
			Assert.assertFalse(preflightRequestCollectors.containsKey(spIdB));

			//
			//  II
			//  add one more client and verify they are both works okay
			//
			OctaneConfiguration configB = new OctaneConfiguration(clientBInstanceId, OctaneSPEndpointSimulator.getSimulatorUrl(), spIdB);
			OctaneClient clientB = OctaneSDK.addClient(configB, VulnerabilitiesServicePluginServicesTest.class);
			VulnerabilitiesService vulnerabilitiesServiceB = clientB.getVulnerabilitiesService();
			vulnerabilitiesServiceB.enqueueRetrieveAndPushVulnerabilities("job-preflight-true", "1", System.currentTimeMillis(), 1);
			vulnerabilitiesServiceB.enqueueRetrieveAndPushVulnerabilities("job-preflight-false", "1", System.currentTimeMillis(), 1);
			List<String> preflightRequests = GeneralTestUtils.waitAtMostFor(12000, () -> preflightRequestCollectors.get(spIdB));
			Assert.assertFalse(preflightRequestCollectors.containsKey(spIdB));
			Assert.assertEquals(2, preflightRequests.size());
			Assert.assertEquals(clientBInstanceId + "|job-preflight-true|1", preflightRequests.get(0));
			Assert.assertEquals(clientBInstanceId + "|job-preflight-false|1", preflightRequests.get(1));

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
			Map<String, List<String>> preflightRequestsCollectors,
			Map<String, List<String>> pushRequestCollectors) {
		Map<String, OctaneSPEndpointSimulator> result = new LinkedHashMap<>();

		for (String spID : spIDs) {
			OctaneSPEndpointSimulator simulator = OctaneSPEndpointSimulator.addInstance(spID);

			//  vulnerabilities preflight API
			simulator.installApiHandler(HttpMethod.GET, "^.*/vulnerabilities/preflight$", request -> {
				try {
					//  retrieve query parameters
					request.mergeQueryParameters("", request.getQueryString(), false);
					preflightRequestsCollectors
							.computeIfAbsent(spID, sid -> new LinkedList<>())
							.add(request.getQueryParameters().getString("instance-id") + "|" +
									request.getQueryParameters().getString("job-ci-id") + "|" +
									request.getQueryParameters().getString("build-ci-id"));
					request.getResponse().setStatus(HttpStatus.SC_OK);
					request.getResponse().getWriter().write(request.getQueryParameters().getString("job-ci-id").contains("true") ? "true" : "false");
					request.getResponse().getWriter().flush();
				} catch (IOException ioe) {
					throw new OctaneSDKGeneralException("failed to write response", ioe);
				}
			});

			//  vulnerabilities push API
			simulator.installApiHandler(HttpMethod.POST, "^.*/vulnerabilities$", request -> {
				try {
					String rawVulnerabilitiesBody = CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(request.getInputStream()));
					pushRequestCollectors
							.computeIfAbsent(spID, sid -> new LinkedList<>())
							.add(rawVulnerabilitiesBody);
					request.getResponse().setStatus(HttpStatus.SC_ACCEPTED);
					request.getResponse().getWriter().write("{\"status\": \"queued\"}");
					request.getResponse().getWriter().flush();
				} catch (IOException ioe) {
					throw new OctaneSDKGeneralException("failed to write response", ioe);
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
