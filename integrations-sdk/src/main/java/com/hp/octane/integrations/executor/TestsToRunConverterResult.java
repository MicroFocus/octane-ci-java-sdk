package com.hp.octane.integrations.executor;

import java.util.List;

public class TestsToRunConverterResult {
    private String rawTestsString;
    private List<TestToRunData> testsData;
    private String convertedTestsString;
    private String workingDirectory;

    public TestsToRunConverterResult(String rawTestsString, List<TestToRunData> testsData, String convertedTestsString, String workingDirectory) {
        this.rawTestsString = rawTestsString;
        this.testsData = testsData;
        this.convertedTestsString = convertedTestsString;
        this.workingDirectory = workingDirectory;
    }

    public String getRawTestsString() {
        return rawTestsString;
    }

    public void setRawTestsString(String rawTestsString) {
        this.rawTestsString = rawTestsString;
    }

    public List<TestToRunData> getTestsData() {
        return testsData;
    }

    public void setTestsData(List<TestToRunData> testsData) {
        this.testsData = testsData;
    }

    public String getConvertedTestsString() {
        return convertedTestsString;
    }

    public void setConvertedTestsString(String convertedTestsString) {
        this.convertedTestsString = convertedTestsString;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
