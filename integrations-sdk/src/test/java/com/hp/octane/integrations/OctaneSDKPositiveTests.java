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
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Octane SDK tests
 */

public class OctaneSDKPositiveTests {
	private static DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test
	public void sdkTestA() {
		List<OctaneClient> octaneClients = OctaneSDK.getClients();
		Assert.assertNotNull(octaneClients);
		OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1002", null, null);

		OctaneSDK.addClient(oc1, PluginServices.class);
		OctaneSDK.addClient(oc2, PluginServices.class);

		octaneClients = OctaneSDK.getClients();
		Assert.assertNotNull(octaneClients);
		Assert.assertFalse(octaneClients.isEmpty());

		OctaneClient client = OctaneSDK.getClientByInstanceId(oc1.getInstanceId());
		Assert.assertNotNull(client);

		client = OctaneSDK.getClientByInstanceId(oc2.getInstanceId());
		Assert.assertNotNull(client);

		OctaneSDK.getClients().forEach(OctaneSDK::removeClient);
	}

	@Test
	public void sdkTestC() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneSDK.addClient(oc, PluginServices.class);
		OctaneClient client = OctaneSDK.getClientBySharedSpaceId("1001");

		Assert.assertNotNull(client);

		oc.setSharedSpace("1002");
		client = OctaneSDK.getClientBySharedSpaceId(oc.getSharedSpace());

		Assert.assertNotNull(client);

		Assert.assertNotNull(OctaneSDK.removeClient(client));
	}

	@Test
	public void sdkTestD() {
		OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1002", null, null);
		OctaneClient clientA = OctaneSDK.addClient(oc1, PluginServices.class);
		OctaneClient clientB = OctaneSDK.addClient(oc2, PluginServices.class);

		try {
			Assert.assertNotNull(clientA);
			Assert.assertNotNull(clientB);

			Assert.assertNotNull(clientA.getConfigurationService());
			Assert.assertNotNull(clientA.getCoverageService());
			Assert.assertNotNull(clientA.getEntitiesService());
			Assert.assertNotNull(clientA.getEventsService());
			Assert.assertNotNull(clientA.getLogsService());
			Assert.assertNotNull(clientA.getRestService());
			Assert.assertNotNull(clientA.getTasksProcessor());
			Assert.assertNotNull(clientA.getTestsService());
			Assert.assertNotNull(clientA.getRestService());
			Assert.assertNotNull(clientA.getVulnerabilitiesService());

			Assert.assertNotNull(clientB.getConfigurationService());
			Assert.assertNotNull(clientA.getCoverageService());
			Assert.assertNotNull(clientB.getEntitiesService());
			Assert.assertNotNull(clientB.getEventsService());
			Assert.assertNotNull(clientB.getLogsService());
			Assert.assertNotNull(clientB.getRestService());
			Assert.assertNotNull(clientB.getTasksProcessor());
			Assert.assertNotNull(clientB.getTestsService());
			Assert.assertNotNull(clientB.getRestService());
			Assert.assertNotNull(clientB.getVulnerabilitiesService());

			Assert.assertNotEquals(clientA.getConfigurationService(), clientB.getConfigurationService());
			Assert.assertNotEquals(clientA.getCoverageService(), clientB.getCoverageService());
			Assert.assertNotEquals(clientA.getEntitiesService(), clientB.getEntitiesService());
			Assert.assertNotEquals(clientA.getEventsService(), clientB.getEventsService());
			Assert.assertNotEquals(clientA.getLogsService(), clientB.getLogsService());
			Assert.assertNotEquals(clientA.getRestService(), clientB.getRestService());
			Assert.assertNotEquals(clientA.getTasksProcessor(), clientB.getTasksProcessor());
			Assert.assertNotEquals(clientA.getTestsService(), clientB.getTestsService());
			Assert.assertNotEquals(clientA.getRestService(), clientB.getRestService());
			Assert.assertNotEquals(clientA.getVulnerabilitiesService(), clientB.getVulnerabilitiesService());
		} finally {
			Assert.assertNotNull(OctaneSDK.removeClient(clientA));
			Assert.assertNotNull(OctaneSDK.removeClient(clientB));
		}
	}

	@Test
	public void sdkTestE() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneClient client = OctaneSDK.addClient(oc, PluginServices.class);

		Assert.assertEquals("OctaneClientImpl{ instanceId: " + oc.getInstanceId() + " }", client.toString());

		Assert.assertNotNull(OctaneSDK.removeClient(client));
	}

	@Test
	public void sdkTestF() {
		OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1002", null, null);
		OctaneClient clientA = OctaneSDK.addClient(oc1, OctaneSDKNegativeTests.PluginServices.class);
		OctaneClient clientB = OctaneSDK.addClient(oc2, OctaneSDKNegativeTests.PluginServices.class);
		Assert.assertNotNull(clientA);
		Assert.assertNotNull(clientB);

		Assert.assertNotNull(OctaneSDK.removeClient(clientB));
		oc2.setSharedSpace(oc1.getSharedSpace());
	}

	public static class PluginServices extends CIPluginServicesBase {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class);
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return dtoFactory.newDTO(CIPluginInfo.class);
		}

		@Override
		public File getAllowedOctaneStorage() {
			return new File("temp");
		}
	}

	private static class OctaneConfigurationIntern extends OctaneConfiguration {
		private OctaneConfigurationIntern(String iId, String url, String spId, String client, String secret) {
			super(iId, url, spId);
			this.setClient(client);
			this.setSecret(secret);
		}
	}
}
