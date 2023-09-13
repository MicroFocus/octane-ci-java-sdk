/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

package com.hp.octane.integrations.services.scmdata;

import com.hp.octane.integrations.*;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class SCMDataServiceNegativeTests {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test(expected = IllegalArgumentException.class)
	public void testA() {
		new SCMDataServiceImpl(null, null, null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testB() {
		new SCMDataServiceImpl(null, (OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC() {
		SCMDataService.newInstance(null, null, null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testD() {
		SCMDataService.newInstance(null, (OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null);
	}

	//  enqueue API negative testing validation
	@Test(expected = IllegalArgumentException.class)
	public void testE1() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, SCMDataServiceNegativeTests.PluginServices.class);
		Assert.assertNotNull(client);

		SCMDataService scmDataService = client.getSCMDataService();
		try {
			scmDataService.enqueueSCMData(null, null,null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE2() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, SCMDataServiceNegativeTests.PluginServices.class);
		Assert.assertNotNull(client);

		SCMDataService scmDataService = client.getSCMDataService();
		try {
			scmDataService.enqueueSCMData("", null,null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE3() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, SCMDataServiceNegativeTests.PluginServices.class);
		Assert.assertNotNull(client);

		SCMDataService scmDataService = client.getSCMDataService();
		try {
			scmDataService.enqueueSCMData("job-id", null,null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE4() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, SCMDataServiceNegativeTests.PluginServices.class);
		Assert.assertNotNull(client);

		SCMDataService scmDataService = client.getSCMDataService();
		try {
			scmDataService.enqueueSCMData("job-id", "",null, null);
		} finally {
			OctaneSDK.removeClient(client);
		}
	}

	//  this one is the OK one
	@Test
	public void testE5() {
		OctaneConfiguration configuration = new OctaneConfigurationIntern(UUID.randomUUID().toString(), "http://localhost:8080", UUID.randomUUID().toString());
		OctaneClient client = OctaneSDK.addClient(configuration, SCMDataServiceNegativeTests.PluginServices.class);
		Assert.assertNotNull(client);

		SCMDataService scmDataService = client.getSCMDataService();
		try {
			scmDataService.enqueueSCMData("job-id", "build-id",null, null);
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
