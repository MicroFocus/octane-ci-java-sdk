package com.hp.octane.integrations.end2end.basic;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;

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
}
