/*
 * Copyright 2017-2026 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static com.hp.octane.integrations.executor.TestsToRunFramework.JUnit4;
import static com.hp.octane.integrations.executor.TestsToRunFramework.MF_MI_AGENT;
import static com.hp.octane.integrations.executor.TestsToRunFramework.MF_UFT;

public class TestsToRunConverterTest {

    private final static String v1MavenFormatRawData = "v1:com.microfocus.octane.testing_framework_demo.mvn.unittest|AppTest|testC;com.microfocus.octane.testing_framework_demo.mvn.unittest|AnotherAppTest|testC;com.microfocus.octane.testing_framework_demo.mvn.unittest|HelloWorldTest|hello3;com.microfocus.octane.testing_framework_demo.mvn.unittest|AnotherAppTest|testA;com.microfocus.octane.testing_framework_demo.mvn.unittest|HelloWorldTest|hello2;com.microfocus.octane.testing_framework_demo.mvn.unittest|HelloWorldTest|hello1;com.microfocus.octane.testing_framework_demo.mvn.unittest|AppTest|testA;com.microfocus.octane.testing_framework_demo.mvn.unittest|AnotherAppTest|testB;com.microfocus.octane.testing_framework_demo.mvn.unittest|AppTest|testB";
    private final static String v2MavenFormatRawData = "{\"testsToRun\":[{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AppTest\",\"testName\":\"testC\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AnotherAppTest\",\"testName\":\"testC\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"HelloWorldTest\",\"testName\":\"hello3\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AnotherAppTest\",\"testName\":\"testA\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"HelloWorldTest\",\"testName\":\"hello2\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"HelloWorldTest\",\"testName\":\"hello1\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AppTest\",\"testName\":\"testA\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AnotherAppTest\",\"testName\":\"testB\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AppTest\",\"testName\":\"testB\",\"parameters\":{\"dataTable\":\"\"}}],\"version\":\"v2\"}";
    private final static String outputMavenResult = "com.microfocus.octane.testing_framework_demo.mvn.unittest.AppTest#testC+testA+testB,com.microfocus.octane.testing_framework_demo.mvn.unittest.HelloWorldTest#hello3+hello2+hello1,com.microfocus.octane.testing_framework_demo.mvn.unittest.AnotherAppTest#testC+testA+testB";

    private final static String v1UFTFormatRawData = "v1:||DTTest1|dataTable=DTTest1.xls|iterations=oneIteration;||DTTest2|dataTable=DTTest2.xls|iterations=rngAll,2;||DTTest3|dataTable=DTTest3.xls|iterations=rngIterations,2,3;||DTTest4|dataTable=DTTest4.xls|iterations=oneIteration|param1=param1Value|param2=(float)param2Value";
    private final static String v2UFTFormatRawData = "{\"testsToRun\":[{\"packageName\":\"\",\"className\":\"\",\"testName\":\"DTTest1\",\"parameters\":{\"dataTable\":\"DTTest1.xls\",\"iterations\":\"oneIteration\"}}, " +
            "{\"packageName\":\"\",\"className\":\"\",\"testName\":\"DTTest2\",\"parameters\":{\"dataTable\":\"DTTest2.xls\",\"iterations\":\"rngAll,2\"}}, " +
            "{\"packageName\":\"\",\"className\":\"\",\"testName\":\"DTTest3\",\"parameters\":{\"dataTable\":\"DTTest3.xls\",\"iterations\":\"rngIterations,2,3\"}}," +
            "{\"packageName\":\"\",\"className\":\"\",\"testName\":\"DTTest4\",\"parameters\":{\"dataTable\":\"DTTest4.xls\",\"iterations\":\"oneIteration\",\"param1\":\"param1Value\",\"param2\":\"(float) param2Value\"}}],\"version\":\"v2\"}";

    private final static String outputUFTResult =
            "<Mtbx>" +
                    "\r\n  <Test name=\"DTTest1\" path=\"\\DTTest1\">" +
                    "\r\n    <DataTable path=\"\\DTTest1.xls\"/>" +
                    "\r\n    <Iterations mode=\"oneIteration\"/>" +
                    "\r\n  </Test>" +
                    "\r\n  <Test name=\"DTTest2\" path=\"\\DTTest2\">" +
                    "\r\n    <DataTable path=\"\\DTTest2.xls\"/>" +
                    "\r\n    <Iterations mode=\"rngAll\"/>" +
                    "\r\n  </Test>" +
                    "\r\n  <Test name=\"DTTest3\" path=\"\\DTTest3\">" +
                    "\r\n    <DataTable path=\"\\DTTest3.xls\"/>" +
                    "\r\n    <Iterations end=\"3\" mode=\"rngIterations\" start=\"2\"/>" +
                    "\r\n  </Test>" +
                    "\r\n  <Test name=\"DTTest4\" path=\"\\DTTest4\">" +
                    "\r\n    <Parameter name=\"param1\" value=\"param1Value\"/>" +
                    "\r\n    <Parameter name=\"param2\" type=\"float\" value=\"param2Value\"/>" +
                    "\r\n    <DataTable path=\"\\DTTest4.xls\"/>" +
                    "\r\n    <Iterations mode=\"oneIteration\"/>" +
                    "\r\n  </Test>" +
                    "\r\n</Mtbx>\r\n";

    private final static String MI_AGENT_RUN_1042 = "{\"type\":\"run\",\"workspace_id\":2001,\"name\":\"tsMIAgent\",\"test_name\":\"Login flow\",\"order_in_suite_run\":1,\"duration\":null,\"id\":\"1042\",\"subtype\":\"run_manual\",\"au_tester_configuration\":{\"browser\":{\"BROWSER_NAME\":\"Google Chrome\",\"BROWSER_LOCALE\":\"en-US\"},\"agent\":{\"MAX_FAILURES\":3,\"MAX_NUMBER_OF_STEPS\":100,\"BROWSER_USE_RUN_TIMEOUT\":2700}},\"has_attachments\":false,\"parent_suite\":{\"type\":\"run_suite\",\"id\":\"5001\",\"name\":\"tsMIAgent\"},\"run_steps\":{\"total_count\":1,\"data\":[{\"type\":\"run_step\",\"id\":\"s1\",\"result\":null,\"attachments\":{\"total_count\":0,\"data\":[]},\"index_in_report\":\"1\",\"index_in_script\":0,\"description\":\"Open login page\",\"actual\":null,\"activity_level\":0,\"from_call_to_test\":false,\"step_type\":{\"type\":\"list_node\",\"id\":\"list_node.manual_test_run_step_type.normal\",\"name\":\"Normal\"},\"run\":{\"type\":\"run_manual\",\"id\":\"1042\",\"name\":\"tsMIAgent\",\"activity_level\":0}}]},\"test\":{\"type\":\"test_manual\",\"id\":\"9001\",\"name\":\"Login flow\",\"subtype\":\"test_manual\",\"activity_level\":0},\"native_status\":{\"type\":\"list_node\",\"id\":\"list_node.run_native_status.not_completed\",\"name\":\"In Progress\"},\"run_by\":{\"type\":\"workspace_user\",\"id\":\"1001\",\"workspace_id\":2001,\"activity_level\":0,\"full_name\":\"sa@nga\"}}";

    private final static String MI_AGENT_RUN_1043 = "{\"type\":\"run\",\"workspace_id\":2001,\"name\":\"tsMIAgent\",\"test_name\":\"Checkout flow\",\"order_in_suite_run\":2,\"duration\":null,\"id\":\"1043\",\"subtype\":\"run_manual\",\"has_attachments\":false,\"parent_suite\":{\"type\":\"run_suite\",\"id\":\"5001\",\"name\":\"tsMIAgent\"},\"run_steps\":{\"total_count\":2,\"data\":[{\"type\":\"run_step\",\"id\":\"s2\",\"result\":null,\"attachments\":{\"total_count\":0,\"data\":[]},\"index_in_report\":\"1\",\"index_in_script\":0,\"description\":\"Add item\",\"actual\":null,\"activity_level\":0,\"from_call_to_test\":false,\"step_type\":{\"type\":\"list_node\",\"id\":\"list_node.manual_test_run_step_type.normal\",\"name\":\"Normal\"},\"run\":{\"type\":\"run_manual\",\"id\":\"1043\",\"name\":\"tsMIAgent\",\"activity_level\":0}},{\"type\":\"run_step\",\"id\":\"s3\",\"result\":null,\"attachments\":{\"total_count\":0,\"data\":[]},\"index_in_report\":\"2\",\"index_in_script\":1,\"description\":\"Verify total\",\"actual\":null,\"activity_level\":0,\"from_call_to_test\":false,\"step_type\":{\"type\":\"list_node\",\"id\":\"list_node.manual_test_run_step_type.validate\",\"name\":\"Validate\"},\"run\":{\"type\":\"run_manual\",\"id\":\"1043\",\"name\":\"tsMIAgent\",\"activity_level\":0}}]},\"test\":{\"type\":\"test_manual\",\"id\":\"9002\",\"name\":\"Checkout flow\",\"subtype\":\"test_manual\",\"activity_level\":0},\"native_status\":{\"type\":\"list_node\",\"id\":\"list_node.run_native_status.not_completed\",\"name\":\"In Progress\"},\"run_by\":{\"type\":\"workspace_user\",\"id\":\"1001\",\"workspace_id\":2001,\"activity_level\":0,\"full_name\":\"sa@nga\"}}";

    private final static String MI_AGENT_EXPECTED_MANIFEST = "{\"data\":[" + MI_AGENT_RUN_1042 + "," + MI_AGENT_RUN_1043 + "],\"total_count\":2}";


    private String converterTest(TestsToRunFramework framework, String rawData) {
        TestsToRunConverter converter = TestsToRunConvertersFactory.createConverter(framework);
        String result = converter.convert(rawData, "", null).getConvertedTestsString();

        return result;
    }

    @Test
    public void customConverterJsonTest() {
        String actual = converterTest(JUnit4, v2MavenFormatRawData);

        Assert.assertEquals(outputMavenResult, actual);
    }

    @Test
    public void customConverterStringTest() {
        String actual = converterTest(JUnit4, v1MavenFormatRawData);

        Assert.assertEquals(outputMavenResult, actual);
    }

    @Test
    public void uftConverterJsonTest() {
        String actual = converterTest(MF_UFT, v2UFTFormatRawData);

        Assert.assertEquals(outputUFTResult, actual);
    }

    @Test
    public void uftConverterStringTest() {
        String actual = converterTest(MF_UFT, v1UFTFormatRawData);

        Assert.assertEquals(outputUFTResult, actual);
    }

    @Test
    public void miAgentConverterManifestTest() throws Exception {
        TestToRunData first = new TestToRunData()
                .setTestName("Login flow")
                .addParameters("runId", "1042")
                .addParameters("manualRunData", MI_AGENT_RUN_1042);
        TestToRunData second = new TestToRunData()
                .setTestName("Checkout flow")
                .addParameters("runId", "1043")
                .addParameters("manualRunData", MI_AGENT_RUN_1043);

        String actual = TestsToRunConvertersFactory.createConverter(MF_MI_AGENT)
                .convert(Arrays.asList(first, second), "", null)
                .getConvertedTestsString();

        ObjectMapper objectMapper = new ObjectMapper();
        Assert.assertEquals(objectMapper.readTree(MI_AGENT_EXPECTED_MANIFEST), objectMapper.readTree(actual));

        Assert.assertEquals("Google Chrome", objectMapper.readTree(actual)
            .path("data").get(0)
            .path("au_tester_configuration").path("browser").path("BROWSER_NAME").asText());
        Assert.assertEquals("sa@nga", objectMapper.readTree(actual)
            .path("data").get(1)
            .path("run_by").path("full_name").asText());
    }
}
