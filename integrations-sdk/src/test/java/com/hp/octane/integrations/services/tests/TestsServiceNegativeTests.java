/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
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

package com.hp.octane.integrations.services.tests;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneConfigurationIntern;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class TestsServiceNegativeTests {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static OctaneClient client;

	@BeforeClass
	public static void setupClient() {
		String inId = UUID.randomUUID().toString();
		String sspId = UUID.randomUUID().toString();
		OctaneConfiguration configuration = new OctaneConfigurationIntern(inId, OctaneSPEndpointSimulator.getSimulatorUrl(), sspId);
		client = OctaneSDK.addClient(configuration, TestsServicePluginServicesTest.class);
	}

	@AfterClass
	public static void removeClient() {
		OctaneSDK.removeClient(client);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testA() {
		new TestsServiceImpl(null, null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testB() {
		new TestsServiceImpl((OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC() {
		TestsService.newInstance(null, null, null, null);
	}

	@Test(expected = ClassCastException.class)
	public void testD() {
		TestsService.newInstance((OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE1() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).isTestsResultRelevant(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE2() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).isTestsResultRelevant("", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF1() throws IOException {
		TestsService testsService = client.getTestsService();
		TestsResult tr = null;
		((TestsServiceImpl)testsService).pushTestsResult(tr, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF2() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).pushTestsResult(dtoFactory.newDTO(TestsResult.class), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF3() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).pushTestsResult(dtoFactory.newDTO(TestsResult.class), "", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF4() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).pushTestsResult(dtoFactory.newDTO(TestsResult.class), "some", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testF5() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).pushTestsResult(dtoFactory.newDTO(TestsResult.class), "some", "");
	}


	@Test(expected = IllegalArgumentException.class)
	public void testG1() throws IOException {
		TestsService testsService = client.getTestsService();
		InputStream is = null;
		((TestsServiceImpl)testsService).pushTestsResult(is, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testG2() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).pushTestsResult(new ByteArrayInputStream(new byte[]{}), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testG3() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).pushTestsResult(new ByteArrayInputStream(new byte[]{}), "", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testG4() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).pushTestsResult(new ByteArrayInputStream(new byte[]{}), "some", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testG5() throws IOException {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).pushTestsResult(new ByteArrayInputStream(new byte[]{}), "some", "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testH1() {
		TestsService testsService = client.getTestsService();
		((TestsServiceImpl)testsService).enqueuePushTestsResult(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testH2() {
		TestsService testsService = client.getTestsService();
		testsService.enqueuePushTestsResult("", null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testH3() {
		TestsService testsService = client.getTestsService();
		testsService.enqueuePushTestsResult("some", null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testH4() {
		TestsService testsService = client.getTestsService();
		testsService.enqueuePushTestsResult("some", "", null);
	}
}
