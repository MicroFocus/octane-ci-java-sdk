package com.hp.octane.integrations.dto.executor.impl;

import com.hp.octane.integrations.dto.executor.TestSuiteExecutionInfo;
import com.hp.octane.integrations.dto.executor.TestExecutionInfo;
import com.hp.octane.integrations.dto.scm.SCMRepository;

import java.util.List;

/**
 * Created by berkovir on 27/03/2017.
 */
public class TestSuiteExecutionInfoImpl implements TestSuiteExecutionInfo {

    private List<TestExecutionInfo> tests;

    private SCMRepository scmRepository;
    private String scmRepositoryCredentialsId;
    private String workspaceId;
    private String suiteId;
    private String suiteRunId;
    private TestingToolType testingToolType;

    @Override
    public List<TestExecutionInfo> getTests() {
        return tests;
    }

    @Override
    public TestSuiteExecutionInfo setTests(List<TestExecutionInfo> tests) {
        this.tests = tests;
        return this;
    }

    @Override
    public SCMRepository getScmRepository() {
        return scmRepository;
    }

    @Override
    public TestSuiteExecutionInfo setScmRepository(SCMRepository scmRepository) {
        this.scmRepository = scmRepository;
        return this;
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public TestSuiteExecutionInfo setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    @Override
    public String getSuiteId() {
        return suiteId;
    }

    @Override
    public TestSuiteExecutionInfo setSuiteId(String suiteId) {
        this.suiteId = suiteId;
        return this;
    }


    @Override
    public TestingToolType getTestingToolType() {
        return testingToolType;
    }

    @Override
    public TestSuiteExecutionInfo setTestingToolType(TestingToolType testingToolType) {
        this.testingToolType = testingToolType;
        return this;
    }

    @Override
    public String getSuiteRunId() {
        return suiteRunId;
    }

    @Override
    public void setSuiteRunId(String suiteRunId) {
        this.suiteRunId = suiteRunId;
    }

    public String getScmRepositoryCredentialsId() {
        return scmRepositoryCredentialsId;
    }

    public void setScmRepositoryCredentialsId(String scmRepositoryCredentialsId) {
        this.scmRepositoryCredentialsId = scmRepositoryCredentialsId;
    }
}
