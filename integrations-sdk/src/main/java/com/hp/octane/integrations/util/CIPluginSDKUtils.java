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

package com.hp.octane.integrations.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
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

	/**
	 * This function will safely sleep the current thread for the specified amount of MILLIS
	 * NO exception thrown expected
	 *
	 * @param period millis to wait
	 */
	public static void doWait(long period) {
		if (period == 0) {
			throw new IllegalArgumentException("period MUST be higher than 0");
		}

		long started = System.currentTimeMillis();
		while (System.currentTimeMillis() - started < period) {
			try {
				Thread.sleep(period - (System.currentTimeMillis() - started));
			} catch (InterruptedException ie) {
				logger.warn("prematurely interrupted while waiting", ie);
			}
		}
	}

	/**
	 * This function will safely sleep the current thread for the specified amount of MILLIS OR until monitor is NOTIFIED - earliest of those
	 * NO exception thrown expected
	 *
	 * @param period  millis to wait in case the monitor stayed silent
	 * @param monitor notification object to interrupt the sleep on demand
	 */
	public static void doBreakableWait(long period, Object monitor) {
		if (period == 0) {
			throw new IllegalArgumentException("period MUST be higher than 0");
		}
		if (monitor == null) {
			throw new IllegalArgumentException("monitor MUST NOT be null");
		}

		long started = System.currentTimeMillis();
		while (System.currentTimeMillis() - started < period) {
			try {
				synchronized (monitor) {
					monitor.wait(period - (System.currentTimeMillis() - started));
				}
				break;
			} catch (InterruptedException ie) {
				logger.warn("prematurely interrupted while waiting", ie);
			}
		}
	}

	/**
	 * This function verifies if the target host matches one of the non-proxy hosts and returns boolean result for that
	 *
	 * @param targetHost       host of request
	 * @param nonProxyHostsStr list of hosts, separated by the '|' character. The wildcard '*' can be used:  localhost|*.mydomain.com)
	 * @return result of verification
	 */
	public static boolean isNonProxyHost(String targetHost, String nonProxyHostsStr) {
		boolean noProxyHost = false;
		List<Pattern> noProxyHosts = new LinkedList<>();
		if (nonProxyHostsStr != null && !nonProxyHostsStr.isEmpty()) {
			String[] hosts = nonProxyHostsStr.split("[ \t\n,|]+");
			for (String host : hosts) {
				if (!host.isEmpty()) {
					noProxyHosts.add(Pattern.compile(host.replace(".", "\\.").replace("*", ".*")));
				}
			}
		}
		for (Pattern pattern : noProxyHosts) {
			if (pattern.matcher(targetHost).find()) {
				noProxyHost = true;
				break;
			}
		}
		return noProxyHost;
	}

	public static URL parseURL(String input) {
		try {
			return new URL(input);
		} catch (MalformedURLException murle) {
			throw new OctaneSDKGeneralException("failed to extract host from URL '" + input + "'", murle);
		}
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
}

