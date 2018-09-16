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

package com.hp.octane.integrations.end2end.basic;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.tests.BuildContext;
import com.hp.octane.integrations.dto.tests.TestRun;
import com.hp.octane.integrations.dto.tests.TestRunResult;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class PluginServicesBFB extends CIPluginServicesBase {
	private static DTOFactory dtoFactory = DTOFactory.getInstance();
	private String instanceId = UUID.randomUUID().toString();
	private String client = "client_SP_B";
	private String secret = "secret_SP_B";

	private String sharedSpaceID;

	PluginServicesBFB(String sharedSpaceID) {
		this.sharedSpaceID = sharedSpaceID;
	}

	@Override
	public CIServerInfo getServerInfo() {
		return dtoFactory.newDTO(CIServerInfo.class)
				.setInstanceId(instanceId)
				.setUrl("http://localhost:" + OctaneSPEndpointSimulator.getUnderlyingServerPort())
				.setType("custom")
				.setVersion("1.1.1");
	}

	@Override
	public CIPluginInfo getPluginInfo() {
		return dtoFactory.newDTO(CIPluginInfo.class)
				.setVersion(OctaneSDK.SDK_VERSION);
	}

	@Override
	public OctaneConfiguration getOctaneConfiguration() {
		return dtoFactory.newDTO(OctaneConfiguration.class)
				.setUrl("http://localhost:" + OctaneSPEndpointSimulator.getUnderlyingServerPort())
				.setSharedSpace(sharedSpaceID)
				.setApiKey(client)
				.setSecret(secret);
	}

	@Override
	public File getAllowedOctaneStorage() {
		return new File("temp");
	}

	@Override
	public TestsResult getTestsResult(String jobCiId, String buildCiId) {
		List<TestRun> testRuns = new LinkedList<>();
		for (int i = 20; i > 0; i--) {
			testRuns.add(dtoFactory.newDTO(TestRun.class)
					.setModuleName("module")
					.setClassName("class")
					.setPackageName("package")
					.setTestName("test")
					.setDuration(1000)
					.setResult(TestRunResult.FAILED)
			);
		}

		return dtoFactory.newDTO(TestsResult.class)
				.setBuildContext(
						dtoFactory.newDTO(BuildContext.class)
								.setServerId(instanceId)
								.setJobId("job-a")
								.setBuildId("1")
				)
				.setTestRuns(testRuns);
	}

	@Override
	public InputStream getBuildLog(String jobCiId, String buildCiId) {
		return new ByteArrayInputStream("some log line".getBytes());
	}
}
