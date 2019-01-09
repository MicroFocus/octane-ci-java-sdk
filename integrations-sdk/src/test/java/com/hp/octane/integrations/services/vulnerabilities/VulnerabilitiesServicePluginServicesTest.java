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
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import com.hp.octane.integrations.testhelpers.SSCServerSimulator;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class VulnerabilitiesServicePluginServicesTest extends CIPluginServices {
	private static DTOFactory dtoFactory = DTOFactory.getInstance();
	private Map<String, SSCProjectConfiguration> projectConfigurations = new LinkedHashMap<>();

	public VulnerabilitiesServicePluginServicesTest() {
		projectConfigurations.put("job-preflight-false #1", dtoFactory.newDTO(SSCProjectConfiguration.class)
				.setSSCUrl(OctaneSPEndpointSimulator.getSimulatorUrl())
				.setSSCBaseAuthToken("sec-token")
				.setProjectName("project-a")
				.setProjectVersion("version-a")
				.setMaxPollingTimeoutHours(1)
		);
		projectConfigurations.put("job-preflight-true #1", dtoFactory.newDTO(SSCProjectConfiguration.class)
				.setSSCUrl(OctaneSPEndpointSimulator.getSimulatorUrl())
				.setSSCBaseAuthToken("sec-token")
				.setProjectName("project-a")
				.setProjectVersion("version-a")
				.setMaxPollingTimeoutHours(1)
		);
		projectConfigurations.put("jobSSC1 #1", dtoFactory.newDTO(SSCProjectConfiguration.class)
				.setSSCUrl(SSCServerSimulator.getSimulatorUrl())
				.setSSCBaseAuthToken("sec-token")
				.setProjectName("project-a")
				.setProjectVersion("version-a")
				.setMaxPollingTimeoutHours(1)
		);
	}

	@Override
	public CIServerInfo getServerInfo() {
		return dtoFactory.newDTO(CIServerInfo.class)
				.setUrl("http://localhost:9999")
				.setType("custom")
				.setVersion("1.1.1");
	}

	@Override
	public CIPluginInfo getPluginInfo() {
		return dtoFactory.newDTO(CIPluginInfo.class)
				.setVersion(OctaneSDK.SDK_VERSION);
	}

	@Override
	public File getAllowedOctaneStorage() {
		return new File("temp");
	}

	@Override
	public SSCProjectConfiguration getSSCProjectConfiguration(String jobId, String buildId) {
		return projectConfigurations.get(jobId + " #" + buildId);
	}
}
