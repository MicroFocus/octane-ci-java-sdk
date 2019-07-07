package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.OctaneConnectivityStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaneConnectivityStatusImpl implements OctaneConnectivityStatus {

    private String supportedSdkVersion;
    private String octaneVersion;

    @Override
    public OctaneConnectivityStatus setSupportedSdkVersion(String supportedSdkVersion) {
       this.supportedSdkVersion = supportedSdkVersion;
       return this;
    }

    @Override
    public String getSupportedSdkVersion() {
        return supportedSdkVersion;
    }

    @Override
    public OctaneConnectivityStatus setOctaneVersion(String octaneVersion) {
        this.octaneVersion = octaneVersion;
        return this;
    }

    @Override
    public String getOctaneVersion() {
        return octaneVersion;
    }
}
