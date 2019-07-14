/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
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
package com.hp.octane.integrations.executor;

import java.util.List;

public class TestsToRunConverterResult {
    private String rawTestsString;
    private List<TestToRunData> testsData;
    private String convertedTestsString;
    private String workingDirectory;
    private String convertedTestsParameter = TestsToRunConverter.DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER;

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

    public String getConvertedTestsParameter() {
        return convertedTestsParameter;
    }

    public void setConvertedTestsParameter(String convertedTestsParameter) {
        this.convertedTestsParameter = convertedTestsParameter;
    }
}
