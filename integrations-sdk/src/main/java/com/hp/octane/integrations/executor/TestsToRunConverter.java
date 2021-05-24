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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.hp.octane.integrations.executor.TestToRunData.TESTS_TO_RUN_STRING_VERSION;

public abstract class TestsToRunConverter {

    public static final String DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER = "testsToRunConverted";
    private String testsToRunConvertedParameterName = DEFAULT_TESTS_TO_RUN_CONVERTED_PARAMETER;
    private String format = "";

    public TestsToRunConverter setFormat(String format) {
        this.format = format;
        return this;
    }

    protected String getFormat() {
        return format;
    }

    public TestsToRunConverterResult convert(String rawTests, String executionDirectory) {

        List<TestToRunData> data = parse(rawTests);
        String converted = convert(data, executionDirectory);
        TestsToRunConverterResult result = new TestsToRunConverterResult(rawTests, data, converted, executionDirectory, testsToRunConvertedParameterName);
        afterConvert(result);
        return result;
    }

    protected void afterConvert(TestsToRunConverterResult result){

    }

    protected abstract String convert(List<TestToRunData> data, String executionDirectory);

    protected void setTestsToRunConvertedParameterName(String value) {
        if (SdkStringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("TestsToRunConvertedParameter cannot be empty");
        }
        testsToRunConvertedParameterName = value;
    }

    protected List<TestToRunData> parse(String rawTests) {

        //format: v1:package1|class1|test1|key1=val1|key2=val2;package2|class2|test2#arguments2;package3|class3|test3#arguments3
        if (SdkStringUtils.isEmpty(rawTests)) {
            return null;
        }
        boolean bTestToRunStringVersion = rawTests.startsWith(TESTS_TO_RUN_STRING_VERSION);
        if( bTestToRunStringVersion ) {
            return parse(rawTests.substring(rawTests.indexOf(":") + 1).split(";"));
        } else {
            return parseJson(rawTests);
        }
    }

    protected List<TestToRunData> parseJson(String rawTestsJson) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            TestToRunDataCollection result = objectMapper.readValue(rawTestsJson, TestToRunDataCollection.class);
            return result.getTestsToRun();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid tests format: " + e.getMessage(), e);
        }
    }

    protected List<TestToRunData> parse(String[] rawTestsArr) {
        List<TestToRunData> result = new ArrayList<>(rawTestsArr.length);
        int TEST_PARTS_MINIMAL_SIZE = 3;//package1|class1|test1
        int PARAMETER_SIZE = 2;//key=value
        for (String rawtest : rawTestsArr) {
            String[] testParts = rawtest.split("\\|");
            if (testParts.length < TEST_PARTS_MINIMAL_SIZE) {
                throw new IllegalArgumentException("Test '" + rawtest + "' does not contains all required components");
            }
            TestToRunData data = new TestToRunData();
            result.add(data);
            data.setPackageName(testParts[0]).setClassName(testParts[1]).setTestName(testParts[2]);
            for (int i = TEST_PARTS_MINIMAL_SIZE; i < testParts.length; i++) {
                String[] parameterParts = testParts[i].split("=");
                if (parameterParts.length != PARAMETER_SIZE) {
                    throw new IllegalArgumentException("Test' " + rawtest + "' contains an illegal parameter format." +
                            "\nTextual format uses the following characters as separators: |;=. Your values probably contain these characters." +
                            "Switch to JSON format by defining in ALM Octane the space parameter 'TESTS_TO_RUN_PARAMETER_JSON_FORMAT' = true.");
                }
                data.addParameters(parameterParts[0], parameterParts[1]);
            }
        }

        return result;
    }
}
