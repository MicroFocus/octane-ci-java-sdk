package com.hp.octane.integrations.services.tests;

import com.hp.octane.integrations.CIPluginServices;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;

public class TestsServicePluginServicesTest extends CIPluginServices {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Override
	public CIServerInfo getServerInfo() {
		return dtoFactory.newDTO(CIServerInfo.class);
	}

	@Override
	public CIPluginInfo getPluginInfo() {
		return dtoFactory.newDTO(CIPluginInfo.class);
	}
}
