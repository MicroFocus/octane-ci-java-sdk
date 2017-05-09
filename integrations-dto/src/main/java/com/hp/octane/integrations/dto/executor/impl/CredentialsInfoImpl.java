package com.hp.octane.integrations.dto.executor.impl;

import com.hp.octane.integrations.dto.executor.CredentialsInfo;

/**
 * Created by shitritn on 4/3/2017.
 */
public class CredentialsInfoImpl implements CredentialsInfo {

    private String username;
    private String password;
    private String credentialsId;

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public CredentialsInfo setUsername(String username) {
        this.username = username;
        return this;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public CredentialsInfo setPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public String getCredentialsId() {
        return this.credentialsId;
    }

    @Override
    public CredentialsInfo setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        return this;
    }
}
