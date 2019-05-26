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

	@Test
	public void mavenConverterTest()  {
		CustomConverter converter = new CustomConverter("$package.$class#$testName", ",");
		String actual = converter.convert(fullFormatRawData, "").getConvertedTestsString();

		Assert.assertEquals("MF.simple.tests.AppTest#testAlwaysFail,MF.simple.tests.App2Test#testSendGet", actual);
	}

	@Test
	public void protractorConverterTest()  {
		ProtractorConverter protractorConverter = new ProtractorConverter();
		String actual = protractorConverter.convert(fullFormatRawData, "").getConvertedTestsString();

		Assert.assertEquals("AppTest testAlwaysFail|App2Test testSendGet", actual);
	}

	@Test
	public void protractorConverterNoPackageTest()  {
		ProtractorConverter protractorConverter = new ProtractorConverter();
		String actual = protractorConverter.convert(noPackageRawData, "").getConvertedTestsString();

		Assert.assertEquals("AppTest testAlwaysFail|App2Test testSendGet", actual);
	}

	@Test
	public void protractorConverterSingleTest()  {
		ProtractorConverter protractorConverter = new ProtractorConverter();
		String actual = protractorConverter.convert(singleRawData, "").getConvertedTestsString();

		Assert.assertEquals("AppTest testAlwaysFail", actual);
	}

	@Test
	public void gradleConverterTest()  {
		GradleConverter gradleConverter = new GradleConverter();
		String actual = gradleConverter.convert(fullFormatRawData, "").getConvertedTestsString();

		Assert.assertEquals(" --tests MF.simple.tests.AppTest.testAlwaysFail --tests MF.simple.tests.App2Test.testSendGet", actual);
	}

    @Test
    public void gradleConverterNoPackageTest()  {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(noPackageRawData, "").getConvertedTestsString();

        Assert.assertEquals(" --tests AppTest.testAlwaysFail --tests App2Test.testSendGet", actual);
    }

    @Test
    public void gradleConverteNoClassTest()  {
        GradleConverter gradleConverter = new GradleConverter();
        String actual = gradleConverter.convert(noClassRawData, "").getConvertedTestsString();

        Assert.assertEquals(" --tests testAlwaysFail --tests testSendGet", actual);
    }

	@Test
	public void gradleConverteSingleTest()  {
		GradleConverter gradleConverter = new GradleConverter();
		String actual = gradleConverter.convert(singleRawData, "").getConvertedTestsString();

		Assert.assertEquals(" --tests MF.simple.tests.AppTest.testAlwaysFail", actual);
	}
}
