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
import com.hp.octane.integrations.executor.converters.MavenSurefireAndFailsafeConverter;
import com.hp.octane.integrations.executor.converters.ProtractorConverter;
import org.junit.Assert;
import org.junit.Test;

/**
 * Octane SDK tests
 */

public class CustomConverterTest {

	private final static String rawTests = "v1:MF.simple.tests|AppTest|testAlwaysFail;MF.simple.tests|App2Test|testSendGet";

	@Test
	public void convertTestA()  {
		CustomConverter converter = new CustomConverter("$package.$class#$testName", ",");
		MavenSurefireAndFailsafeConverter mvnConverter = new MavenSurefireAndFailsafeConverter("$package.$class#$testName", ",");

		String actual = converter.convert(rawTests, "").getConvertedTestsString();
		String expected = mvnConverter.convert(rawTests, "").getConvertedTestsString();

	// TODO: fix tests order in mvn or custom convertor: groupBy in mvn changes the tests order
		//  Assert.assertEquals(expected, actual);
		Assert.assertEquals("MF.simple.tests.AppTest#testAlwaysFail,MF.simple.tests.App2Test#testSendGet", actual);
	}

	@Test
	public void convertTestB()  {
		CustomConverter converter = new CustomConverter("$class $testName", "|");
		ProtractorConverter protractorConverter = new ProtractorConverter("$class $testName", "|");

		String actual = converter.convert(rawTests, "").getConvertedTestsString();
		String expected = protractorConverter.convert(rawTests, "").getConvertedTestsString();

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void convertTestC()  {
		CustomConverter converter = new CustomConverter(" --tests $package.$class.$testName", "");
		GradleConverter gradleConverter = new GradleConverter(" --tests $package.$class.$testName", "");

		String actual = converter.convert(rawTests, "").getConvertedTestsString();
		String expected = gradleConverter.convert(rawTests, "").getConvertedTestsString();

		Assert.assertEquals(expected, actual);
	}

}
