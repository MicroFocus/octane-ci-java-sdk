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

import com.hp.octane.integrations.executor.converters.MbtTest;

import java.io.Serializable;
import java.util.List;

public class TestsToRunConverterResult implements Serializable {
    private String rawTestsString;
    private List<TestToRunData> testsData;
    private String convertedTestsString;
    private String workingDirectory;
    private String testsToRunConvertedParameterName;
    private List<MbtTest> mbtTests;

    public TestsToRunConverterResult(String rawTestsString, List<TestToRunData> testsData, String convertedTestsString, String workingDirectory, String testsToRunConvertedParameterName) {
        this.rawTestsString = rawTestsString;
        this.testsData = testsData;
        this.convertedTestsString = convertedTestsString;
        this.workingDirectory = workingDirectory;
        this.testsToRunConvertedParameterName = testsToRunConvertedParameterName;
    }

    public String getRawTestsString() {
        return rawTestsString;
    }

    public List<TestToRunData> getTestsData() {
        return testsData;
    }

    public String getConvertedTestsString() {
        return convertedTestsString;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getTestsToRunConvertedParameterName() {
        return testsToRunConvertedParameterName;
    }

    public List<MbtTest> getMbtTests() {
        return mbtTests;
    }

    public void setMbtTests(List<MbtTest> mbtTests) {
        this.mbtTests = mbtTests;
    }
}
