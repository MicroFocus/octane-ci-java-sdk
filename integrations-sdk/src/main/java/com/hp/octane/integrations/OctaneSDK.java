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

import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.dto.general.OctaneConnectivityStatus;
import com.hp.octane.integrations.exceptions.OctaneConnectivityException;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.entities.EntitiesService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

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
	 * @return OctaneClient
	 */
	synchronized public static OctaneClient addClient(OctaneConfiguration octaneConfiguration, Class<? extends CIPluginServices> pluginServicesClass) {
		long startTime = System.currentTimeMillis();
		if (octaneConfiguration == null) {
			throw new IllegalArgumentException("octane configuration MUST NOT be null");
		}
		if (clients.containsKey(octaneConfiguration)) {
			throw new IllegalStateException("provided octane configuration instance already in use");
		}

		//  validate instance ID uniqueness
		String instanceId = octaneConfiguration.getInstanceId();
		logger.info(octaneConfiguration.geLocationForLog() + "Octane Client instance initializing, instanceId " + octaneConfiguration.getInstanceId());

		if (!isInstanceIdUnique(instanceId)) {
			throw new IllegalStateException("SDK instance claiming for instance ID [" + instanceId + "] is already present");
		}

		//  validate shared space ID uniqueness
		String sharedSpace = octaneConfiguration.getSharedSpace();
		if (!isSharedSpaceUnique(octaneConfiguration.getFarm(), sharedSpace)) {
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
		long initTime = ((System.currentTimeMillis() - startTime) / 1000);
		logger.info(octaneConfiguration.geLocationForLog() + "OctaneClient is initialized SUCCESSFULLY in " + initTime + " sec.");
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
	 * Returns true if sdk defined clients
	 * @return return true is hasClients
	 */
	public static boolean hasClients() {
		return !clients.isEmpty();
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

		OctaneClient removedClient = null;
		if (targetEntry != null) {
			try {
				removedClient = clients.remove(targetEntry.getKey());
				targetEntry.getKey().attached = false;
				if (removedClient != null) {
					((OctaneClientImpl) targetEntry.getValue()).remove();
				}
			} catch (Throwable throwable) {
				logger.error("failure detected while closing OctaneClient", throwable);
			}
		}
		return removedClient;
	}

	/***
	 *
	 * This method allows to test Octane configuration prior to creating full functioning Octane client (use case - test connection in UI)
	 * In case of failed configuration , IllegalArgumentException is thrown
	 *
	 * @param octaneServerUrl base Octane server URL
	 * @param sharedSpaceId   shared space ID
	 * @param client          client / api key
	 * @param secret          secret / api secret
	 * @param pluginServicesClass class that extends CIPluginServices
	 * @throws IOException in case of basic connectivity failure
	 */
	public static List<Entity> testOctaneConfigurationAndFetchAvailableWorkspaces(String octaneServerUrl, String sharedSpaceId, String client, String secret, Class<? extends CIPluginServices> pluginServicesClass) throws IOException {
		//  instance ID is a MUST parameter but not needed for configuration validation, therefore RANDOM value provided
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
		OctaneConnectivityStatus octaneConnectivityStatus = configurationService.validateConfigurationAndGetConnectivityStatus();
		if (!CIPluginSDKUtils.isSdkSupported(octaneConnectivityStatus)) {
			throw new OctaneConnectivityException(0, OctaneConnectivityException.UNSUPPORTED_SDK_VERSION_KEY, OctaneConnectivityException.UNSUPPORTED_SDK_VERSION_MESSAGE);
		}

		try {
			EntitiesService entitiesService = EntitiesService.newInstance(configurer, restService);
			List<Entity> workspaces = entitiesService.getEntities(null/*no workspace*/, EntityConstants.Workspaces.COLLECTION_NAME, null/*no conditions*/, Arrays.asList(EntityConstants.Base.NAME_FIELD));
			return workspaces;
		} catch (Exception e) {
			logger.error(configuration.geLocationForLog() + "Failed to fetch workspaces in testOctaneConfigurationAndFetchAvailableWorkspaces : " + e.getMessage());
			return null;
		}
	}

	static boolean isInstanceIdUnique(String instanceId) {
		return clients.keySet().stream().noneMatch(oc -> oc.getInstanceId().equals(instanceId));
	}

	static boolean isSharedSpaceUnique(String host, String sharedSpace) {
		return clients.keySet().stream().noneMatch(oc -> (oc.getFarm() + oc.getSharedSpace()).equals(host + sharedSpace));
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
