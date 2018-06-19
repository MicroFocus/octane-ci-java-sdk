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

package com.hp.octane.integrations.dto;

import com.hp.octane.integrations.dto.general.CIServerTypes;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Basic tests to verify if every DTO is registered and available for serialization
 */

public class DTOFactoryTest {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test
	public void test_All_DTOs() {
		//  CIEventCauseType

		//  CIEventCause

		//  CIProxyConfiguration

		//  OctaneConfiguration

		//  HttpMethod

		//  OctaneRequest

		//  OctaneResponse

		//  OctaneResultAbridged

		//  OctaneTaskAbridged

		//  BuildConverage

		//  FileCoverage

		//  LineCoverage

		//  TestCoverage

		//  CIEventType

		//  PhaseType

		//  CIEvent

		//  CIEventsList

		//  CIServerTypes

		//  CIJobsList

		//  CIPluginInfo

		//  CIPluginSDKInfo

		//  CIProviderSummaryInfo

		//  CIServerInfo

		//  CIParameterType

		//  CIParameter

		//  PipelineNode

		//  PipelinePhase

		//  SCMType

		//  SCMChange

		//  SCMCommit

		//  SCMData

		//  SCMRepository

		//  CIBuildResult

		//  CIBuildStatus

		//  SnapshotNode

		//  SnapshotPhase

		//  TestRunResult

		//  BuildContext

		//  Property

		//  TestCase

		//  TestRun

		//  TestRunError

		//  TestsResult

		//  TestSuite
	}

	@Test
	public void test_A() {
		CIPluginInfo CIPluginInfo = dtoFactory.newDTO(CIPluginInfo.class);
		assertNotNull(CIPluginInfo);
		assertNull(CIPluginInfo.getVersion());

		CIPluginInfo newRef = CIPluginInfo.setVersion("1.2.3");
		assertNotNull(newRef);
		assertEquals(newRef, CIPluginInfo);
		assertEquals("1.2.3", CIPluginInfo.getVersion());
		assertEquals("1.2.3", newRef.getVersion());

		String jsonA = dtoFactory.dtoToJson(CIPluginInfo);
		String jsonB = dtoFactory.dtoToJson(newRef);
		assertEquals(jsonA, jsonB);

		CIPluginInfo CIPluginInfoImplDes = dtoFactory.dtoFromJson(jsonA, CIPluginInfo.class);
		assertNotNull(CIPluginInfoImplDes);
		assertEquals("1.2.3", CIPluginInfoImplDes.getVersion());
	}

	@Test
	public void test_B() {
		CIServerInfo ciServerInfo = dtoFactory.newDTO(CIServerInfo.class)
				.setType(CIServerTypes.JENKINS.value())
				.setInstanceId("instance id")
				.setInstanceIdFrom(123456789L)
				.setSendingTime(123456789L)
				.setUrl("http://localhost:8080")
				.setVersion("1.2.3");

		assertEquals(CIServerTypes.JENKINS.value(), ciServerInfo.getType());
		assertEquals("instance id", ciServerInfo.getInstanceId());
		assertEquals((Long) 123456789L, ciServerInfo.getInstanceIdFrom());
		assertEquals((Long) 123456789L, ciServerInfo.getSendingTime());
		assertEquals("http://localhost:8080", ciServerInfo.getUrl());
		assertEquals("1.2.3", ciServerInfo.getVersion());

		String json = dtoFactory.dtoToJson(ciServerInfo);
	}

	@Test
	public void test_C() {
		List<CIServerInfo> coll = new ArrayList<>();
		CIServerInfo instA = dtoFactory.newDTO(CIServerInfo.class)
				.setType(CIServerTypes.JENKINS.value())
				.setInstanceId("instance id A")
				.setInstanceIdFrom(123456789L)
				.setSendingTime(123456789L)
				.setUrl("http://localhost:8080/A")
				.setVersion("1.2.3");
		CIServerInfo instB = dtoFactory.newDTO(CIServerInfo.class)
				.setType(CIServerTypes.JENKINS.value())
				.setInstanceId("instance id B")
				.setInstanceIdFrom(123456789L)
				.setSendingTime(123456789L)
				.setUrl("http://localhost:8080/B")
				.setVersion("1.2.4");
		CIServerInfo instC = dtoFactory.newDTO(CIServerInfo.class)
				.setType(CIServerTypes.JENKINS.value())
				.setInstanceId("instance id C")
				.setInstanceIdFrom(123456789L)
				.setSendingTime(123456789L)
				.setUrl("http://localhost:8080/C")
				.setVersion("1.2.5");
		coll.add(instA);
		coll.add(instB);
		coll.add(instC);

		String json = dtoFactory.dtoCollectionToJson(coll);

		assertNotNull(json);

		CIServerInfo[] newColl = dtoFactory.dtoCollectionFromJson(json, CIServerInfo[].class);
		assertNotNull(newColl);
		assertEquals(3, newColl.length);
	}
}
