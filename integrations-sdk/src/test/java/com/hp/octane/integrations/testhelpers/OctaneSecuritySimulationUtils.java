/*
 * Copyright 2017-2025 Open Text
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
package com.hp.octane.integrations.testhelpers;

import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class OctaneSecuritySimulationUtils {
	private static final String SECURITY_COOKIE_NAME = "LWSSO_COOKIE_KEY";
	private static final String SECURITY_TOKEN_SEPARATOR = ":";

	private OctaneSecuritySimulationUtils() {
	}

	static void signIn(Request request, Response response) throws IOException {
		String body = Content.Source.asString(request, StandardCharsets.UTF_8);
		Map json = CIPluginSDKUtils.getObjectMapper().readValue(body, Map.class);
		String client = (String) json.get("client_id");
		String secret = (String) json.get("client_secret");
		response.getHeaders().add(HttpHeader.SET_COOKIE, buildSetCookieHeader(client, secret));
	}

	static boolean authenticate(Request request, Response response) {
		List<HttpCookie> cookies = Request.getCookies(request);
		if (cookies != null) {
			for (HttpCookie cookie : cookies) {
				if (SECURITY_COOKIE_NAME.equals(cookie.getName())) {
					String[] securityItems = cookie.getValue().split(SECURITY_TOKEN_SEPARATOR);
					long issuedAt = Long.parseLong(securityItems[2]);
					if (System.currentTimeMillis() - issuedAt > 2000) {
						response.getHeaders().add(HttpHeader.SET_COOKIE, buildSetCookieHeader(securityItems[0], securityItems[1]));
					}
					return true;
				}
			}
		}
		response.setStatus(HttpStatus.SC_UNAUTHORIZED);
		return false;
	}

	static private String buildSetCookieHeader(String client, String secret) {
		String value = String.join(SECURITY_TOKEN_SEPARATOR, client, secret, String.valueOf(System.currentTimeMillis()));
		return SECURITY_COOKIE_NAME + "=" + value + "; Path=/; HttpOnly";
	}
}
