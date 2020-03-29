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
 */

package com.hp.octane.integrations.executor;

import com.hp.octane.integrations.executor.converters.CustomConverter;
import com.hp.octane.integrations.executor.converters.GradleConverter;
import com.hp.octane.integrations.executor.converters.ProtractorConverter;
import org.junit.Assert;
import org.junit.Test;

/**
 * Octane SDK tests
 */

public class CustomConverterTest {

    private final static String fullFormatRawData = "v1:MF.simple.tests|AppTest|testAlwaysFail;MF.simple.tests|App2Test|testSendGet";
    private final static String noPackageRawData = "v1:|AppTest|testAlwaysFail;|App2Test|testSendGet";
    private final static String noClassRawData = "v1:||testAlwaysFail;||testSendGet";
    private final static String singleRawData = "v1:MF.simple.tests|AppTest|testAlwaysFail";

    private String buildCustomFormat(String testPattern, String testDelimiter) {
        String format = "{\"testPattern\":\"%s\",\"testDelimiter\":\"%s\"}";
        return String.format(format, testPattern, testDelimiter);
    }

    @Test
    public void mavenConverterTest() {
        CustomConverter converter = new CustomConverter(buildCustomFormat("$package.$class#$testName", ","));
        String actual = converter.convert(fullFormatRawData, "").getConvertedTestsString();

        Assert.assertEquals("MF.simple.tests.AppTest#testAlwaysFail,MF.simple.tests.App2Test#testSendGet", actual);
    }

    @Test
    public void protractorConverterMultipleCaseTest() {
        ProtractorConverter protractorConverter = new ProtractorConverter();
        String actual = protractorConverter.convert(fullFormatRawData, "").getConvertedTestsString();

        Assert.assertEquals("AppTest testAlwaysFail|App2Test testSendGet", actual);
    }

    @Test
    public void protractorConverterMultipleCaseNoPackageTest() {
        ProtractorConverter protractorConverter = new ProtractorConverter();
        String actual = protractorConverter.convert(noPackageRawData, "").getConvertedTestsString();

        Assert.assertEquals("AppTest testAlwaysFail|App2Test testSendGet", actual);
    }

    @Test
    public void protractorSetFormatIsIgnored() {
        ProtractorConverter protractorConverter = new ProtractorConverter();
        String actual = protractorConverter.setFormat("{\"testPattern\":\"bubub\",\"testDelimiter\":\"---\"}").convert(fullFormatRawData, "").getConvertedTestsString();

        Assert.assertEquals("AppTest testAlwaysFail|App2Test testSendGet", actual);
    }

    @Test
    public void protractorConverterSingleCaseTest() {
        ProtractorConverter protractorConverter = new ProtractorConverter();
        String actual = protractorConverter.convert(singleRawData, "").getConvertedTestsString();

        Assert.assertEquals("AppTest testAlwaysFail", actual);
    }

    @Test
    public void gradleConverterMultipleCaseTest() {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(fullFormatRawData, "").getConvertedTestsString();

        Assert.assertEquals(" --tests MF.simple.tests.AppTest.testAlwaysFail --tests MF.simple.tests.App2Test.testSendGet", actual);
    }

    @Test
    public void gradleConverterMultipleCaseNoPackageTest() {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(noPackageRawData, "").getConvertedTestsString();

        Assert.assertEquals(" --tests AppTest.testAlwaysFail --tests App2Test.testSendGet", actual);
    }

    @Test
    public void gradleConverteMultipleCaseNoClassTest() {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(noClassRawData, "").getConvertedTestsString();

        Assert.assertEquals(" --tests testAlwaysFail --tests testSendGet", actual);
    }

    @Test
    public void gradleConverteSingleCaseTest() {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(singleRawData, "").getConvertedTestsString();

        Assert.assertEquals(" --tests MF.simple.tests.AppTest.testAlwaysFail", actual);
    }
}
