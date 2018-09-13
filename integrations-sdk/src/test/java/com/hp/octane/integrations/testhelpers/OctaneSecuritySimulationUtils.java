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
 */

package com.hp.octane.integrations.testhelpers;

import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class OctaneSecuritySimulationUtils {
	private static final String SECURITY_COOKIE_NAME = "LWSSO_COOKIE_KEY";
	private static final String SECURITY_TOKEN_SEPARATOR = ":";

	private OctaneSecuritySimulationUtils() {
	}

	static void signIn(Request request) throws IOException {
		String body = CIPluginSDKUtils.inputStreamToUTF8String(request.getInputStream());
		Map json = CIPluginSDKUtils.getObjectMapper().readValue(body, Map.class);
		String client = (String) json.get("client_id");
		String secret = (String) json.get("client_secret");
		Cookie securityCookie = createSecurityCookie(client, secret);
		request.getResponse().addCookie(securityCookie);
		request.setHandled(true);
	}

	static boolean authenticate(Request request) {
		for (Cookie cookie : request.getCookies()) {
			if (SECURITY_COOKIE_NAME.equals(cookie.getName())) {
				String[] securityItems = cookie.getValue().split(SECURITY_TOKEN_SEPARATOR);
				long issuedAt = Long.parseLong(securityItems[2]);
				if (System.currentTimeMillis() - issuedAt > 2000) {
					Cookie securityCookie = createSecurityCookie(securityItems[0], securityItems[1]);
					request.getResponse().addCookie(securityCookie);
				}
				return true;
			}
		}
		request.getResponse().setStatus(HttpStatus.SC_UNAUTHORIZED);
		request.setHandled(true);
		return false;
	}

	static private Cookie createSecurityCookie(String client, String secret) {
		Cookie result = new Cookie(
				SECURITY_COOKIE_NAME,
				String.join(SECURITY_TOKEN_SEPARATOR, Stream.of(client, secret, String.valueOf(System.currentTimeMillis())).collect(Collectors.toList()))
		);
		result.setHttpOnly(true);
		result.setDomain(".localhost");
		return result;
	}
}
