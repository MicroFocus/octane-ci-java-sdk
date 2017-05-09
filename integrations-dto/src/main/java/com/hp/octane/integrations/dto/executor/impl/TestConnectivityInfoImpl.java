/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;

/**
 * Created by shitritn on 4/3/2017.
 */
public class TestConnectivityInfoImpl implements TestConnectivityInfo {
    private SCMRepository scmRepository;
    private String username;
    private String password;
    private String credentialsId;

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
}
