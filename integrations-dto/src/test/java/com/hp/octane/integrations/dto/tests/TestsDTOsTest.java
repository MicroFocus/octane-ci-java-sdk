/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

import javax.xml.bind.JAXBException;

import com.hp.octane.integrations.dto.DTOFactory;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
	private static final Long duration = 3000L;
	private static final Long started = System.currentTimeMillis();

	@Test
	public void test_A() throws JAXBException {
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
	public void test_B() throws JAXBException {
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
}
