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

package com.hp.octane.integrations.dto.connectivity;

import com.hp.octane.integrations.dto.DTOFactory;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * OctaneTaskAbridged test
 */

public class OctaneRequestTest {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test(expected = IllegalArgumentException.class)
	public void test_A() {
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setUrl(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_B() {
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setUrl("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_C() {
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setUrl("some non valid url");
	}

	@Test
	public void test_D() {
		String validURL = "http://localhost:8080/something?param=value";
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setUrl(validURL);
		assertEquals(validURL, request.getUrl());
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_E() {
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setMethod(null);
	}

	@Test
	public void test_F() throws IOException {
		String body = "body";
		String validURL = "http://localhost:8080/something?param=value";
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class)
				.setBody(body);
		assertEquals(body, isToString(request.getBody()));
	}

	@Test
	public void test_G() throws IOException {
		OctaneRequest request = dtoFactory.dtoFromJson("{\"method\":\"GET\",\"url\":\"http://localhost:8080\",\"body\":\"something\"}", OctaneRequest.class);
		assertEquals(HttpMethod.GET, request.getMethod());
		assertEquals("http://localhost:8080", request.getUrl());
		assertEquals("something", isToString(request.getBody()));
	}

	private String isToString(InputStream stream) throws IOException {
		int l;
		byte[] buffer = new byte[2048];
		String result = "";
		while ((l = stream.read(buffer, 0, 2048)) > 0) {
			result += new String(buffer, 0, l);
		}
		return result;
	}
}
