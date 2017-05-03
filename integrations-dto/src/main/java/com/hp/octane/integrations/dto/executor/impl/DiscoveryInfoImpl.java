package com.hp.octane.integrations.dto.executor.impl;

import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;

/**
 * Created by berkovir on 27/03/2017.
 */
public class DiscoveryInfoImpl implements DiscoveryInfo {

    private String executorId;
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
}
