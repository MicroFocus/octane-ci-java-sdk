package com.hp.octane.integrations.end2end.basic;

import com.hp.octane.integrations.OctaneConfiguration;

import java.util.UUID;

public class OctaneConfigurationBasicFunctionalityTest extends OctaneConfiguration {
	public static String instanceId = UUID.randomUUID().toString();


	public OctaneConfigurationBasicFunctionalityTest(String url, String sharedSpace, String client, String secret) {
		this.setUrl(url);
		this.setSharedSpace(sharedSpace);
		this.setClient(client);
		this.setSecret(secret);
	}
}
