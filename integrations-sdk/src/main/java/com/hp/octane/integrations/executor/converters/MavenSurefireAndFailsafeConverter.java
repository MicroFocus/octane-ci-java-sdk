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
import com.hp.octane.integrations.utils.SdkStringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converter for JUnit/TestNg over  Maven Surefire And Failsafe  plugins
 * Expected command line : mvn -Dtest=TestCircle#test* test
 * The class create string in format : package1.className1#testNameA+testNameB,package2.className2#testNameC;
 *
 *
 * Failsafe : mvn -Dit.test=package1.className1#testNameA verify
 * Surefire : mvn -Dtest=package1.className1#testNameA test
 */
public class MavenSurefireAndFailsafeConverter extends TestsToRunConverter {

    @Override
    public String convert(List<TestToRunData> data, String executionDirectory, Map<String, String> globalParameters) {
        StringBuilder sb = new StringBuilder();
        String classJoiner = "";
        Map<String, List<TestToRunData>> fullClassName2tests = data.stream().collect(Collectors.groupingBy(item -> getFullClassPath(item)));
        for (Map.Entry<String, List<TestToRunData>> entry : fullClassName2tests.entrySet()) {
            sb.append(classJoiner);
            String tests = entry.getValue().stream().map(TestToRunData::getTestName).collect(Collectors.joining("+"));
            sb.append(entry.getKey()).append("#").append(tests);
            classJoiner = ",";
        }
        return sb.toString();
    }

    private String getFullClassPath(TestToRunData runData) {
        String result = "";
        if (SdkStringUtils.isNotEmpty(runData.getPackageName())) {
            result = runData.getPackageName() + ".";
        }
        result += runData.getClassName();
        return result;
    }
}
