/*
 *     © 2017 EntIT Software LLC, a Micro Focus company, L.P.
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

import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;

/**
 * Created by berkovir on 27/03/2017.
 */
public class DiscoveryInfoImpl implements DiscoveryInfo {

    private String executorId;
    private String executorLogicalName;
    private String workspaceId;
    private String scmRepositoryId;
    private String scmRepositoryCredentialsId;
    private SCMRepository scmRepository;
    private boolean forceFullDiscovery;
    private TestingToolType testingToolType;

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
}
