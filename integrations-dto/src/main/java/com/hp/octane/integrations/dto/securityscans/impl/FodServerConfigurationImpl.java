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
package com.hp.octane.integrations.dto.securityscans.impl;

import com.hp.octane.integrations.dto.securityscans.FodServerConfiguration;

public class FodServerConfigurationImpl implements FodServerConfiguration {

    private String apiUrl;
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private long maxPollingTimeoutHours;

    @Override
    public String getApiUrl() {
        return this.apiUrl;
    }

    @Override
    public FodServerConfiguration setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        return this;
    }

    @Override
    public String getClientId() {
        return this.clientId;
    }

    @Override
    public FodServerConfiguration setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    @Override
    public String getClientSecret() {
        return this.clientSecret;
    }

    @Override
    public FodServerConfiguration setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    @Override
    public long getMaxPollingTimeoutHours() {
        return this.maxPollingTimeoutHours;
    }

    @Override
    public FodServerConfiguration setMaxPollingTimeoutHours(long maxPollingTimeoutHours) {
        this.maxPollingTimeoutHours = maxPollingTimeoutHours;
        return this;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public FodServerConfiguration setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    @Override
    public String getBaseUrl() {
        return this.baseUrl;
    }
}
