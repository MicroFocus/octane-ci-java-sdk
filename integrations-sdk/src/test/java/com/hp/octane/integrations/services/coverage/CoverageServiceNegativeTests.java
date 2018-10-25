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

package com.hp.octane.integrations.services.coverage;

import com.hp.octane.integrations.CIPluginServices;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.coverage.CoverageReportType;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class CoverageServiceNegativeTests {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	//  Coverage service
	@Test(expected = IllegalArgumentException.class)
	public void testA_1() {
		new CoverageServiceImpl(null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testA_2() {
		new CoverageServiceImpl((OctaneSDK.SDKServicesConfigurer) new Object(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testA_3() {
		CoverageService.newInstance(null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testA_4() {
		CoverageService.newInstance((OctaneSDK.SDKServicesConfigurer) new Object(), null, null);
	}

	//  Sonar service
	@Test(expected = IllegalArgumentException.class)
	public void testB_1() {
		new SonarServiceImpl(null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testB_2() {
		new SonarServiceImpl((OctaneSDK.SDKServicesConfigurer) new Object(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testB_3() {
		SonarService.newInstance(null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testB_4() {
		SonarService.newInstance((OctaneSDK.SDKServicesConfigurer) new Object(), null, null);
	}

	//  enqueue API negative testing validation
	@Test(expected = IllegalArgumentException.class)
	public void testE_1() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage(null, null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE_2() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage("", null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE_3() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage("job-id", null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE_4() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage("job-id", "", null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE_5() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage("job-id", "build-id", null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	//  push API negative testing validation
	@Test(expected = IllegalArgumentException.class)
	public void testF_1() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.pushCoverage(null, null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF_2() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.pushCoverage("", null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF_3() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.pushCoverage("job-id", null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF_4() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.pushCoverage("job-id", "", null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF_5() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.pushCoverage("job-id", "build-id", null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF_6() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.pushCoverage("job-id", "build-id", CoverageReportType.JACOCOXML, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	//  is relevant API negative test
	@Test(expected = IllegalArgumentException.class)
	public void testG_1() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.isCoverageReportRelevant(null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testG_2() {
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.isCoverageReportRelevant("");
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
