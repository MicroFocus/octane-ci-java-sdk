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
package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;

import java.util.List;

/**
 * Converter for JUnit4 for maven.
 * Expected command line : mvn -Dtest=TestCircle#test* test
 * The class create string in format : className1#testName1;className2#testName2;
 * Since of Surefire Plugin 2.19 you can select multiple methods (JUnit 4, JUnit 4.7+ and TestNG):
 */
public class JUnit4MavenConverter extends TestsToRunConverter {
    @Override
    public String convert(List<TestToRunData> data, String executionDirectory) {
        StringBuilder sb = new StringBuilder();
        String joiner = "";
        for (TestToRunData testData : data) {
            sb.append(joiner).append(testData.getClassName()).append("#").append(testData.getTestName());
            joiner = ";";
        }
        return sb.toString();
    }
}
