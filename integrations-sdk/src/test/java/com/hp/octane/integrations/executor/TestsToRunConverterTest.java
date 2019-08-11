package com.hp.octane.integrations.executor;

import org.junit.Assert;
import org.junit.Test;

import static com.hp.octane.integrations.executor.TestsToRunFramework.JUnit4;
import static com.hp.octane.integrations.executor.TestsToRunFramework.MF_UFT;

public class TestsToRunConverterTest {

    private final static String v1MavenFormatRawData = "v1:com.microfocus.octane.testing_framework_demo.mvn.unittest|AppTest|testC;com.microfocus.octane.testing_framework_demo.mvn.unittest|AnotherAppTest|testC;com.microfocus.octane.testing_framework_demo.mvn.unittest|HelloWorldTest|hello3;com.microfocus.octane.testing_framework_demo.mvn.unittest|AnotherAppTest|testA;com.microfocus.octane.testing_framework_demo.mvn.unittest|HelloWorldTest|hello2;com.microfocus.octane.testing_framework_demo.mvn.unittest|HelloWorldTest|hello1;com.microfocus.octane.testing_framework_demo.mvn.unittest|AppTest|testA;com.microfocus.octane.testing_framework_demo.mvn.unittest|AnotherAppTest|testB;com.microfocus.octane.testing_framework_demo.mvn.unittest|AppTest|testB";
    private final static String v2MavenFormatRawData = "{\"testsToRun\":[{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AppTest\",\"testName\":\"testC\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AnotherAppTest\",\"testName\":\"testC\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"HelloWorldTest\",\"testName\":\"hello3\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AnotherAppTest\",\"testName\":\"testA\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"HelloWorldTest\",\"testName\":\"hello2\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"HelloWorldTest\",\"testName\":\"hello1\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AppTest\",\"testName\":\"testA\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AnotherAppTest\",\"testName\":\"testB\",\"parameters\":{\"dataTable\":\"\"}},{\"packageName\":\"com.microfocus.octane.testing_framework_demo.mvn.unittest\",\"className\":\"AppTest\",\"testName\":\"testB\",\"parameters\":{\"dataTable\":\"\"}}],\"version\":\"v2\"}";
    private final static String outputMavenResult = "com.microfocus.octane.testing_framework_demo.mvn.unittest.AppTest#testC+testA+testB,com.microfocus.octane.testing_framework_demo.mvn.unittest.HelloWorldTest#hello3+hello2+hello1,com.microfocus.octane.testing_framework_demo.mvn.unittest.AnotherAppTest#testC+testA+testB";

    private final static String v1UFTFormatRawData = "v1:||DTTest1|dataTable=DTTest1.xls;||DTTest2|dataTable=DTTest2.xls;||DTTest3|dataTable=DTTest2.xls";
    private final static String v2UFTFormatRawData = "{\"testsToRun\":[{\"packageName\":\"\",\"className\":\"\",\"testName\":\"DTTest1\",\"parameters\":{\"dataTable\":\"DTTest1.xls\"}},{\"packageName\":\"\",\"className\":\"\",\"testName\":\"DTTest2\",\"parameters\":{\"dataTable\":\"DTTest2.xls\"}},{\"packageName\":\"\",\"className\":\"\",\"testName\":\"DTTest3\",\"parameters\":{\"dataTable\":\"DTTest2.xls\"}}],\"version\":\"v2\"}";
    private final static String outputUFTResult =
                                                    "<Mtbx>" +
                                                    "\r\n  <Test name=\"DTTest1\" path=\"\\DTTest1\">" +
                                                    "\r\n    <DataTable path=\"\\DTTest1.xls\"/>" +
                                                    "\r\n  </Test>" +
                                                    "\r\n  <Test name=\"DTTest2\" path=\"\\DTTest2\">" +
                                                    "\r\n    <DataTable path=\"\\DTTest2.xls\"/>" +
                                                    "\r\n  </Test>" +
                                                    "\r\n  <Test name=\"DTTest3\" path=\"\\DTTest3\">" +
                                                    "\r\n    <DataTable path=\"\\DTTest2.xls\"/>" +
                                                    "\r\n  </Test>" +
                                                    "\r\n</Mtbx>\r\n";


    private String converterTest(TestsToRunFramework framework, String rawData) {
        TestsToRunConverter converter = TestsToRunConvertersFactory.createConverter(framework);
        String result = converter.convert(rawData, "").getConvertedTestsString();

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

}
