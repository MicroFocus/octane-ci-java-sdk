package com.hp.octane.integrations;/*
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
 *
 */

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

		OctaneSDK.newClient(new PluginServices1());
		OctaneSDK.newClient(new PluginServices2());

		octaneClients = OctaneSDK.getClients();
		Assert.assertNotNull(octaneClients);
		Assert.assertFalse(octaneClients.isEmpty());

		OctaneClient client = OctaneSDK.getClient(instance1);
		Assert.assertNotNull(client);
		Assert.assertEquals(instance1, client.getEffectiveInstanceId());

		client = OctaneSDK.getClient(instance2);
		Assert.assertNotNull(client);
		Assert.assertEquals(instance2, client.getEffectiveInstanceId());
	}

	@Test
	public void sdkTestB() {
		OctaneSDK.newClient(new PluginServices3());
		OctaneClient client = OctaneSDK.getClient(PluginServices3.dynamicInstance);

		Assert.assertNotNull(client);
		Assert.assertEquals(PluginServices3.dynamicInstance, client.getEffectiveInstanceId());

		PluginServices3.dynamicInstance = instance2;
		client = OctaneSDK.getClient(instance2);

		Assert.assertNotNull(client);
		Assert.assertEquals(instance2, client.getEffectiveInstanceId());
	}

	@Test
	public void sdkTestC() {
		CIPluginServices pluginServices = new PluginServices4();
		OctaneSDK.newClient(pluginServices);
		OctaneClient client = OctaneSDK.getClient(pluginServices);

		Assert.assertNotNull(client);
		Assert.assertEquals(PluginServices4.instanceId, client.getEffectiveInstanceId());
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
}
