/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.services.coverage;

import com.hp.octane.integrations.*;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.coverage.CoverageReportType;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.services.sonar.SonarService;
import com.hp.octane.integrations.services.sonar.SonarServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class CoverageServiceNegativeTests {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	//  Coverage service
	@Test(expected = IllegalArgumentException.class)
	public void testA1() {
		new CoverageServiceImpl(null, null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testA2() {
		new CoverageServiceImpl((OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testA3() {
		CoverageService.newInstance(null, null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testA4() {
		CoverageService.newInstance((OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null);
	}

	//  Sonar service
	@Test(expected = IllegalArgumentException.class)
	public void testB1() {
		new SonarServiceImpl(null, null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testB2() {
		new SonarServiceImpl((OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testB3() {
		SonarService.newInstance(null, null, null,null);
	}

	@Test(expected = ClassCastException.class)
	public void testB4() {
		SonarService.newInstance((OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null);
	}

	//  enqueue API negative testing validation
	@Test(expected = IllegalArgumentException.class)
	public void testE1() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage(null, null, null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE2() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage("", null, null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE3() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage("job-id", null, null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE4() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage("job-id", "", null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE5() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.enqueuePushCoverage("job-id", "build-id", null, null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	//  push API negative testing validation
	@Test(expected = IllegalArgumentException.class)
	public void testF1() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
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
	public void testF2() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
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
	public void testF3() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
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
	public void testF4() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
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
	public void testF5() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
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
	public void testF6() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
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
	public void testG1() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.isSonarReportRelevant(null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testG2() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, PluginServices.class);
		Assert.assertNotNull(client);

		CoverageService coverageService = client.getCoverageService();
		try {
			coverageService.isSonarReportRelevant("");
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
