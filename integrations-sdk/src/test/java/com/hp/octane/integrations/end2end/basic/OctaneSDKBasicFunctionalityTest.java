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
import com.hp.octane.integrations.testhelpers.GeneralTestUtils;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
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
	private static final Logger logger = LogManager.getLogger(OctaneSDK.class);
	private static DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test(timeout = 20000)
	public void testA() {
		Map<String, OctaneSPEndpointSimulator> simulators = null;
		Map<String, List<CIEvent>> eventsCollectors = new LinkedHashMap<>();
		try {
			String spIdA = UUID.randomUUID().toString();
			String spIdB = UUID.randomUUID().toString();

			//  init 2 shared space endpoints simulators
			simulators = initSPEPSimulators(
					Stream.of(spIdA, spIdB).collect(Collectors.toSet()),
					eventsCollectors);

			//  add one client and verify it works okay
			OctaneClient clientA = OctaneSDK.addClient(new PluginServicesBFA(spIdA));
			Assert.assertTrue(clientA.getConfigurationService().isConfigurationValid());
			simulateEventsCycleAllClients();
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (eventsCollectors.containsKey(spIdA) && eventsCollectors.get(spIdA).size() == 3) {
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//  add one more client and verify they are both works okay
			OctaneClient clientB = OctaneSDK.addClient(new PluginServicesBFB(spIdB));
			Assert.assertTrue(clientA.getConfigurationService().isConfigurationValid());
			eventsCollectors.get(spIdA).clear();
			simulateEventsCycleAllClients();
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (eventsCollectors.containsKey(spIdA) &&
						eventsCollectors.get(spIdA).size() == 3 &&
						eventsCollectors.containsKey(spIdB) &&
						eventsCollectors.get(spIdB).size() == 3) {
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//  publish test results
			//  TODO

			//  publish logs
			//  TODO

			//  remove one client and verify it is shut indeed and the second continue to work okay
			OctaneSDK.removeClient(clientA);
			eventsCollectors.get(spIdA).clear();
			eventsCollectors.get(spIdB).clear();
			simulateEventsCycleAllClients();
			GeneralTestUtils.waitAtMostFor(5000, () -> {
				if (eventsCollectors.containsKey(spIdB) &&
						eventsCollectors.get(spIdB).size() == 3) {
					Assert.assertTrue(eventsCollectors.get(spIdA).isEmpty());
					//  TODO: add deeper verification
					return true;
				} else {
					return null;
				}
			});

			//  remove second client and ensure no interactions anymore
			OctaneSDK.removeClient(clientB);
			eventsCollectors.get(spIdB).clear();
			simulateEventsCycleAllClients();
			CIPluginSDKUtils.doWait(2000);
			Assert.assertTrue(eventsCollectors.get(spIdA).isEmpty());
			Assert.assertTrue(eventsCollectors.get(spIdB).isEmpty());
		} finally {
			//  remove simulators
			if (simulators != null) removeSPEPSimulators(simulators.values());
		}

	}

	private Map<String, OctaneSPEndpointSimulator> initSPEPSimulators(Set<String> spIDs, Map<String, List<CIEvent>> eventsCollectors) {
		Map<String, OctaneSPEndpointSimulator> result = new LinkedHashMap<>();

		for (String spID : spIDs) {
			OctaneSPEndpointSimulator simulator = OctaneSPEndpointSimulator.addInstance(spID);

			//  events API
			simulator.installApiHandler(HttpMethod.PUT, "^.*events$", request -> {
				try {
					String rawEventsBody = CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(request.getInputStream()));
					CIEventsList eventsList = dtoFactory.dtoFromJson(rawEventsBody, CIEventsList.class);
					Assert.assertNotNull(eventsList);
					Assert.assertNotNull(eventsList.getServer());
					Assert.assertNotNull(eventsList.getEvents());
					for (CIEvent event : eventsList.getEvents()) {
						eventsCollectors
								.computeIfAbsent(spID, sp -> new LinkedList<>())
								.add(event);
					}
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
