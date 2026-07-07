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
package com.hp.octane.integrations.services.tests;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneConfigurationIntern;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestsServiceNegativeTests {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static OctaneClient client;

	@BeforeAll
	public static void setupClient() {
		String inId = UUID.randomUUID().toString();
		String sspId = UUID.randomUUID().toString();
		OctaneConfiguration configuration = new OctaneConfigurationIntern(inId, OctaneSPEndpointSimulator.getSimulatorUrl(), sspId);
		client = OctaneSDK.addClient(configuration, TestsServicePluginServicesTest.class);
	}

	@AfterAll
	public static void removeClient() {
		OctaneSDK.removeClient(client);
	}

	@Test
	public void testA() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TestsServiceImpl(null, null, null, null);
        });
    }

	@Test
	public void testB() {
        assertThrows(ClassCastException.class, () -> {
            new TestsServiceImpl((OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null);
        });
    }

	@Test
	public void testC() {
        assertThrows(IllegalArgumentException.class, () ->
            TestsService.newInstance(null, null, null, null));
    }

	@Test
	public void testD() {
        assertThrows(ClassCastException.class, () ->
            TestsService.newInstance((OctaneSDK.SDKServicesConfigurer) new Object(), null, null, null));
    }

	@Test
	public void testE1() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            ((TestsServiceImpl) testsService).isTestsResultRelevant(null, null);
        });
    }

	@Test
	public void testE2() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            ((TestsServiceImpl) testsService).isTestsResultRelevant("", null);
        });
    }

	@Test
	public void testF1() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            TestsResult tr = null;
            ((TestsServiceImpl) testsService).pushTestsResult(tr, null, null);
        });
    }

	@Test
	public void testF2() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            TestsResult newDTO = dtoFactory.newDTO(TestsResult.class);
            ((TestsServiceImpl) testsService).pushTestsResult(newDTO, null, null);
        });
    }

	@Test
	public void testF3() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            TestsResult newDTO = dtoFactory.newDTO(TestsResult.class);
            ((TestsServiceImpl) testsService).pushTestsResult(newDTO, "", null);
        });
    }

	@Test
	public void testF4() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            TestsResult newDTO = dtoFactory.newDTO(TestsResult.class);
            ((TestsServiceImpl) testsService).pushTestsResult(newDTO, "some", null);
        });
    }

	@Test
	public void testF5() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            TestsResult newDTO = dtoFactory.newDTO(TestsResult.class);
            ((TestsServiceImpl) testsService).pushTestsResult(newDTO, "some", "");
        });
    }


	@Test
	public void testG1() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            InputStream is = null;
            ((TestsServiceImpl) testsService).pushTestsResult(is, null, null);
        });
    }

	@Test
	public void testG2() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            InputStream is = new ByteArrayInputStream(new byte[]{});
            ((TestsServiceImpl) testsService).pushTestsResult(is, null, null);
        });
    }

	@Test
	public void testG3() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            InputStream is = new ByteArrayInputStream(new byte[]{});
            ((TestsServiceImpl) testsService).pushTestsResult(is, "", null);
        });
    }

	@Test
	public void testG4() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            InputStream is = new ByteArrayInputStream(new byte[]{});
            ((TestsServiceImpl) testsService).pushTestsResult(is, "some", null);
        });
    }

	@Test
	public void testG5() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            InputStream is = new ByteArrayInputStream(new byte[]{});
            ((TestsServiceImpl) testsService).pushTestsResult(is, "some", "");
        });
    }

	@Test
	public void testH1() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            ((TestsServiceImpl) testsService).enqueuePushTestsResult(null, null, null);
        });
    }

	@Test
	public void testH2() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            testsService.enqueuePushTestsResult("", null, null);
        });
    }

	@Test
	public void testH3() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            testsService.enqueuePushTestsResult("some", null, null);
        });
    }

	@Test
	public void testH4() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestsService testsService = client.getTestsService();
            testsService.enqueuePushTestsResult("some", "", null);
        });
    }
}
