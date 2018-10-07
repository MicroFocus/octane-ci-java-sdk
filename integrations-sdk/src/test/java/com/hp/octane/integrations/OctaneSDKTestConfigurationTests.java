package com.hp.octane.integrations;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Assert;
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

		OctaneResponse response = OctaneSDK.testOctaneConfiguration(OctaneSPEndpointSimulator.getSimulatorUrl(), spId, "client", "secret", PluginServices.class);
		Assert.assertNotNull(response);
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatus());

		OctaneSPEndpointSimulator.removeInstance(spId);
	}

	static class PluginServices extends CIPluginServicesBase {
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
