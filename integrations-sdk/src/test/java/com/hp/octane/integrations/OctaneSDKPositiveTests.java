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
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.spi.CIPluginServices;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

/**
 * Octane SDK tests
 */

public class OctaneSDKPositiveTests {
	private static DTOFactory dtoFactory = DTOFactory.getInstance();
	private static String instance1 = UUID.randomUUID().toString();
	private static String instance2 = UUID.randomUUID().toString();

	@Test
	public void sdkTestA() {
		List<OctaneClient> octaneClients = OctaneSDK.getClients();
		Assert.assertNotNull(octaneClients);

		OctaneSDK.addClient(new PluginServices1());
		OctaneSDK.addClient(new PluginServices2());

		octaneClients = OctaneSDK.getClients();
		Assert.assertNotNull(octaneClients);
		Assert.assertFalse(octaneClients.isEmpty());

		OctaneClient client = OctaneSDK.getClient(instance1);
		Assert.assertNotNull(client);
		Assert.assertEquals(instance1, client.getEffectiveInstanceId());

		client = OctaneSDK.getClient(instance2);
		Assert.assertNotNull(client);
		Assert.assertEquals(instance2, client.getEffectiveInstanceId());

		OctaneSDK.getClients().forEach(OctaneSDK::removeClient);
	}

	@Test
	public void sdkTestB() {
		OctaneSDK.addClient(new PluginServices3());
		OctaneClient client = OctaneSDK.getClient(PluginServices3.dynamicInstance);

		Assert.assertNotNull(client);
		Assert.assertEquals(PluginServices3.dynamicInstance, client.getEffectiveInstanceId());

		PluginServices3.dynamicInstance = instance2;
		client = OctaneSDK.getClient(instance2);

		Assert.assertNotNull(client);
		Assert.assertEquals(instance2, client.getEffectiveInstanceId());

		OctaneSDK.removeClient(client);
	}

	@Test
	public void sdkTestC() {
		CIPluginServices pluginServices = new PluginServices4();
		OctaneSDK.addClient(pluginServices);
		OctaneClient client = OctaneSDK.getClient(pluginServices);

		Assert.assertNotNull(client);
		Assert.assertEquals(PluginServices4.instanceId, client.getEffectiveInstanceId());

		OctaneSDK.removeClient(client);
	}

	@Test
	public void sdkTestD() {
		OctaneClient clientA = OctaneSDK.addClient(new PluginServices4());
		OctaneClient clientB = OctaneSDK.addClient(new PluginServices5());

		Assert.assertNotNull(clientA);
		Assert.assertNotNull(clientB);
		Assert.assertEquals(PluginServices4.instanceId, clientA.getEffectiveInstanceId());
		Assert.assertEquals(PluginServices5.instanceId, clientB.getEffectiveInstanceId());

		Assert.assertNotNull(clientA.getConfigurationService());
		Assert.assertNotNull(clientA.getEntitiesService());
		Assert.assertNotNull(clientA.getEventsService());
		Assert.assertNotNull(clientA.getLogsService());
		Assert.assertNotNull(clientA.getRestService());
		Assert.assertNotNull(clientA.getTasksProcessor());
		Assert.assertNotNull(clientA.getTestsService());
		Assert.assertNotNull(clientA.getRestService());
		Assert.assertNotNull(clientA.getVulnerabilitiesService());

		Assert.assertNotNull(clientB.getConfigurationService());
		Assert.assertNotNull(clientB.getEntitiesService());
		Assert.assertNotNull(clientB.getEventsService());
		Assert.assertNotNull(clientB.getLogsService());
		Assert.assertNotNull(clientB.getRestService());
		Assert.assertNotNull(clientB.getTasksProcessor());
		Assert.assertNotNull(clientB.getTestsService());
		Assert.assertNotNull(clientB.getRestService());
		Assert.assertNotNull(clientB.getVulnerabilitiesService());

		Assert.assertNotEquals(clientA.getConfigurationService(), clientB.getConfigurationService());
		Assert.assertNotEquals(clientA.getEntitiesService(), clientB.getEntitiesService());
		Assert.assertNotEquals(clientA.getEventsService(), clientB.getEventsService());
		Assert.assertNotEquals(clientA.getLogsService(), clientB.getLogsService());
		Assert.assertNotEquals(clientA.getRestService(), clientB.getRestService());
		Assert.assertNotEquals(clientA.getTasksProcessor(), clientB.getTasksProcessor());
		Assert.assertNotEquals(clientA.getTestsService(), clientB.getTestsService());
		Assert.assertNotEquals(clientA.getRestService(), clientB.getRestService());
		Assert.assertNotEquals(clientA.getVulnerabilitiesService(), clientB.getVulnerabilitiesService());
	}

	private static class PluginServices1 extends CIPluginServicesBase {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class)
					.setInstanceId(instance1);
		}
	}

	private static class PluginServices2 extends CIPluginServicesBase {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class)
					.setInstanceId(instance2);
		}
	}

	private static class PluginServices3 extends CIPluginServicesBase {
		private static String dynamicInstance = UUID.randomUUID().toString();

		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class)
					.setInstanceId(dynamicInstance);
		}
	}

	private static class PluginServices4 extends CIPluginServicesBase {
		private static String instanceId = UUID.randomUUID().toString();

		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class)
					.setInstanceId(instanceId);
		}
	}

	private static class PluginServices5 extends CIPluginServicesBase {
		private static String instanceId = UUID.randomUUID().toString();

		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class)
					.setInstanceId(instanceId);
		}
	}
}
