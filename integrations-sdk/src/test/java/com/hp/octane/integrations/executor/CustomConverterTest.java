/*
 * Copyright 2017-2025 Open Text
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

import com.hp.octane.integrations.executor.converters.BDDConverter;
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
        String actual = converter.convert(fullFormatRawData, "", null).getConvertedTestsString();

        Assert.assertEquals("MF.simple.tests.AppTest#testAlwaysFail,MF.simple.tests.App2Test#testSendGet", actual);
    }

    @Test
    public void protractorConverterMultipleCaseTest() {
        ProtractorConverter protractorConverter = new ProtractorConverter();
        String actual = protractorConverter.convert(fullFormatRawData, "", null).getConvertedTestsString();

        Assert.assertEquals("AppTest testAlwaysFail|App2Test testSendGet", actual);
    }

    @Test
    public void protractorConverterMultipleCaseNoPackageTest() {
        ProtractorConverter protractorConverter = new ProtractorConverter();
        String actual = protractorConverter.convert(noPackageRawData, "", null).getConvertedTestsString();

        Assert.assertEquals("AppTest testAlwaysFail|App2Test testSendGet", actual);
    }

    @Test
    public void protractorSetFormatIsIgnored() {
        ProtractorConverter protractorConverter = new ProtractorConverter();
        String actual = protractorConverter.setFormat("{\"testPattern\":\"bubub\",\"testDelimiter\":\"---\"}").convert(fullFormatRawData, "", null).getConvertedTestsString();

        Assert.assertEquals("AppTest testAlwaysFail|App2Test testSendGet", actual);
    }

    @Test
    public void protractorConverterSingleCaseTest() {
        ProtractorConverter protractorConverter = new ProtractorConverter();
        String actual = protractorConverter.convert(singleRawData, "", null).getConvertedTestsString();

        Assert.assertEquals("AppTest testAlwaysFail", actual);
    }

    @Test
    public void gradleConverterMultipleCaseTest() {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(fullFormatRawData, "", null).getConvertedTestsString();

        Assert.assertEquals(" --tests MF.simple.tests.AppTest.testAlwaysFail --tests MF.simple.tests.App2Test.testSendGet", actual);
    }

    @Test
    public void gradleConverterMultipleCaseNoPackageTest() {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(noPackageRawData, "", null).getConvertedTestsString();

        Assert.assertEquals(" --tests AppTest.testAlwaysFail --tests App2Test.testSendGet", actual);
    }

    @Test
    public void gradleConverteMultipleCaseNoClassTest() {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(noClassRawData, "", null).getConvertedTestsString();

        Assert.assertEquals(" --tests testAlwaysFail --tests testSendGet", actual);
    }

    @Test
    public void gradleConverteSingleCaseTest() {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(singleRawData, "", null).getConvertedTestsString();

        Assert.assertEquals(" --tests MF.simple.tests.AppTest.testAlwaysFail", actual);
    }

    @Test
    public void bddTest() {
        String data = "v1:||feature name 1021|runId=2011|featureFilePath=src\\test\\resources\\dan\\Dan_1021.feature;||feature name '1024 #1024  bbb|runId=2012|featureFilePath=src\\test\\resources\\elisheva\\ES_1024 a.feature";
        BDDConverter converter = new BDDConverter();
        String actual = converter.convert(data, "", null).getConvertedTestsString();
        String expected = "'src\\test\\resources\\dan\\Dan_1021.feature' 'src\\test\\resources\\elisheva\\ES_1024 a.feature' --name '^feature name 1021$' --name '^feature name .1024 #1024  bbb$'";
        Assert.assertEquals(expected, actual);
    }
}
