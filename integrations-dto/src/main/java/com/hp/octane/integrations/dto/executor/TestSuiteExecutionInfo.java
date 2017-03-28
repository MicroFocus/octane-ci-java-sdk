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

    String getExecutorId();

    TestSuiteExecutionInfo setExecutorId(String executorId);

    String getWorkspaceId();

    TestSuiteExecutionInfo setWorkspaceId(String workspaceId);

    String getSuiteId();

    TestSuiteExecutionInfo setSuiteId(String suiteId);

    TestingToolType getTestingToolType();

    TestSuiteExecutionInfo setTestingToolType(TestingToolType testingToolType);
}
