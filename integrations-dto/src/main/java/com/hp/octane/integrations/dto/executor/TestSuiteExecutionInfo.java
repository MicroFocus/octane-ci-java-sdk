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

package com.hp.octane.integrations.dto.executor;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.scm.SCMRepository;

import java.util.List;

/**
 * Created by berkovir on 27/03/2017.
 */
public interface TestSuiteExecutionInfo extends DTOBase {

    List<TestExecutionInfo> getTests();

    TestSuiteExecutionInfo setTests(List<TestExecutionInfo> tests);

    SCMRepository getScmRepository();

    TestSuiteExecutionInfo setScmRepository(SCMRepository scmRepository);

    String getWorkspaceId();

    TestSuiteExecutionInfo setWorkspaceId(String workspaceId);

    String getSuiteId();

    TestSuiteExecutionInfo setSuiteId(String suiteId);

    TestingToolType getTestingToolType();

    TestSuiteExecutionInfo setTestingToolType(TestingToolType testingToolType);

    String getSuiteRunId();

    void setSuiteRunId(String suiteRunId);

    String getScmRepositoryCredentialsId();

    void setScmRepositoryCredentialsId(String scmRepositoryCredentialsId);
}