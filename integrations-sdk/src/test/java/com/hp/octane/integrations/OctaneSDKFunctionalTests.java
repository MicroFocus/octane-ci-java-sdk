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

package com.hp.octane.integrations;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Octane SDK functional sanity tests
 *
 * Basic functional sanity test, configuring 2 clients against 2 Octane shared spaces (servers)
 * Validating connectivity, events sent and tests / logs push (including pre-flights) - all this in isolated non-interfering fashion
 */

public class OctaneSDKFunctionalTests {
	private static DTOFactory dtoFactory = DTOFactory.getInstance();
	private static String sharedSpaceIdA = UUID.randomUUID().toString();
	private static String sharedSpaceIdB = UUID.randomUUID().toString();
	private static OctaneSPEndpointSimulator octaneSPEndpointSimulatorA;
	private static OctaneSPEndpointSimulator octaneSPEndpointSimulatorB;
	private static CompletableFuture<Boolean> resolvedA = new CompletableFuture<>();
	private static CompletableFuture<Boolean> resolvedB = new CompletableFuture<>();

	@BeforeClass
	public static void beforeTests() {
		//  prepare EP A
		octaneSPEndpointSimulatorA = OctaneSPEndpointSimulator.addInstance(sharedSpaceIdA);
		octaneSPEndpointSimulatorA.installApiHandler("^.*tasks$", request -> {
			System.out.println("get tasks A...");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException ie) {

			}
			resolvedA.complete(true);
			request.getResponse().setStatus(HttpStatus.SC_NO_CONTENT);
			request.setHandled(true);
		});

		//  prepare EP B
		octaneSPEndpointSimulatorB = OctaneSPEndpointSimulator.addInstance(sharedSpaceIdB);
		octaneSPEndpointSimulatorB.installApiHandler("^.*tasks$", request -> {
			System.out.println("get tasks B...");
			try {
				Thread.sleep(4000);
			} catch (InterruptedException ie) {

			}
			resolvedB.complete(true);
			request.getResponse().setStatus(HttpStatus.SC_NO_CONTENT);
			request.setHandled(true);
		});
	}

	@AfterClass
	public static void afterTests() {
		OctaneSPEndpointSimulator.removeInstance(octaneSPEndpointSimulatorA.getSharedSpaceId());
		OctaneSPEndpointSimulator.removeInstance(octaneSPEndpointSimulatorB.getSharedSpaceId());
	}

	@Test
	public void test() throws Exception {
		//  init 2 clients with configurations to match 2 shared space endpoints
		OctaneClient clientA = OctaneSDK.addClient(new PluginServicesA());
		OctaneClient clientB = OctaneSDK.addClient(new PluginServicesB());

		CompletableFuture.allOf(resolvedA, resolvedB).get();

		OctaneSDK.removeClient(clientA);
		OctaneSDK.removeClient(clientB);
	}

	private static class PluginServicesA extends CIPluginServicesBase {
		private static String instanceId = UUID.randomUUID().toString();

		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class)
					.setInstanceId(instanceId)
					.setUrl("http://localhost:" + OctaneSPEndpointSimulator.getUnderlyingServerPort())
					.setType("custom")
					.setVersion("1.1.1");
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return dtoFactory.newDTO(CIPluginInfo.class)
					.setVersion(OctaneSDK.SDK_VERSION);
		}

		@Override
		public OctaneConfiguration getOctaneConfiguration() {
			return dtoFactory.newDTO(OctaneConfiguration.class)
					.setUrl("http://localhost:" + OctaneSPEndpointSimulator.getUnderlyingServerPort())
					.setSharedSpace(sharedSpaceIdA)
					.setApiKey("apiKey_SP_A")
					.setSecret("secret_SP_A");
		}
	}

	private static class PluginServicesB extends CIPluginServicesBase {
		private static String instanceId = UUID.randomUUID().toString();

		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class)
					.setInstanceId(instanceId)
					.setUrl("http://localhost:" + OctaneSPEndpointSimulator.getUnderlyingServerPort())
					.setType("custom")
					.setVersion("1.1.1");
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return dtoFactory.newDTO(CIPluginInfo.class)
					.setVersion(OctaneSDK.SDK_VERSION);
		}

		@Override
		public OctaneConfiguration getOctaneConfiguration() {
			return dtoFactory.newDTO(OctaneConfiguration.class)
					.setUrl("http://localhost:" + OctaneSPEndpointSimulator.getUnderlyingServerPort())
					.setSharedSpace(sharedSpaceIdB)
					.setApiKey("apiKey_SP_B")
					.setSecret("secret_SP_B");
		}
	}
}
