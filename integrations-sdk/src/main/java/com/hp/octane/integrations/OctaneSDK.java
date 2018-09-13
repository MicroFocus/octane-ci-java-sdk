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

package com.hp.octane.integrations;

import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * OctaneSDK serves initialization phase when hosting plugin configures OctaneClient/s to work with
 */

public final class OctaneSDK {
	private static final Logger logger = LogManager.getLogger(OctaneSDK.class);
	private static final Map<CIPluginServices, OctaneClient> clients = new LinkedHashMap<>();

	public static final Integer API_VERSION;
	public static final String SDK_VERSION;

	static {
		Properties p = new Properties();
		try {
			p.load(OctaneSDK.class.getClassLoader().getResourceAsStream("sdk.properties"));
			if (!p.isEmpty()) {
				API_VERSION = Integer.parseInt(p.getProperty("api.version"));
				SDK_VERSION = p.getProperty("sdk.version");
			} else {
				throw new IllegalStateException("SDK properties found to be empty (someone tampered with the binary? 'sdk.properties' resource has been overrode?)");
			}
		} catch (Throwable t) {
			logger.error("initialization failed: failed to load SDK properties", t);
			throw new IllegalStateException("OctaneSDK initialization failed: failed to load SDK properties", t);
		}
	}

	/**
	 * gateway to initialize an OctaneSDK instance/s
	 *
	 * @param pluginServices Object that implements the CIPluginServices interface. This object is actually a composite
	 *                       API of all the endpoints to be implemented by a hosting CI Plugin for ALM Octane use cases.
	 */
	synchronized public static OctaneClient addClient(CIPluginServices pluginServices) {
		if (pluginServices == null) {
			throw new IllegalArgumentException("initialization failed: MUST be initialized with valid plugin services provider");
		}
		if (pluginServices.getServerInfo() == null) {
			throw new IllegalArgumentException("plugin services MUST provide server info (found to be NULL)");
		}
		String instanceId = pluginServices.getServerInfo().getInstanceId();
		if (instanceId == null || instanceId.isEmpty()) {
			throw new IllegalArgumentException("plugin services's server info MUST provide instance ID which is not NULL nor empty");
		}
		if (clients.containsKey(pluginServices)) {
			throw new IllegalStateException("SDK instance configured with this ci plugin services instance is already present");
		}
		if (clients.values().stream().anyMatch(sdk -> instanceId.equals(sdk.getEffectiveInstanceId()))) {
			throw new IllegalStateException("SDK instance claiming for instance ID [" + instanceId + "] is already present");
		}

		OctaneClient newInstance = new OctaneClientImpl(new SDKServicesConfigurer(pluginServices));
		clients.put(pluginServices, newInstance);
		logger.info("SDK instance initialized SUCCESSFULLY");

		return newInstance;
	}

	/**
	 * provides all initialized OctaneClients
	 *
	 * @return OctaneClients; MAY NOT be NULL
	 */
	public static List<OctaneClient> getClients() {
		return new ArrayList<>(clients.values());
	}

	/**
	 * provides specific OctaneClient (the first one found) - claiming for specified instance ID
	 * attentions: since instance ID is not owned by OctaneClient and may be changed after the initialization, we may effectively have duplicate instance ID at some point of time
	 * if no instance found for the specified instance ID - IllegalStateException will be thrown
	 *
	 * @param instanceId instance ID of the desired client
	 * @return OctaneClient; MAY NOT be NULL
	 */
	public static OctaneClient getClient(String instanceId) throws IllegalStateException {
		if (instanceId == null || instanceId.isEmpty()) {
			throw new IllegalArgumentException("instance ID MUST NOT be null nor empty");
		}

		List<OctaneClient> result = new LinkedList<>();
		for (OctaneClient client : clients.values()) {
			if (instanceId.equals(client.getEffectiveInstanceId())) {
				result.add(client);
			}
		}

		if (result.size() == 1) {
			return result.get(0);
		} else if (result.isEmpty()) {
			throw new IllegalStateException("no client with instance ID [" + instanceId + "] present");
		} else {
			logger.warn("found more than 1 OctaneClient claiming for instance ID [" + instanceId + "], someone of them will be returned by this API");
			return result.get(0);
		}
	}

	/**
	 * provides specific OctaneClient - claiming for specified instance ID
	 * if no instance found for the specified instance ID - IllegalStateException will be thrown
	 *
	 * @param pluginServices instance of PluginServices
	 * @return OctaneClient; MAY NOT be NULL
	 */
	public static OctaneClient getClient(CIPluginServices pluginServices) throws IllegalStateException {
		if (pluginServices == null) {
			throw new IllegalArgumentException("plugin services parameter MUST NOT be null");
		}

		if (clients.containsKey(pluginServices)) {
			return clients.get(pluginServices);
		} else {
			throw new IllegalStateException("no client initialized with specified CIPluginServices instance present");
		}
	}

	/**
	 * removes client while shutting down all of its services
	 *
	 * @param client client to be shut down and removed
	 * @return invalidated client
	 */
	synchronized public static OctaneClient removeClient(OctaneClient client) {
		if (client == null) {
			throw new IllegalArgumentException("client MUST NOT be null");
		}

		Map.Entry<CIPluginServices, OctaneClient> targetEntry = null;
		for (Map.Entry<CIPluginServices, OctaneClient> entry : clients.entrySet()) {
			if (entry.getValue() == client) {
				targetEntry = entry;
				break;
			}
		}

		if (targetEntry != null) {
			return clients.remove(targetEntry.getKey());
		} else {
			throw new IllegalStateException("client specified for removal is not present");
		}
	}

	/**
	 * This class designed for internal usage only and effectively non-usable / should not be used for any other purpose
	 */
	public static final class SDKServicesConfigurer {
		public final CIPluginServices pluginServices;

		private SDKServicesConfigurer(CIPluginServices pluginServices) {
			this.pluginServices = pluginServices;
		}
	}
}
