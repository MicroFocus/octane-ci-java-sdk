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
package com.hp.octane.integrations;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OctaneSDKTestConfigurationTests {
	private static final Logger logger = LogManager.getLogger(OctaneSDKTestConfigurationTests.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test
	public void testA1() throws IOException {
		String spId = "1001";
		OctaneSPEndpointSimulator simulator = OctaneSPEndpointSimulator.addInstance(spId);
		simulator.installApiHandler(HttpMethod.GET, "^.*/analytics/ci/servers/connectivity/status$", request -> {
			request.getResponse().setStatus(HttpServletResponse.SC_OK);
			try {
				request.getResponse().getWriter().write("{}");
				request.getResponse().flushBuffer();
			} catch (IOException ioe) {
				logger.error("failed to process status request in MOCK server", ioe);
			}
		});
		simulator.installApiHandler(HttpMethod.GET, "^.*/workspaces?.*$", request -> {
			request.getResponse().setStatus(HttpServletResponse.SC_OK);
			try {
				request.getResponse().getWriter().write("{\"total_count\":1,\"data\":[{\"type\":\"workspace\",\"id\":\"1002\"}],\"exceeds_total_count\":false}");
				request.getResponse().flushBuffer();
			} catch (IOException ioe) {
				logger.error("failed to process status request in MOCK server", ioe);
			}
		});

		OctaneSDK.testOctaneConfigurationAndFetchAvailableWorkspaces(OctaneSPEndpointSimulator.getSimulatorUrl(), spId, "client", "secret", PluginServices.class);
		OctaneSPEndpointSimulator.removeInstance(spId);
	}

	static class PluginServices extends CIPluginServices {
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
