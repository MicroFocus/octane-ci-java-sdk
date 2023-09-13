/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;

/**
 * Created by berkovir on 27/03/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoveryInfoImpl implements DiscoveryInfo {

    private String executorId;
    private String executorType;
    private String executorLogicalName;
    private String workspaceId;
    private String scmRepositoryId;
    private String scmRepositoryCredentialsId;
    private SCMRepository scmRepository;
    private boolean forceFullDiscovery;
    private TestingToolType testingToolType;
    private String configurationId;
    private String additionalData;

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    public DiscoveryInfo setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    @Override
    public SCMRepository getScmRepository() {
        return scmRepository;
    }

    @Override
    public DiscoveryInfo setScmRepository(SCMRepository scmRepository) {
        this.scmRepository = scmRepository;
        return this;
    }

    @Override
    public boolean isForceFullDiscovery() {
        return forceFullDiscovery;
    }

    @Override
    public DiscoveryInfo setForceFullDiscovery(boolean forceFullDiscovery) {
        this.forceFullDiscovery = forceFullDiscovery;
        return this;
    }

    @Override
    public String getExecutorId() {
        return executorId;
    }

    @Override
    public DiscoveryInfo setExecutorId(String executorId) {
        this.executorId = executorId;
        return this;
    }

    @Override
    public TestingToolType getTestingToolType() {
        return testingToolType;
    }

    @Override
    public DiscoveryInfo setTestingToolType(TestingToolType testingToolType) {
        this.testingToolType = testingToolType;
        return this;
    }

    @Override
    public String getScmRepositoryId() {
        return scmRepositoryId;
    }

    @Override
    public void setScmRepositoryId(String scmRepositoryId) {
        this.scmRepositoryId = scmRepositoryId;
    }

    public String getScmRepositoryCredentialsId() {
        return scmRepositoryCredentialsId;
    }

    public void setScmRepositoryCredentialsId(String scmRepositoryCredentialsId) {
        this.scmRepositoryCredentialsId = scmRepositoryCredentialsId;
    }

    public String getExecutorLogicalName() {
        return executorLogicalName;
    }

    public DiscoveryInfo setExecutorLogicalName(String executorLogicalName) {
        this.executorLogicalName = executorLogicalName;
        return this;
    }

    @Override
    public String getConfigurationId() {
        return configurationId;
    }

    @Override
    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    @Override
    public String getAdditionalData() {
        return additionalData;
    }

    @Override
    public DiscoveryInfo setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
        return this;
    }

    @Override
    public String getExecutorType() {
        return executorType;
    }

    @Override
    public void setExecutorType(String executorType) {
        this.executorType = executorType;
    }

}
