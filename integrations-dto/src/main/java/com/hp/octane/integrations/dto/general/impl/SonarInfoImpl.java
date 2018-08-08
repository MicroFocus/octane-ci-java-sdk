package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.SonarInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarInfoImpl implements SonarInfo{

    String serverUrl;
    String ServerVersion;
    String ServerAuthenticationToken;

    public SonarInfoImpl(){

    }

    public SonarInfoImpl(String serverUrl, String serverVersion, String serverAuthenticationToken) {
        this.serverUrl = serverUrl;
        ServerVersion = serverVersion;
        ServerAuthenticationToken = serverAuthenticationToken;
    }

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    @Override
    public SonarInfo setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    @Override
    public String getServerVersion() {
        return ServerVersion;
    }

    @Override
    public SonarInfo setServerVersion(String serverVersion) {
        ServerVersion = serverVersion;
        return this;
    }

    @Override
    public String getServerAuthenticationToken() {
        return ServerAuthenticationToken;
    }

    @Override
    public SonarInfo setServerAuthenticationToken(String serverAuthenticationToken) {
        ServerAuthenticationToken = serverAuthenticationToken;
        return this;
    }
}
