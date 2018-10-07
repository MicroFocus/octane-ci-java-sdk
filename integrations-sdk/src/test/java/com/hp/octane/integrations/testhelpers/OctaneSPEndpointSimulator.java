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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Each Octane Shared Space Endpoint simulator instance will function as an isolated context for tests targeting specific shared space
 * There can be unlimited number of such an endpoints
 * Each instance is thread and scope safe
 */

public class OctaneSPEndpointSimulator extends AbstractHandler {
	private static final Logger logger = LogManager.getLogger(OctaneSPEndpointSimulator.class);

	//  simulator's factory static content
	//
	private static final int DEFAULT_PORT = 3333;
	private static final Map<String, OctaneSPEndpointSimulator> serverSimulators = new LinkedHashMap<>();
	private static Server server;
	private static HandlerCollection handlers;
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
			handlers = new HandlerCollection(true);
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
	private final Map<String, Consumer<Request>> apiHandlersRegistry = new LinkedHashMap<>();
	private final String sp;

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
	public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
		if (request.isHandled()) {
			return;
		}

		if (signInApiPattern.matcher(s).matches()) {
			OctaneSecuritySimulationUtils.signIn(request);
			return;
		}

		if (!OctaneSecuritySimulationUtils.authenticate(request)) {
			return;
		}

		if (!s.startsWith("/api/shared_spaces/" + sp + "/") && !s.startsWith("/internal-api/shared_spaces/" + sp + "/")) {
			return;
		}

		apiHandlersRegistry.keySet().stream()
				.filter(apiHandlerKey -> {
					String[] keyParts = apiHandlerKey.split(API_HANDLER_KEY_JOINER);
					return request.getMethod().compareTo(keyParts[0]) == 0 && Pattern.compile(keyParts[1]).matcher(s).matches();
				})
				.findFirst()
				.ifPresent(apiPattern -> {
					apiHandlersRegistry.get(apiPattern).accept(request);
					request.setHandled(true);
				});

		if (!request.isHandled()) {
			request.getResponse().setStatus(HttpStatus.SC_NOT_FOUND);
			request.setHandled(true);
		}
	}

	public void installApiHandler(HttpMethod method, String pattern, Consumer<Request> apiHandler) {
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
		installApiHandler(HttpMethod.GET, "^.*/analytics/ci/servers/connectivity/status$", request -> {
			request.getResponse().setStatus(HttpStatus.SC_OK);
			request.setHandled(true);
		});
	}

	private void installNOOPTasksApiHandler() {
		installApiHandler(HttpMethod.GET, "^.*tasks$", request -> {
			CIPluginSDKUtils.doWait(3000);
			request.getResponse().setStatus(HttpStatus.SC_NO_CONTENT);
			request.setHandled(true);
		});
	}
}
