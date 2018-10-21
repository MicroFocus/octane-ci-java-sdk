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

import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.rest.RestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * OctaneSDK serves initialization phase when hosting plugin configures OctaneClient/s to work with
 */

public final class OctaneSDK {
	private static final Logger logger = LogManager.getLogger(OctaneSDK.class);
	private static final Map<OctaneConfiguration, OctaneClient> clients = new LinkedHashMap<>();

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
	 * @param octaneConfiguration valid Octane configuration object
	 * @param pluginServicesClass Class that implements the CIPluginServices interface. This object is a composite
	 *                            API of all the endpoints to be implemented by a hosting CI Plugin for ALM Octane use cases
	 */
	synchronized public static OctaneClient addClient(OctaneConfiguration octaneConfiguration, Class<? extends CIPluginServices> pluginServicesClass) {
		if (octaneConfiguration == null) {
			throw new IllegalArgumentException("octane configuration MUST NOT be null");
		}
		if (clients.containsKey(octaneConfiguration)) {
			throw new IllegalStateException("provided octane configuration instance already in use");
		}

		//  validate instance ID uniqueness
		String instanceId = octaneConfiguration.getInstanceId();
		if (!isInstanceIdUnique(instanceId)) {
			throw new IllegalStateException("SDK instance claiming for instance ID [" + instanceId + "] is already present");
		}

		//  validate shared space ID uniqueness
		String sharedSpace = octaneConfiguration.getSharedSpace();
		if (!isSharedSpaceIdUnique(sharedSpace)) {
			throw new IllegalStateException("SDK instance claiming for shared space ID [" + sharedSpace + "] is already present");
		}

		//  validate plugin services class and instantiate
		if (pluginServicesClass == null) {
			throw new IllegalArgumentException("plugin service class MUST be initialized with valid plugin services provider");
		}
		CIPluginServices pluginServices;
		try {
			pluginServices = pluginServicesClass.newInstance();
			pluginServices.setInstanceId(instanceId);
			if (!pluginServices.isValid()) {
				throw new IllegalArgumentException("plugin services implementation is invalid");
			}
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("failed to instantiate plugin services '" + pluginServicesClass.getSimpleName() + "'", e);
		}

		OctaneClient newInstance = new OctaneClientImpl(new SDKServicesConfigurer(octaneConfiguration, pluginServices));
		octaneConfiguration.attached = true;
		clients.put(octaneConfiguration, newInstance);
		logger.info("Octane Client instance initialized SUCCESSFULLY");

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
	 * if no client instance found for the specified instance ID - IllegalStateException will be thrown
	 *
	 * @param instanceId instance ID of the desired client
	 * @return OctaneClient; MAY NOT be NULL
	 */
	public static OctaneClient getClientByInstanceId(String instanceId) throws IllegalStateException {
		if (instanceId == null || instanceId.isEmpty()) {
			throw new IllegalArgumentException("instance ID MUST NOT be null nor empty");
		}

		OctaneClient result = clients.entrySet().stream()
				.filter(e -> instanceId.equals(e.getKey().getInstanceId()))
				.findFirst()
				.map(Map.Entry::getValue)
				.orElse(null);

		if (result != null) {
			return result;
		} else {
			throw new IllegalStateException("no client with instance ID [" + instanceId + "] present");
		}
	}

	/**
	 * provides specific OctaneClient (the first one found) - claiming for specified shared space ID
	 * attentions: since shared space ID is not owned by OctaneClient and may be changed after the initialization, we may effectively have duplicate shared space ID at some point of time
	 * if no client instance found for the specified shared space ID - IllegalStateException will be thrown
	 *
	 * @param sharedSpaceId shared space ID of the desired client
	 * @return OctaneClient; MAY NOT be NULL
	 */
	public static OctaneClient getClientBySharedSpaceId(String sharedSpaceId) throws IllegalStateException {
		if (sharedSpaceId == null || sharedSpaceId.isEmpty()) {
			throw new IllegalArgumentException("shared space ID MUST NOT be null nor empty");
		}

		OctaneClient result = clients.entrySet().stream()
				.filter(e -> sharedSpaceId.equals(e.getKey().getSharedSpace()))
				.findFirst()
				.map(Map.Entry::getValue)
				.orElse(null);

		if (result != null) {
			return result;
		} else {
			throw new IllegalStateException("no client with shared space ID [" + sharedSpaceId + "] present");
		}
	}

	/**
	 * removes client while shutting down all of its services and cleaning all used persistent storage
	 *
	 * @param client client to be shut down and removed
	 * @return invalidated client or NULL if no such a client found
	 */
	synchronized public static OctaneClient removeClient(OctaneClient client) {
		if (client == null) {
			throw new IllegalArgumentException("client MUST NOT be null");
		}

		Map.Entry<OctaneConfiguration, OctaneClient> targetEntry = null;
		for (Map.Entry<OctaneConfiguration, OctaneClient> entry : clients.entrySet()) {
			if (entry.getValue() == client) {
				targetEntry = entry;
				break;
			}
		}

		if (targetEntry != null) {
			try {
				((OctaneClientImpl) targetEntry.getValue()).remove();
			} catch (Throwable throwable) {
				logger.error("failure detected while closing OctaneClient", throwable);
			}
			targetEntry.getKey().attached = false;
			return clients.remove(targetEntry.getKey());
		} else {
			return null;
		}
	}

	/**
	 * This method allows to test Octane configuration prior to creating full functioning Octane client (use case - test connection in UI)
	 *
	 * @param octaneServerUrl base Octane server URL
	 * @param sharedSpaceId   shared space ID
	 * @param client          client / api key
	 * @param secret          secret / api secret
	 * @return Octane server response; response MAY be inspected for the specific error in order to create meaningful message to the user
	 * @throws IOException in case of basic connectivity failure
	 */
	public static OctaneResponse testOctaneConfiguration(String octaneServerUrl, String sharedSpaceId, String client, String secret, Class<? extends CIPluginServices> pluginServicesClass) throws IOException {
		//  [YG]: instance ID is a MUST parameter but not needed for configuration validation, therefore RANDOM value provided
		OctaneConfiguration configuration = new OctaneConfiguration(UUID.randomUUID().toString(), octaneServerUrl, sharedSpaceId);
		configuration.setSecret(secret);
		configuration.setClient(client);
		if (pluginServicesClass == null) {
			throw new IllegalArgumentException("plugin services provider is invalid");
		}
		CIPluginServices pluginServices;
		try {
			pluginServices = pluginServicesClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("failed to instantiate plugin services '" + pluginServicesClass.getSimpleName() + "'", e);
		}
		SDKServicesConfigurer configurer = new SDKServicesConfigurer(configuration, pluginServices);
		RestService restService = RestService.newInstance(configurer);
		ConfigurationService configurationService = ConfigurationService.newInstance(configurer, restService);
		return configurationService.validateConfiguration(configuration);
	}

	static boolean isInstanceIdUnique(String instanceId) {
		return clients.keySet().stream().noneMatch(oc -> oc.getInstanceId().equals(instanceId));
	}

	static boolean isSharedSpaceIdUnique(String sharedSpace) {
		return clients.keySet().stream().noneMatch(oc -> oc.getSharedSpace().equals(sharedSpace));
	}

	/**
	 * This class designed for internal usage only and effectively non-usable / should not be used for any other purpose
	 */
	public static final class SDKServicesConfigurer {
		public final OctaneConfiguration octaneConfiguration;
		public final CIPluginServices pluginServices;

		private SDKServicesConfigurer(OctaneConfiguration octaneConfiguration, CIPluginServices pluginServices) {
			this.octaneConfiguration = octaneConfiguration;
			this.pluginServices = pluginServices;
		}
	}
}
