package com.hp.octane.integrations.end2end.basic;

import com.hp.octane.integrations.OctaneConfigurationIntern;

public class OctaneConfigurationBasicFunctionalityTest extends OctaneConfigurationIntern {

	public OctaneConfigurationBasicFunctionalityTest(String instanceId, String url, String sharedSpace, String client, String secret) {
		super(instanceId, url, sharedSpace);
		this.setClient(client);
		this.setSecret(secret);
	}
}
