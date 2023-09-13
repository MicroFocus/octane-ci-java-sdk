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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * OctaneTaskAbridged test
 */

public class OctaneRequestTest {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test(expected = IllegalArgumentException.class)
	public void testA() {
		dtoFactory.newDTO(OctaneRequest.class).setUrl(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testB() {
		dtoFactory.newDTO(OctaneRequest.class).setUrl("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC() {
		dtoFactory.newDTO(OctaneRequest.class).setUrl("some non valid url");
	}

	@Test
	public void testD() {
		String validURL = "http://localhost:8080/something?param=value";
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class).setUrl(validURL);
		assertEquals(validURL, request.getUrl());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testE() {
		dtoFactory.newDTO(OctaneRequest.class).setMethod(null);
	}

	@Test
	public void testF1() throws IOException {
		String body = "body עברית русский";
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class).setBody(body);
		assertEquals(body, isToString(request.getBody()));
	}

	@Test
	public void testF2() throws IOException {
		String body = "body עברית русский";
		OctaneRequest request = dtoFactory.newDTO(OctaneRequest.class).setBody(new ByteArrayInputStream(body.getBytes(Charset.forName("UTF-8"))));
		assertEquals(body, isToString(request.getBody()));
	}

	@Test
	public void testG() throws IOException {
		OctaneRequest request = dtoFactory.dtoFromJson("{\"method\":\"GET\",\"url\":\"http://localhost:8080\",\"body\":\"something\"}", OctaneRequest.class);
		assertEquals(HttpMethod.GET, request.getMethod());
		assertEquals("http://localhost:8080", request.getUrl());
		assertEquals("something", isToString(request.getBody()));
	}

	private String isToString(InputStream stream) throws IOException {
		int l;
		byte[] buffer = new byte[2048];
		StringBuilder result = new StringBuilder();
		while ((l = stream.read(buffer, 0, 2048)) > 0) {
			result.append(new String(buffer, 0, l, Charset.forName("UTF-8")));
		}
		return result.toString();
	}
}
