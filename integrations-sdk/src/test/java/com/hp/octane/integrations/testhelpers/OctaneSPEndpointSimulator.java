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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Each Octane Shared Space Endpoint simulator instance will function as an isolated context for tests targeting specific shared space
 * There can be unlimited number of such an endpoints
 * Each instance is thread and scope safe
 */

public class OctaneSPEndpointSimulator extends Handler.Abstract {
	private static final Logger logger = LogManager.getLogger(OctaneSPEndpointSimulator.class);

	//  simulator's factory static content
	//
	private static final int DEFAULT_PORT = 3333;
	private static final Map<String, OctaneSPEndpointSimulator> serverSimulators = new LinkedHashMap<>();
	private static Server server;
	private static Handler.Sequence handlers;
	private static Integer selectedPort;

	/**
	 * Entry point to obtain Octane Server Simulator dedicated instance
	 *
	 * @param sharedSpaceId shared space ID will define uniqueness of instance
	 * @return initialize Octane Server Simulator
	 */
	synchronized public static OctaneSPEndpointSimulator addInstance(String sharedSpaceId) {
		if (sharedSpaceId == null || sharedSpaceId.isEmpty()) {
			throw new IllegalArgumentException("shared space ID MUST NOT be null nor empty");
		}

		if (server == null) {
			startServer();
		}
		return serverSimulators.computeIfAbsent(sharedSpaceId, OctaneSPEndpointSimulator::new);
	}

	/**
	 * Entry point to remove Server Simulator instance
	 * it is HIGHLY advised to clean up instances as a best practice, although there should be no harm if all the instances left intact to the end of the test suite run
	 *
	 * @param sharedSpaceId shared space ID identifier of the needed simulator's instance
	 */
	synchronized public static void removeInstance(String sharedSpaceId) {
		if (sharedSpaceId == null || sharedSpaceId.isEmpty()) {
			throw new IllegalArgumentException("shared space ID MUST NOT be null nor empty");
		}

		Handler ossAsHandler = serverSimulators.get(sharedSpaceId);
		if (ossAsHandler != null) {
			ossAsHandler.destroy();
			handlers.removeHandler(ossAsHandler);
			serverSimulators.remove(sharedSpaceId);
		}
	}

	/**
	 * Despite of the fact that different instances will simulate scoped contexts, the actual Jetty server handling all requests will be one
	 * This method returns its actual PORT
	 *
	 * @return effectively selected server port
	 */
	synchronized public static String getSimulatorUrl() {
		if (selectedPort == null) {
			startServer();
		}
		return "http://localhost:" + selectedPort;
	}

	private static void startServer() {
		String rawPort = System.getProperty("octane.server.simulator.port");
		server = new Server(rawPort == null ? (selectedPort = DEFAULT_PORT) : (selectedPort = Integer.parseInt(rawPort)));
		try {
			handlers = new Handler.Sequence();
			server.setHandler(handlers);
			server.start();
			logger.info("SUCCESSFULLY started, listening on port " + selectedPort);
		} catch (Exception e) {
			throw new RuntimeException("failed to start embedded Jetty", e);
		}
	}

	//  particular simulator instance's logic
	//  each instance will add its own request handler (self), which will work in a specific shared space context
	//
	private final String API_HANDLER_KEY_JOINER = " # ";
	private final Pattern signInApiPattern = Pattern.compile("/authentication/sign_in");
	private final Map<String, BiConsumer<Request, Response>> apiHandlersRegistry = new LinkedHashMap<>();
	private final String sp;
	private String octaneVersion = "15.1.1";

	//  Jetty 12: the request callback must be passed to the async body write, otherwise the response
	//  is finalized before the body is flushed. We expose it to the (callback-less) API handlers via ThreadLocal.
	private static final ThreadLocal<Callback> CURRENT_CALLBACK = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> RESPONSE_WRITTEN = new ThreadLocal<>();

	private OctaneSPEndpointSimulator(String sp) {
		this.sp = sp;

		//  install default API handlers
		installNOOPTasksApiHandler();
		installDefaultConnectivityStatusApiHandler();

		handlers.addHandler(this);
	}

	public String getSharedSpaceId() {
		return sp;
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		String pathInfo = Request.getPathInContext(request);
		String method = request.getMethod();

		if (signInApiPattern.matcher(pathInfo).matches()) {
			OctaneSecuritySimulationUtils.signIn(request, response);
			callback.succeeded();
			return true;
		}

		if (!OctaneSecuritySimulationUtils.authenticate(request, response)) {
			callback.succeeded();
			return true;
		}

		if (!pathInfo.startsWith("/api/shared_spaces/" + sp + "/") && !pathInfo.startsWith("/internal-api/shared_spaces/" + sp + "/")) {
			return false;
		}

		BiConsumer<Request, Response> apiHandler = apiHandlersRegistry.entrySet().stream()
				.filter(entry -> {
					String[] keyParts = entry.getKey().split(API_HANDLER_KEY_JOINER);
					return method.compareTo(keyParts[0]) == 0 && Pattern.compile(keyParts[1]).matcher(pathInfo).matches();
				})
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(null);

		if (apiHandler != null) {
			CURRENT_CALLBACK.set(callback);
			RESPONSE_WRITTEN.set(Boolean.FALSE);
			try {
				apiHandler.accept(request, response);
			} finally {
				boolean written = Boolean.TRUE.equals(RESPONSE_WRITTEN.get());
				CURRENT_CALLBACK.remove();
				RESPONSE_WRITTEN.remove();
				//  if the handler wrote a body, the write already owns the callback; otherwise complete it here
				if (!written) {
					callback.succeeded();
				}
			}
			return true;
		}

		response.setStatus(HttpStatus.SC_NOT_FOUND);
		callback.succeeded();
		return true;
	}

	/**
	 * Writes a response body and completes the current request callback (Jetty 12 async-safe).
	 * API handlers MUST use this instead of {@code Content.Sink.write(..., Callback.NOOP)} when returning a body.
	 */
	public static void writeResponseBody(Response response, String content) {
		Callback cb = CURRENT_CALLBACK.get();
		RESPONSE_WRITTEN.set(Boolean.TRUE);
		Content.Sink.write(response, true, content, cb != null ? cb : Callback.NOOP);
	}

	/**
	 * Reads the full request body as a UTF-8 string, transparently gunzipping when Content-Encoding is gzip.
	 * Jetty 12's {@code Content.Source.asString(UTF_8)} strictly validates UTF-8 and fails on binary (gzip) bodies,
	 * so we read raw bytes first.
	 */
	public static String readRequestBody(Request request) {
		try (InputStream is = Content.Source.asInputStream(request)) {
			byte[] bytes = is.readAllBytes();
			if ("gzip".equalsIgnoreCase(request.getHeaders().get("Content-Encoding"))) {
				return CIPluginSDKUtils.inputStreamToUTF8String(new GZIPInputStream(new ByteArrayInputStream(bytes)));
			}
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void installApiHandler(HttpMethod method, String pattern, BiConsumer<Request, Response> apiHandler) {
		String handlerKey = method + API_HANDLER_KEY_JOINER + pattern;
		if (apiHandlersRegistry.containsKey(handlerKey)) {
			logger.warn("api handler for '" + handlerKey + "' already installed and will be replaced");
		}
		apiHandlersRegistry.put(handlerKey, apiHandler);
	}

	public void removeApiHandler(HttpMethod method, String pattern) {
		apiHandlersRegistry.remove(method + API_HANDLER_KEY_JOINER + pattern);
	}

	private void installDefaultConnectivityStatusApiHandler() {
		installApiHandler(HttpMethod.GET, "^.*/analytics/ci/servers/connectivity/status$", (request, response) -> {
			response.setStatus(HttpStatus.SC_OK);
			String msg = "{\"supportedSdkVersion\": \"1.0.0\", \"octaneVersion\": \"" + octaneVersion + "\"}";
			response.getHeaders().put("Content-Type", "application/json");
			writeResponseBody(response, msg);
		});
	}

	private void installNOOPTasksApiHandler() {
		installApiHandler(HttpMethod.GET, "^.*tasks$", (request, response) -> {
			CIPluginSDKUtils.doWait(3000);
			response.setStatus(HttpStatus.SC_NO_CONTENT);
		});
	}

	public String getOctaneVersion() {
		return octaneVersion;
	}

	public void setOctaneVersion(String octaneVersion) {
		this.octaneVersion = octaneVersion;
	}
}
