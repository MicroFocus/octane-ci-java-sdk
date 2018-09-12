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
 *
 */

package com.hp.octane.integrations;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.spi.CIPluginServices;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

/**
 * Octane SDK tests
 */

public class OctaneSDKNegativeTests {
	private static DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeA() {
		OctaneSDK.newClient(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeB() {
		OctaneSDK.newClient(new PluginServices1());
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeC() {
		OctaneSDK.newClient(new PluginServices2());
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeD() {
		OctaneSDK.newClient(new PluginServices3());
	}

	//  duplicate CIPluginServices instance
	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeE() {
		CIPluginServices pluginServices = new PluginServices4();
		OctaneClient successfulOne = OctaneSDK.newClient(pluginServices);
		try {
			OctaneSDK.newClient(pluginServices);
		} finally {
			OctaneSDK.removeClient(successfulOne);
		}
	}

	//  duplicate instance ID
	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeF() {
		OctaneClient successfulOne = OctaneSDK.newClient(new PluginServices4());
		try {
			OctaneSDK.newClient(new PluginServices4());
		} finally {
			successfulOne.close();
		}
	}

	//  get client by instance ID
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeG() {
		OctaneSDK.getClient((String) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeH() {
		OctaneSDK.getClient("");
	}

	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeI() {
		OctaneSDK.getClient("none-existing-one");
	}

	//  get client by CIPluginServices object
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeJ() {
		OctaneSDK.getClient((CIPluginServices) null);
	}

	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeK() {
		OctaneSDK.getClient(new PluginServices1());
	}

	//  remove client
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeL() {
		OctaneSDK.removeClient(null);
	}

	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeM() {
		OctaneClient successfulOne = OctaneSDK.newClient(new PluginServices4());
		OctaneClient successfulTwo = OctaneSDK.newClient(new PluginServices4());
		OctaneClient removed = OctaneSDK.removeClient(successfulOne);
		try {
			OctaneSDK.removeClient(removed);
		} finally {
			OctaneSDK.removeClient(successfulTwo);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeN() {
		OctaneClient successfulOne = OctaneSDK.newClient(new PluginServices4());
		successfulOne.close();
		successfulOne.close();
	}

	//  client dynamically breaks serverInfo/instanceId contract
	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeO() {
		OctaneClient successfulOne = OctaneSDK.newClient(new PluginServices5());
		Assert.assertNotNull(successfulOne);
		Assert.assertEquals(PluginServices5.instanceId, successfulOne.getEffectiveInstanceId());

		try {
			PluginServices5.serverInfoNull = true;
			successfulOne.getEffectiveInstanceId();
		} finally {
			PluginServices5.serverInfoNull = false;
			OctaneSDK.removeClient(successfulOne);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeP() {
		OctaneClient successfulOne = OctaneSDK.newClient(new PluginServices5());
		Assert.assertNotNull(successfulOne);
		Assert.assertEquals(PluginServices5.instanceId, successfulOne.getEffectiveInstanceId());

		try {
			PluginServices5.instanceIdNull = true;
			successfulOne.getEffectiveInstanceId();
		} finally {
			PluginServices5.instanceIdNull = false;
			OctaneSDK.removeClient(successfulOne);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void sdkTestNegativeQ() {
		OctaneClient successfulOne = OctaneSDK.newClient(new PluginServices5());
		Assert.assertNotNull(successfulOne);
		Assert.assertEquals(PluginServices5.instanceId, successfulOne.getEffectiveInstanceId());

		try {
			PluginServices5.instanceIdEmpty = true;
			successfulOne.getEffectiveInstanceId();
		} finally {
			PluginServices5.instanceIdEmpty = false;
			OctaneSDK.removeClient(successfulOne);
		}
	}

	//  illegal OctaneClient creation
	@Test(expected = IllegalArgumentException.class)
	public void sdkTestNegativeR() {
		new OctaneClientImpl(null);
	}

	private static class PluginServices1 extends CIPluginServicesBase {
	}

	private static class PluginServices2 extends CIPluginServicesBase {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class);
		}
	}

	private static class PluginServices3 extends CIPluginServicesBase {
		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class)
					.setInstanceId("");
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
		private static boolean serverInfoNull = false;
		private static boolean instanceIdNull = false;
		private static boolean instanceIdEmpty = false;

		@Override
		public CIServerInfo getServerInfo() {
			return serverInfoNull ? null : dtoFactory.newDTO(CIServerInfo.class)
					.setInstanceId(instanceIdNull ? null : (instanceIdEmpty ? "" : instanceId));
		}
	}
}
