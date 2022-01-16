package com.hp.octane.integrations;

public class OctaneConfigurationIntern extends OctaneConfiguration {

    public OctaneConfigurationIntern(String iId, String url, String spId) {
        this(iId, url, spId, null, null);
    }

    public OctaneConfigurationIntern(String iId, String url, String spId, String client, String secret) {
        super(iId);
        this.setUrlAndSpace(url, spId);
        this.setClient(client);
        this.setSecret(secret);
    }
}
