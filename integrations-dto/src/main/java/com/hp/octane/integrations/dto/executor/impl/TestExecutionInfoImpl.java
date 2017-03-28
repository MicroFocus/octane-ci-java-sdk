package com.hp.octane.integrations.dto.executor.impl;

import com.hp.octane.integrations.dto.executor.TestExecutionInfo;

/**
 * Created by berkovir on 27/03/2017.
 */
public class TestExecutionInfoImpl implements TestExecutionInfo {
    private String testName;
    private String packageName;

    @Override
    public String getTestName() {
        return testName;
    }

    @Override
    public TestExecutionInfo setTestName(String testName) {
        this.testName = testName;
        return this;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public TestExecutionInfo setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }
}
