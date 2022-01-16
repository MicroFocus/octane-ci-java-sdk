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

package com.hp.octane.integrations.dto.tests;

import com.hp.octane.integrations.dto.DTOFactory;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Testing Tests DTOs
 */

public class TestsDTOsTest {
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();
    private static final String moduleName = "module";
    private static final String packageName = "package";
    private static final String className = "class";
    private static final String testName = "test";
    private static final TestRunResult result = TestRunResult.PASSED;
    private static final long duration = 3000;
    private static final long started = System.currentTimeMillis();

    @Test
    public void test_A() {
        TestRun tr = dtoFactory.newDTO(TestRun.class)
                .setModuleName(moduleName)
                .setPackageName(packageName)
                .setClassName(className)
                .setTestName(testName)
                .setResult(result)
                .setStarted(started)
                .setDuration(duration);

        String xml = dtoFactory.dtoToXml(tr);
        assertNotNull(xml);
        assertTrue("external_run_id should not be in xml", !xml.contains("external_run_id"));
        assertTrue("external_test_id should not be in xml", !xml.contains("external_test_id"));
        TestRun backO = dtoFactory.dtoFromXml(xml, TestRun.class);
        assertNotNull(backO);
        assertEquals(moduleName, backO.getModuleName());
        assertEquals(packageName, backO.getPackageName());
        assertEquals(className, backO.getClassName());
        assertEquals(testName, backO.getTestName());
        assertEquals(result, backO.getResult());
        assertEquals(started, backO.getStarted());
        assertEquals(duration, backO.getDuration());
    }

    @Test
    public void test_A_with_externalRunAndTest() {
        String externalTestId = "externalTestId111";
        String externalRunId = "externalRunId111";
        TestRun tr = dtoFactory.newDTO(TestRun.class)
                .setModuleName(moduleName)
                .setPackageName(packageName)
                .setClassName(className)
                .setTestName(testName)
                .setResult(result)
                .setStarted(started)
                .setDuration(duration)
                .setExternalTestId(externalTestId)
                .setExternalRunId(externalRunId);

        String xml = dtoFactory.dtoToXml(tr);
        assertNotNull(xml);
        TestRun backO = dtoFactory.dtoFromXml(xml, TestRun.class);
        assertNotNull(backO);
        assertEquals(moduleName, backO.getModuleName());
        assertEquals(packageName, backO.getPackageName());
        assertEquals(className, backO.getClassName());
        assertEquals(testName, backO.getTestName());
        assertEquals(result, backO.getResult());
        assertEquals(started, backO.getStarted());
        assertEquals(duration, backO.getDuration());
        assertEquals(externalRunId, backO.getExternalRunId());
        assertEquals(externalTestId, backO.getExternalTestId());
    }

    @Test
    public void test_B() {
        TestRun tr1 = dtoFactory.newDTO(TestRun.class)
                .setModuleName(moduleName)
                .setPackageName(packageName)
                .setClassName(className)
                .setTestName(testName)
                .setResult(result)
                .setStarted(started)
                .setDuration(duration);
        TestRun tr2 = dtoFactory.newDTO(TestRun.class)
                .setModuleName(moduleName)
                .setPackageName(packageName)
                .setClassName(className)
                .setTestName(testName)
                .setResult(result)
                .setStarted(started)
                .setDuration(duration);
        TestRun tr3 = dtoFactory.newDTO(TestRun.class)
                .setModuleName(moduleName)
                .setPackageName(packageName)
                .setClassName(className)
                .setTestName(testName)
                .setResult(result)
                .setStarted(started)
                .setDuration(duration);
        TestsResult result = dtoFactory.newDTO(TestsResult.class)
                .setTestRuns(Arrays.asList(tr1, tr2, tr3));

        String xml = dtoFactory.dtoToXml(result);
        assertNotNull(xml);
        TestsResult backO = dtoFactory.dtoFromXml(xml, TestsResult.class);
        assertNotNull(backO);
        assertNotNull(backO.getTestRuns());
        assertEquals(3, backO.getTestRuns().size());
    }

    @Test
    public void parsingMqmTestResults() {
        String payload = "<test_result><build server_id=\"to-be-filled-in-SDK\" job_id=\"simpleTests\" build_id=\"284\"/><test_fields><test_field type=\"Test_Type\" value=\"End to End\"/><test_field type=\"Testing_Tool_Type\" value=\"Selenium\"/>" +
                "</test_fields><test_runs><test_run module=\"/helloWorld\" package=\"hello\" class=\"HelloWorldTest\" name=\"testTwo\" status=\"Failed\" duration=\"2\" started=\"1430919316223\">" +
                "<error type=\"java.lang.AssertionError\" message=\"expected:'111' but was:'222'\">java.lang.AssertionError :aaa</error><description>My run description</description></test_run></test_runs></test_result>";
        TestsResult result = dtoFactory.dtoFromXml(payload, TestsResult.class);
        assertNotNull(result.getBuildContext());
        assertNotNull(result.getTestFields());
        assertNotNull(result.getTestRuns());

        assertEquals(result.getBuildContext().getServerId(), "to-be-filled-in-SDK");
        assertEquals(result.getBuildContext().getJobId(), "simpleTests");
        assertEquals(result.getBuildContext().getBuildId(), "284");

        assertEquals(result.getTestFields().size(), 2);
        assertEquals(result.getTestFields().get(0).getValue(), "End to End");
        assertEquals(result.getTestFields().get(0).getType(), "Test_Type");

        assertEquals(result.getTestFields().get(1).getValue(), "Selenium");
        assertEquals(result.getTestFields().get(1).getType(), "Testing_Tool_Type");

        assertEquals(result.getTestRuns().get(0).getModuleName(), "/helloWorld");
        assertEquals(result.getTestRuns().get(0).getPackageName(), "hello");
        assertEquals(result.getTestRuns().get(0).getClassName(), "HelloWorldTest");
        assertEquals(result.getTestRuns().get(0).getTestName(), "testTwo");
        assertEquals(result.getTestRuns().get(0).getResult(), TestRunResult.FAILED);
        assertEquals(result.getTestRuns().get(0).getDuration(), 2);
        assertEquals(result.getTestRuns().get(0).getStarted(), 1430919316223l);

        assertEquals(result.getTestRuns().get(0).getError().getErrorType(), "java.lang.AssertionError");
        assertEquals(result.getTestRuns().get(0).getError().getErrorType(), "java.lang.AssertionError");
        assertEquals(result.getTestRuns().get(0).getError().getErrorMessage(), "expected:'111' but was:'222'");
        assertEquals(result.getTestRuns().get(0).getError().getStackTrace(), "java.lang.AssertionError :aaa");

        assertEquals(result.getTestRuns().get(0).getDescription(), "My run description");
        String converted = dtoFactory.dtoToXml(result);
        assertEquals(payload, converted);

    }

    @Test
    public void parsingJUnitTestResults() {
        String payload = "<testsuite><properties><property name=\"nameAAA\" value=\"valueAAA\"/><property name=\"nameBBB\" value=\"valueBBB\"/></properties><testcase name=\"testAppErr\" classname=\"MF.simple.tests.AppTest\" time=\"0.002\"><failure type=\"junit.framework.AssertionFailedError\">junit.framework.AssertionFailedError at MF.simple.tests.AppTest.testAppC2(AppTest.java:56)</failure></testcase><testcase name=\"testAppA\" classname=\"MF.simple.tests.AppTest\" time=\"0.001\"/><testcase name=\"testAppB\" classname=\"MF.simple.tests.AppTest\" time=\"0.003\"/><testcase name=\"testAppC\" classname=\"MF.simple.tests.AppTest\" time=\"0\"/></testsuite>";
        TestSuite result = dtoFactory.dtoFromXml(payload, TestSuite.class);

        assertEquals(2, result.getProperties().size());
        assertEquals("nameAAA", result.getProperties().get(0).getPropertyName());
        assertEquals("valueAAA", result.getProperties().get(0).getPropertyValue());
        assertEquals("nameBBB", result.getProperties().get(1).getPropertyName());
        assertEquals("valueBBB", result.getProperties().get(1).getPropertyValue());

        assertEquals(4, result.getTestCases().size());

        assertEquals("testAppErr", result.getTestCases().get(0).getName());
        assertEquals("MF.simple.tests.AppTest", result.getTestCases().get(0).getClassName());
        assertEquals("0.002", result.getTestCases().get(0).getTime());
        assertNotNull(result.getTestCases().get(0).getFailure());
        assertEquals("junit.framework.AssertionFailedError", result.getTestCases().get(0).getFailure().getType());
        assertEquals("junit.framework.AssertionFailedError at MF.simple.tests.AppTest.testAppC2(AppTest.java:56)", result.getTestCases().get(0).getFailure().getStackTrace());

        assertEquals("testAppA", result.getTestCases().get(1).getName());
        assertEquals("MF.simple.tests.AppTest", result.getTestCases().get(1).getClassName());
        assertEquals("0.001", result.getTestCases().get(1).getTime());
        assertNull(result.getTestCases().get(1).getFailure());

        assertEquals("testAppB", result.getTestCases().get(2).getName());
        assertEquals("MF.simple.tests.AppTest", result.getTestCases().get(2).getClassName());
        assertEquals("0.003", result.getTestCases().get(2).getTime());
        assertNull(result.getTestCases().get(2).getFailure());

        assertEquals("testAppC", result.getTestCases().get(3).getName());
        assertEquals("MF.simple.tests.AppTest", result.getTestCases().get(3).getClassName());
        assertEquals("0", result.getTestCases().get(3).getTime());
        assertNull(result.getTestCases().get(3).getFailure());

        String converted = dtoFactory.dtoToXml(result);
        assertEquals(payload, converted);

    }
}
