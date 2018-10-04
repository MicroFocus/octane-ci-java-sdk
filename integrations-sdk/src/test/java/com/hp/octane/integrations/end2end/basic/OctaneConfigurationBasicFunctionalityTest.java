package com.hp.octane.integrations.end2end.basic;

import com.hp.octane.integrations.OctaneConfiguration;

public class OctaneConfigurationBasicFunctionalityTest extends OctaneConfiguration {

	public OctaneConfigurationBasicFunctionalityTest(String instanceId, String url, String sharedSpace, String client, String secret) {
		this.setInstanceId(instanceId);
		this.setUrl(url);
		this.setSharedSpace(sharedSpace);
		this.setClient(client);
		this.setSecret(secret);
	}
}
