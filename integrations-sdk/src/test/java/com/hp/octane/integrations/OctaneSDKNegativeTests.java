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
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * Octane SDK tests
 */

public class OctaneSDKNegativeTests {
	private static DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeA() {
		OctaneSDK.addClient(null, null);
	}

	//  bad plugin services class
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeC() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneSDK.addClient(oc, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeC1() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneSDK.addClient(oc, PluginServices1.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeC2() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneSDK.addClient(oc, PluginServices2.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeC3() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneSDK.addClient(oc, PluginServices3.class);
	}

	//  duplicate OctaneConfiguration instance
	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeE1() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneClient successfulOne = OctaneSDK.addClient(oc, PluginServices.class);
		try {
			OctaneSDK.addClient(oc, PluginServices.class);
		} finally {
			Assert.assertNotNull(OctaneSDK.removeClient(successfulOne));
		}
	}

	//  duplicate instance ID
	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeE2() {
		OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneConfiguration oc2 = new OctaneConfigurationIntern(oc1.getInstanceId(), "http://localhost", "1002", null, null);
		OctaneClient successfulOne = OctaneSDK.addClient(oc1, PluginServices.class);
		try {
			OctaneSDK.addClient(oc2, PluginServices.class);
		} finally {
			Assert.assertNotNull(OctaneSDK.removeClient(successfulOne));
		}
	}

	//  duplicate shared space ID
	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeE3() {
		OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneClient successfulOne = OctaneSDK.addClient(oc1, PluginServices.class);
		try {
			OctaneSDK.addClient(oc2, PluginServices.class);
		} finally {
			Assert.assertNotNull(OctaneSDK.removeClient(successfulOne));
		}
	}

	//  get client by instance ID
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeG() {
		OctaneSDK.getClientByInstanceId(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeH() {
		OctaneSDK.getClientByInstanceId("");
	}

	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeI() {
		OctaneSDK.getClientByInstanceId("none-existing-one");
	}

	//  remove client
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeL() {
		OctaneSDK.removeClient(null);
	}

	@Test
	public void sdkTestNegativeM() {
		OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1002", null, null);
		OctaneClient successfulOne = OctaneSDK.addClient(oc1, PluginServices.class);
		OctaneClient successfulTwo = OctaneSDK.addClient(oc2, PluginServices.class);
		OctaneClient removed = OctaneSDK.removeClient(successfulOne);
		Assert.assertNull(OctaneSDK.removeClient(removed));
		Assert.assertNotNull(OctaneSDK.removeClient(successfulTwo));
	}

	@Test
	public void sdkTestNegativeN() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneClient successfulOne = OctaneSDK.addClient(oc, PluginServices.class);
		Assert.assertNotNull(OctaneSDK.removeClient(successfulOne));
		Assert.assertNull(OctaneSDK.removeClient(successfulOne));
	}

	//  client dynamically breaks unique instanceId/sharedSpaceId contract
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeO4() {
		OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneConfiguration oc2 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1002", null, null);
		OctaneClient clientA = OctaneSDK.addClient(oc1, PluginServices.class);
		OctaneClient clientB = OctaneSDK.addClient(oc2, PluginServices.class);
		Assert.assertNotNull(clientA);
		Assert.assertNotNull(clientB);

		try {
			oc1.setSharedSpace(oc2.getSharedSpace());
		} finally {
			Assert.assertNotNull(OctaneSDK.removeClient(clientA));
			Assert.assertNotNull(OctaneSDK.removeClient(clientB));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeO5() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneClient client = OctaneSDK.addClient(oc, PluginServices.class);
		Assert.assertNotNull(client);

		try {
			oc.setSharedSpace(null);
		} finally {
			Assert.assertNotNull(OctaneSDK.removeClient(client));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeO6() {
		OctaneConfiguration oc = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneClient client = OctaneSDK.addClient(oc, PluginServices.class);
		Assert.assertNotNull(client);

		try {
			oc.setSharedSpace("");
		} finally {
			Assert.assertNotNull(OctaneSDK.removeClient(client));
		}
	}

	//  illegal OctaneConfiguration properties for test Octane configuration
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeQ1() throws IOException {
		OctaneSDK.testOctaneConfiguration(null, null, null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeQ2() throws IOException {
		OctaneSDK.testOctaneConfiguration("non-valid-url", null, null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeQ3() throws IOException {
		OctaneSDK.testOctaneConfiguration("http://localhost:9999", null, null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeQ4() throws IOException {
		OctaneSDK.testOctaneConfiguration("http://localhost:9999", "", null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeQ5() throws IOException {
		OctaneSDK.testOctaneConfiguration("http://localhost:9999", "1001", null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeQ6() throws IOException {
		OctaneSDK.testOctaneConfiguration("http://localhost:9999", "1001", null, null, PluginServices3.class);
	}

	//  illegal OctaneClient creation
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeR() {
		new OctaneClientImpl(null);
	}

	@Test
	public void sdkTestNegativeS() {
		try {
			OctaneSDK.SDKServicesConfigurer.class.getConstructor(OctaneConfiguration.class, CIPluginServices.class).newInstance(null, null);
			Assert.fail("should not be able to create");
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeU() {
		OctaneSDK.getClientBySharedSpaceId(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeV() {
		OctaneSDK.getClientBySharedSpaceId("");
	}

	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeW() {
		OctaneSDK.getClientBySharedSpaceId("1");
	}

	//  MOCK classes
	public static class PluginServices extends CIPluginServices {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class);
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return dtoFactory.newDTO(CIPluginInfo.class);
		}
	}

	public static class PluginServices1 extends CIPluginServices {
		@Override
		public CIServerInfo getServerInfo() {
			return null;
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return dtoFactory.newDTO(CIPluginInfo.class);
		}
	}

	public static class PluginServices2 extends CIPluginServices {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class);
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return null;
		}
	}

	private static class PluginServices3 extends CIPluginServices {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class);
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return dtoFactory.newDTO(CIPluginInfo.class);
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
