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
import com.hp.octane.integrations.spi.CIPluginServices;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import org.junit.Assert;
import org.junit.Test;

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

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeB() {
		OctaneConfiguration oc = new OctaneConfiguration();

		OctaneSDK.addClient(oc, null);
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
	public void sdkTestNegativeE() {
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
	public void sdkTestNegativeF() {
		OctaneConfiguration oc1 = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost", "1001", null, null);
		OctaneConfiguration oc2 = new OctaneConfigurationIntern(oc1.getInstanceId(), "http://localhost", "1002", null, null);
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

//	//  client dynamically breaks serverInfo/instanceId contract
//	@Test(expected = IllegalStateException.class)
//	public void sdkTestNegativeO() {
//		OctaneClient successfulOne = OctaneSDK.addClient(new PluginServices5());
//		Assert.assertNotNull(successfulOne);
//		Assert.assertEquals(PluginServices5.instanceId, successfulOne.getEffectiveInstanceId());
//
//		try {
//			PluginServices5.serverInfoNull = true;
//			successfulOne.getEffectiveInstanceId();
//		} finally {
//			PluginServices5.serverInfoNull = false;
//			Assert.assertNotNull(OctaneSDK.removeClient(successfulOne));
//		}
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void sdkTestNegativeP() {
//		OctaneClient successfulOne = OctaneSDK.addClient(new PluginServices5());
//		Assert.assertNotNull(successfulOne);
//		Assert.assertEquals(PluginServices5.instanceId, successfulOne.getEffectiveInstanceId());
//
//		try {
//			PluginServices5.instanceIdNull = true;
//			successfulOne.getEffectiveInstanceId();
//		} finally {
//			PluginServices5.instanceIdNull = false;
//			Assert.assertNotNull(OctaneSDK.removeClient(successfulOne));
//		}
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void sdkTestNegativeQ() {
//		OctaneClient successfulOne = OctaneSDK.addClient(new PluginServices5());
//		Assert.assertNotNull(successfulOne);
//		Assert.assertEquals(PluginServices5.instanceId, successfulOne.getEffectiveInstanceId());
//
//		try {
//			PluginServices5.instanceIdEmpty = true;
//			successfulOne.getEffectiveInstanceId();
//		} finally {
//			PluginServices5.instanceIdEmpty = false;
//			Assert.assertNotNull(OctaneSDK.removeClient(successfulOne));
//		}
//	}

	//  illegal OctaneClient creation
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeR() {
		new OctaneClientImpl(null);
	}

	@Test
	public void sdkTestNegativeS() {
		try {
			OctaneSDK.SDKServicesConfigurer.class.getConstructor(CIPluginServices.class).newInstance(null);
			Assert.fail("should not be able to create");
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
		}
	}

	//  illegal shared space ID
//	@Test(expected = IllegalStateException.class)
//	public void sdkTestNegativeT() {
//		OctaneClient successfulOne = OctaneSDK.addClient(new PluginServices6());
//		try {
//			OctaneSDK.addClient(new PluginServices7());
//		} finally {
//			Assert.assertNotNull(OctaneSDK.removeClient(successfulOne));
//		}
//	}

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

//	@Test(expected = IllegalStateException.class)
//	public void sdkTestNegativeX() {
//		OctaneClient successfulOne = OctaneSDK.addClient(new PluginServices7());
//		try {
//			OctaneSDK.getClientBySharedSpaceId("1");
//		} finally {
//			Assert.assertNotNull(OctaneSDK.removeClient(successfulOne));
//		}
//	}

	//  MOCK classes
	public static class PluginServices extends CIPluginServicesBase {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class);
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return dtoFactory.newDTO(CIPluginInfo.class);
		}
	}

	public static class PluginServices1 extends CIPluginServicesBase {
		@Override
		public CIServerInfo getServerInfo() {
			return null;
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return dtoFactory.newDTO(CIPluginInfo.class);
		}
	}

	public static class PluginServices2 extends CIPluginServicesBase {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class);
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return null;
		}
	}

	private static class PluginServices3 extends CIPluginServicesBase {
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
			this.setInstanceId(iId);
			this.setUrl(url);
			this.setSharedSpace(spId);
			this.setClient(client);
			this.setSecret(secret);
		}
	}

//	private static class PluginServices6 extends CIPluginServicesBase {
//		private static String instanceId = UUID.randomUUID().toString();
//		private static String sharedSpaceId = "1001";
//
//		@Override
//		public CIServerInfo getServerInfo() {
//			return dtoFactory.newDTO(CIServerInfo.class).setInstanceId(instanceId);
//		}
//
//		@Override
//		public OctaneConfiguration getOctaneConfiguration() {
//			return dtoFactory.newDTO(OctaneConfiguration.class).setSharedSpace(sharedSpaceId);
//		}
//	}
//
//	private static class PluginServices7 extends CIPluginServicesBase {
//		private static String instanceId = UUID.randomUUID().toString();
//		private static String sharedSpaceId = "1001";
//
//		@Override
//		public CIServerInfo getServerInfo() {
//			return dtoFactory.newDTO(CIServerInfo.class).setInstanceId(instanceId);
//		}
//
//		@Override
//		public OctaneConfiguration getOctaneConfiguration() {
//			return dtoFactory.newDTO(OctaneConfiguration.class).setSharedSpace(sharedSpaceId);
//		}
//	}
}
