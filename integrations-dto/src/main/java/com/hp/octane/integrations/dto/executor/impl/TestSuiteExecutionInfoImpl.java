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
