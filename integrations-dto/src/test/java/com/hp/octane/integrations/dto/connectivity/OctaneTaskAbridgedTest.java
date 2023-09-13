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

package com.hp.octane.integrations.dto.connectivity;

import com.hp.octane.integrations.dto.DTOFactory;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * OctaneTaskAbridged test
 */

public class OctaneTaskAbridgedTest {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test
	public void test_A() {
		String id = UUID.randomUUID().toString();
		String serviceId = UUID.randomUUID().toString();
		String url = "http://non-existing/url";
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("content-type", "application/json");
		String body = "{}";

		OctaneTaskAbridged task = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(id)
				.setServiceId(serviceId)
				.setUrl(url)
				.setMethod(HttpMethod.GET)
				.setHeaders(headers)
				.setBody(body);

		assertNotNull(task);

		String json = dtoFactory.dtoToJson(task);
		assertNotNull(json);
		assertFalse(json.isEmpty());

		OctaneTaskAbridged deTask = dtoFactory.dtoFromJson(json, OctaneTaskAbridged.class);
		assertNotNull(deTask);
		assertEquals(id, deTask.getId());
		assertEquals(serviceId, deTask.getServiceId());
		assertEquals(url, deTask.getUrl());
		assertEquals(HttpMethod.GET, deTask.getMethod());
		assertNotNull(deTask.getHeaders());
		assertEquals(1, deTask.getHeaders().size());
		assertEquals("application/json", deTask.getHeaders().get("content-type"));
		assertEquals(body, deTask.getBody());
	}
}
