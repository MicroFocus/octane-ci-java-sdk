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
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Octane SDK functional sanity test
 *
 * Basic functional sanity test, configuring 2 clients against 2 Octane shared spaces (servers)
 * Validating connectivity, events sent and tests / logs push (including pre-flights) - all this in isolated non-interfering fashion
 */

public class OctaneSDKBasicFunctionalityTest {
	private static final Logger logger = LogManager.getLogger(OctaneSDK.class);
	private static DTOFactory dtoFactory = DTOFactory.getInstance();

	private static CompletableFuture<Boolean> resolvedA = new CompletableFuture<>();
	private static CompletableFuture<Boolean> resolvedB = new CompletableFuture<>();

	@Test(timeout = 20000)
	public void testA() throws Exception {
		Map<String, OctaneSPEndpointSimulator> simulators = null;
		try {

			String spIdA = UUID.randomUUID().toString();
			String spIdB = UUID.randomUUID().toString();

			//  init 2 shared space endpoints simulators
			simulators = initSPEPSimulators(Stream.of(spIdA, spIdB).collect(Collectors.toSet()));

			//  add one client and verify it works okay
			OctaneClient clientA = OctaneSDK.addClient(new PluginServicesBFA(spIdA));
			//  TODO: verification

			//  add one more client and verify they are both works okay
			OctaneClient clientB = OctaneSDK.addClient(new PluginServicesBFB(spIdB));
			//  TODO: verification

			CompletableFuture.allOf(resolvedA).get();

			//  remove one client and verify it is shut indeed and the second continue to work okay
			OctaneSDK.removeClient(clientA);
			//  TODO: verification

			//  remove second client and ensure no interactions anymore
			OctaneSDK.removeClient(clientB);
			//  TODO: verification

		} finally {
			//  remove simulators
			if (simulators != null) removeSPEPSimulators(simulators.values());
		}

	}

	private Map<String, OctaneSPEndpointSimulator> initSPEPSimulators(Set<String> spIDs) {
		Map<String, OctaneSPEndpointSimulator> result = new LinkedHashMap<>();

		for (String spID : spIDs) {
			OctaneSPEndpointSimulator simulator = OctaneSPEndpointSimulator.addInstance(spID);
			simulator.installApiHandler("^.*tasks$", request -> {
				System.out.println("get tasks A...");
				try {
					Thread.sleep(3000);
				} catch (InterruptedException ie) {

				}
				resolvedA.complete(true);
				request.getResponse().setStatus(HttpStatus.SC_NO_CONTENT);
				request.setHandled(true);
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
