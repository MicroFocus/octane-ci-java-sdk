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

import com.hp.octane.integrations.CIPluginServices;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class VulnerabilitiesServiceNegativeTests {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test(expected = IllegalArgumentException.class)
	public void testA() {
		new VulnerabilitiesServiceImpl(null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testB() {
		new VulnerabilitiesServiceImpl((OctaneSDK.SDKServicesConfigurer) new Object(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC() {
		VulnerabilitiesService.newInstance(null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testD() {
		VulnerabilitiesService.newInstance((OctaneSDK.SDKServicesConfigurer) new Object(), null, null);
	}

	//  enqueue API negative testing validation
	@Test(expected = IllegalArgumentException.class)
	public void testE_1() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		VulnerabilitiesService vulnerabilitiesService = client.getVulnerabilitiesService();
		try {
			vulnerabilitiesService.enqueueRetrieveAndPushVulnerabilities(null, null, 0, 0);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE_2() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		VulnerabilitiesService vulnerabilitiesService = client.getVulnerabilitiesService();
		try {
			vulnerabilitiesService.enqueueRetrieveAndPushVulnerabilities("", null, 0, 0);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE_3() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		VulnerabilitiesService vulnerabilitiesService = client.getVulnerabilitiesService();
		try {
			vulnerabilitiesService.enqueueRetrieveAndPushVulnerabilities("job-id", null, 0, 0);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE_4() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		VulnerabilitiesService vulnerabilitiesService = client.getVulnerabilitiesService();
		try {
			vulnerabilitiesService.enqueueRetrieveAndPushVulnerabilities("job-id", "", 0, 0);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	//  this one is the OK one
	@Test
	public void testE_5() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		VulnerabilitiesService vulnerabilitiesService = client.getVulnerabilitiesService();
		try {
			vulnerabilitiesService.enqueueRetrieveAndPushVulnerabilities("job-id", "build-id", 0, 0);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	public static final class PluginServices extends CIPluginServices {

		@Override
		public CIServerInfo getServerInfo() {
			return dtoFactory.newDTO(CIServerInfo.class);
		}

		@Override
		public CIPluginInfo getPluginInfo() {
			return dtoFactory.newDTO(CIPluginInfo.class);
		}
	}
}
