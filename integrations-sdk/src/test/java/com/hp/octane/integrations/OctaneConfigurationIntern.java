package com.hp.octane.integrations;

public class OctaneConfigurationIntern extends OctaneConfiguration {
    private String uiLocation;

    public OctaneConfigurationIntern(String iId, String url, String spId) {
        this(iId, url, spId, null, null);
    }

    public OctaneConfigurationIntern(String iId, String url, String spId, String client, String secret) {
        super(iId);
        this.uiLocation = url + "/ui/?&p=" + spId;
        this.setUiLocation(uiLocation);
        this.setClient(client);
        this.setSecret(secret);
    }

    public String getUiLocation() {
        return uiLocation;
    }
}
