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
