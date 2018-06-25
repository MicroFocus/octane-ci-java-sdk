/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

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
