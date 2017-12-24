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

package com.hp.octane.integrations.dto;

import com.hp.octane.integrations.dto.general.CIProviderSummaryInfo;
import com.hp.octane.integrations.dto.general.CIServerTypes;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Status Info tests.
 */

public class StatusInfoTest {
	private final static DTOFactory dtoFactory = DTOFactory.getInstance();
	private final static String PLUGIN_VERSION = "2.3.4";
	private final static String SERVER_VERION = "1.2.3";
	private final static String INPUT_SERVER_URL = "http://some.url/";
	private final static String EXPECTED_SERVER_URL = "http://some.url";
	private final static String SERVER_UUID = UUID.randomUUID().toString();
	private final static Long SERVER_UUID_FROM = System.currentTimeMillis();
	private final static Long SYNC_TIME = System.currentTimeMillis();

	@Test
	public void testA() {
		CIProviderSummaryInfo statusInfo = dtoFactory.newDTO(CIProviderSummaryInfo.class);

		CIPluginInfo CIPluginInfo = dtoFactory.newDTO(CIPluginInfo.class)
				.setVersion(PLUGIN_VERSION);

		CIServerInfo CIServerInfo = dtoFactory.newDTO(CIServerInfo.class)
				.setType(CIServerTypes.JENKINS.value())
				.setVersion(SERVER_VERION)
				.setInstanceId(SERVER_UUID)
				.setInstanceIdFrom(SERVER_UUID_FROM)
				.setSendingTime(SYNC_TIME)
				.setUrl(INPUT_SERVER_URL);

		statusInfo.setPlugin(CIPluginInfo);
		statusInfo.setServer(CIServerInfo);

		String json = dtoFactory.dtoToJson(statusInfo);

		CIProviderSummaryInfo newStatus = dtoFactory.dtoFromJson(json, CIProviderSummaryInfo.class);

		assertNotNull(newStatus);

		assertNotNull(newStatus.getPlugin());
		assertEquals(PLUGIN_VERSION, newStatus.getPlugin().getVersion());

		assertNotNull(newStatus.getServer());
		assertEquals(CIServerTypes.JENKINS.value(), newStatus.getServer().getType());
		assertEquals(SERVER_VERION, newStatus.getServer().getVersion());
		assertEquals(SERVER_UUID, newStatus.getServer().getInstanceId());
		assertEquals(SERVER_UUID_FROM, newStatus.getServer().getInstanceIdFrom());
		assertEquals(SYNC_TIME, newStatus.getServer().getSendingTime());
		assertEquals(EXPECTED_SERVER_URL, newStatus.getServer().getUrl());
	}
}
