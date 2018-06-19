/*
 *     © 2017 EntIT Software LLC, a Micro Focus company, L.P.
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

package com.hp.octane.integrations.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by lazara on 08/06/2017.
 */

public class CIPluginSDKUtils {
	private static final Logger logger = LogManager.getLogger(CIPluginSDKUtils.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public static void doWait(long period) {
		try {
			Thread.sleep(period);
		} catch (InterruptedException ie) {
			logger.warn("interrupted while doing breakable wait");
		}
	}

	public static boolean isNonProxyHost(String targetHost, String nonProxyHostsStr) {
		boolean noProxyHost = false;
		for (Pattern pattern : getNoProxyHostPatterns(nonProxyHostsStr)) {
			if (pattern.matcher(targetHost).find()) {
				noProxyHost = true;
				break;
			}
		}
		return noProxyHost;
	}

	public static String urlEncodePathParam(String input) {
		String result = input;
		try {
			result = URLEncoder.encode(input, StandardCharsets.UTF_8.name()).replace("+", "%20");
		} catch (UnsupportedEncodingException uee) {
			logger.error("failed to URL encode '" + input + "', continuing with unchanged original value", uee);
		}
		return result;
	}

	public static String urlEncodeQueryParam(String input) {
		String result = input;
		try {
			result = URLEncoder.encode(input, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException uee) {
			logger.error("failed to URL encode '" + input + "', continuing with unchanged original value", uee);
		}
		return result;
	}

	private static List<Pattern> getNoProxyHostPatterns(String noProxyHost) {
		List<Pattern> result = new LinkedList<>();
		if (noProxyHost != null && !noProxyHost.isEmpty()) {
			String[] hosts = noProxyHost.split("[ \t\n,|]+");
			for (String host : hosts) {
				if (!host.isEmpty()) {
					result.add(Pattern.compile(host.replace(".", "\\.").replace("*", ".*")));
				}
			}
		}
		return result;
	}
}

