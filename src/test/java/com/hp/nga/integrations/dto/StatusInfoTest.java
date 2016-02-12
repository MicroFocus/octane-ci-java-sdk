package com.hp.nga.integrations.dto;

import com.hp.nga.integrations.dto.general.CIProviderSummaryInfo;
import com.hp.nga.integrations.dto.general.CIServerTypes;
import com.hp.nga.integrations.dto.general.PluginInfo;
import com.hp.nga.integrations.dto.general.ServerInfo;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by gullery on 03/01/2016.
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

		PluginInfo pluginInfo = dtoFactory.newDTO(PluginInfo.class)
				.setVersion(PLUGIN_VERSION);

		ServerInfo serverInfo = dtoFactory.newDTO(ServerInfo.class)
				.setType(CIServerTypes.JENKINS)
				.setVersion(SERVER_VERION)
				.setInstanceId(SERVER_UUID)
				.setInstanceIdFrom(SERVER_UUID_FROM)
				.setSendingTime(SYNC_TIME)
				.setUrl(INPUT_SERVER_URL);

		statusInfo.setPlugin(pluginInfo);
		statusInfo.setServer(serverInfo);

		String json = dtoFactory.dtoToJson(statusInfo);

		CIProviderSummaryInfo newStatus = dtoFactory.dtoFromJson(json, CIProviderSummaryInfo.class);

		assertNotNull(newStatus);

		assertNotNull(newStatus.getPlugin());
		assertEquals(PLUGIN_VERSION, newStatus.getPlugin().getVersion());

		assertNotNull(newStatus.getServer());
		assertEquals(CIServerTypes.JENKINS, newStatus.getServer().getType());
		assertEquals(SERVER_VERION, newStatus.getServer().getVersion());
		assertEquals(SERVER_UUID, newStatus.getServer().getInstanceId());
		assertEquals(SERVER_UUID_FROM, newStatus.getServer().getInstanceIdFrom());
		assertEquals(SYNC_TIME, newStatus.getServer().getSendingTime());
		assertEquals(EXPECTED_SERVER_URL, newStatus.getServer().getUrl());
	}
}
