/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.dto.executor.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;

/**
 * Created by shitritn on 4/3/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestConnectivityInfoImpl implements TestConnectivityInfo {
    private SCMRepository scmRepository;
    private String username;
    private String password;
    private String credentialsId;
    private String additionalData;

    @Override
    public SCMRepository getScmRepository() {
        return scmRepository;
    }

    @Override
    public TestConnectivityInfo setScmRepository(SCMRepository scmRepository) {
        this.scmRepository = scmRepository;
        return this;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public TestConnectivityInfo setUsername(String username) {
        this.username = username;
        return this;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public TestConnectivityInfo setPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public String getCredentialsId() {
        return this.credentialsId;
    }

    @Override
    public TestConnectivityInfo setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        return this;
    }

    @Override
    public String getAdditionalData() {
        return this.additionalData;
    }

    @Override
    public TestConnectivityInfo setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
        return this;
    }
}
