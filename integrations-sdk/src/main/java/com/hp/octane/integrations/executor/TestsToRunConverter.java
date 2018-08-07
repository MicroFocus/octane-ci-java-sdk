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

import com.hp.octane.integrations.util.SdkStringUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class TestsToRunConverter {

    public TestsToRunConverterResult convert(String rawTests, String executionDirectory) {

        List<TestToRunData> data = parse(rawTests);
        String converted = convert(data, executionDirectory);
        TestsToRunConverterResult result = new TestsToRunConverterResult(rawTests, data, converted, executionDirectory);
        return result;
    }

    protected abstract String convert(List<TestToRunData> data, String executionDirectory);

    protected List<TestToRunData> parse(String rawTests) {

        //format: v1:package1|class1|test1|key1=val1|key2=val2;package2|class2|test2#arguments2;package3|class3|test3#arguments3
        if (SdkStringUtils.isEmpty(rawTests)) {
            return null;
        }
        int versionIndex = rawTests.indexOf(":");
        if (versionIndex < 0) {
            throw new IllegalArgumentException("Invalid format : missing version part.");
        }
        String version = rawTests.substring(0, versionIndex);
        if (!version.equalsIgnoreCase("v1")) {
            throw new IllegalArgumentException("Invalid format. Not supported version " + version + ".");
        }

        String[] rawTestsArr = rawTests.substring(versionIndex + 1).split(";");
        List<TestToRunData> result = new ArrayList<>(rawTestsArr.length);
        int TEST_PARTS_MINIMAL_SIZE = 3;//package1|class1|test1
        int PARAMETER_SIZE = 2;//key=value
        for (String rawtest : rawTestsArr) {
            String[] testParts = rawtest.split("\\|");
            if (testParts.length < TEST_PARTS_MINIMAL_SIZE) {
                throw new IllegalArgumentException("Test '" + rawtest + "' doesnot contains all required components");
            }
            TestToRunData data = new TestToRunData();
            result.add(data);
            data.setPackageName(testParts[0]).setClassName(testParts[1]).setTestName(testParts[2]);
            for (int i = TEST_PARTS_MINIMAL_SIZE; i < testParts.length; i++) {
                String[] parameterParts = testParts[i].split("=");
                if (parameterParts.length != PARAMETER_SIZE) {
                    throw new IllegalArgumentException("Test' " + rawtest + "' contains illegal parameter format");
                }
                data.addParameters(testParts[0], testParts[1]);
            }
        }

        return result;
    }

}
