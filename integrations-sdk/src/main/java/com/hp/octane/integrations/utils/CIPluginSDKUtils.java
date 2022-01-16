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

package com.hp.octane.integrations.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.general.OctaneConnectivityStatus;
import com.hp.octane.integrations.services.configurationparameters.EncodeCiJobBase64Parameter;
import org.apache.commons.codec.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
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
	 * @param nonProxyHostsStr list of hosts, separated by the '|' character. The wildcard '*' can be used. Example: localhost|*.mydomain.com)
	 * @return result of verification
	 */
	public static boolean isNonProxyHost(String targetHost, String nonProxyHostsStr) {
		boolean nonProxyHost = false;
		List<Pattern> nonProxyHostPatterns = new LinkedList<>();
		if (nonProxyHostsStr != null && !nonProxyHostsStr.isEmpty()) {
			nonProxyHostsStr = nonProxyHostsStr.replaceAll("^(\'|\")|(\'|\")$", "");
			String[] nonProxyHosts = nonProxyHostsStr.split("[ \t\n,|]+");
			for (String nph : nonProxyHosts) {
				if (!nph.isEmpty()) {
					nonProxyHostPatterns.add(Pattern.compile(nph.replace(".", "\\.").replace("*", ".*")));
				}
			}
		}
		for (Pattern pattern : nonProxyHostPatterns) {
			if (pattern.matcher(targetHost).find()) {
				nonProxyHost = true;
				break;
			}
		}
		return nonProxyHost;
	}

	/**
	 * parses string as URL
	 *
	 * @param input valid URL sting
	 * @return parsed URL
	 * @throws IllegalArgumentException exception on any incorrect URL input
	 */
	public static URL parseURL(String input) {
		try {
			return new URL(input);
		} catch (MalformedURLException murle) {
			throw new IllegalArgumentException("failed to parse '" + input + "' as URL", murle);
		}
	}

	public static CIProxyConfiguration getProxyConfiguration(String url, OctaneSDK.SDKServicesConfigurer configurer) {
		Function<URL, CIProxyConfiguration> proxySupplier;
		if (configurer != null) {
			proxySupplier = configurer.pluginServices::getProxyConfiguration;
		} else if (OctaneSDK.hasClients()) {
			proxySupplier = OctaneSDK.getClients().get(0).getRestService().getProxySupplier();
		} else {
			return null;
		}
		URL octaneUrl = CIPluginSDKUtils.parseURL(url);
		return proxySupplier.apply(octaneUrl);
	}

	/**
	 * encodes string as path param
	 * - this method WILL NOT fail on wrong input, but just return it as is, writing the error to the log
	 * - although caught in the method, we do NOT expect to fail on UnsupportedEncodingException
	 *
	 * @param input input that is intended to be used as path param
	 * @return encoded string or the same input if NULL passed or failed otherwise
	 */
	public static String urlEncodePathParam(String input) {
		String result = input;
		if (input != null) {
			try {
				result = URLEncoder.encode(input, StandardCharsets.UTF_8.name()).replace("+", "%20");
			} catch (UnsupportedEncodingException uee) {
				logger.error("failed to URL encode '" + input + "', continuing with unchanged original value", uee);
			}
		}
		return result;
	}

	public static String urlEncodeBase64(String input) {
		String result = input;
		if (input != null) {
			result = Base64.getUrlEncoder().encodeToString(input.getBytes(Charsets.UTF_8));
		}
		return result;
	}

	public static String addParameterEncode64ToUrl(String url) {
		String tmp = url + (url.contains("?") ? "&" : "?") + EncodeCiJobBase64Parameter.OCTANE_PARAMETER + "=" + EncodeCiJobBase64Parameter.OCTANE_PARAMETER_VALUE;
		return tmp;
	}


	/**
	 * encodes string as query param
	 * - this method WILL NOT fail on wrong input, but just return it as is, writing the error to the log
	 * - although caught in the method, we do NOT expect to fail on UnsupportedEncodingException
	 *
	 * @param input input that is intended to be used as query param
	 * @return encoded string or the same input if NULL passed or failed otherwise
	 */
	public static String urlEncodeQueryParam(String input) {
		String result = input;
		if (input != null) {
			try {
				result = URLEncoder.encode(input, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException uee) {
				logger.error("failed to URL encode '" + input + "', continuing with unchanged original value", uee);
			}
		}
		return result;
	}

	public static String inputStreamToUTF8String(InputStream is) throws IOException {
		return inputStreamToString(is, StandardCharsets.UTF_8);
	}

	public static String inputStreamToString(InputStream is, Charset charset) throws IOException {
		if (is == null) {
			throw new IllegalArgumentException("input stream MUST NOT be null");
		}
		if (charset == null) {
			throw new IllegalArgumentException("charset MUST NOT be null");
		}

		ByteArrayOutputStream result = new ByteArrayOutputStream();
		int readLen;
		byte[] buffer = new byte[4096];
		while ((readLen = is.read(buffer)) != -1) result.write(buffer, 0, readLen);
		result.flush();
		return result.toString(charset.name());
	}


	public static int compareStringVersion(String version1, String version2) {
		String[] arr1 = version1.split("\\.");
		String[] arr2 = version2.split("\\.");

		int i = 0;
		while (i < arr1.length || i < arr2.length) {
			if (i < arr1.length && i < arr2.length) {
				if (Integer.parseInt(arr1[i]) < Integer.parseInt(arr2[i])) {
					return -1;
				} else if (Integer.parseInt(arr1[i]) > Integer.parseInt(arr2[i])) {
					return 1;
				}
			} else if (i < arr1.length) {
				if (Integer.parseInt(arr1[i]) != 0) {
					return 1;
				}
			} else if (i < arr2.length) {
				if (Integer.parseInt(arr2[i]) != 0) {
					return -1;
				}
			}

			i++;
		}

		return 0;
	}

	public static boolean isSdkSupported(OctaneConnectivityStatus octaneConnectivityStatus) {
		try {
			return (octaneConnectivityStatus.getSupportedSdkVersion() == null ||
					compareStringVersion(OctaneSDK.SDK_VERSION, octaneConnectivityStatus.getSupportedSdkVersion()) >= 0);
		} catch (Exception e) {
			logger.error("unable to compare plugin SDK version: " + OctaneSDK.SDK_VERSION + " with Supported SDK version: " + octaneConnectivityStatus.getSupportedSdkVersion() + ". " + e.getMessage());
			return true;
		}
	}

	public static boolean isServiceTemporaryUnavailable(String body) {
		return body != null && body.contains("Service Temporar");
		//Service Temporary Unavailable
		//Service Temporarily Unavailable"
	}

	/**
	 *
	 * @param jobId current job id
	 * @param causes causes that called to jobId
	 * @param parents parents - will contains final list of  parents
	 */
	public static void getRootJobCiIds(String jobId, List<CIEventCause> causes , Set<String> parents) {
		if (causes != null) {
			for (CIEventCause cause : causes) {
				if (CIEventCauseType.UPSTREAM.equals(cause.getType())) {
					getRootJobCiIds(cause.getProject(), cause.getCauses() , parents);
				} else {
					if (jobId != null && !jobId.isEmpty() && parents!=null) {
						parents.add(jobId);
					}
				}
			}
		}
	}

	public static String getNextCorrelationId(){
		return UUID.randomUUID().toString().replace("-","").substring(0,25);
	}
}

