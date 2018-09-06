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

package com.hp.octane.integrations;

import com.hp.octane.integrations.api.OctaneClient;
import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * OctaneSDK serves initialization phase when hosting plugin configures OctaneClient/s to work with
 */

public final class OctaneSDK {
	private static final Logger logger = LogManager.getLogger(OctaneSDK.class);
	private static final Map<CIPluginServices, OctaneClient> instances = new LinkedHashMap<>();

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
	 * @param ciPluginServices Object that implements the CIPluginServices interface. This object is actually a composite
	 *                         API of all the endpoints to be implemented by a hosting CI Plugin for ALM Octane use cases.
	 */
	synchronized public static OctaneClient newInstance(CIPluginServices ciPluginServices) {
		if (ciPluginServices == null) {
			throw new IllegalArgumentException("initialization failed: MUST be initialized with valid plugin services provider");
		}
		if (ciPluginServices.getServerInfo() == null) {
			throw new IllegalArgumentException("plugin services MUST provide server info (found to be NULL)");
		}
		String instanceId = ciPluginServices.getServerInfo().getInstanceId();
		if (instanceId == null || instanceId.isEmpty()) {
			throw new IllegalArgumentException("plugin services's server info MUST provide instance ID which is not NULL nor empty");
		}
		if (instances.containsKey(ciPluginServices)) {
			throw new IllegalStateException("SDK instance configured with this ci plugin services instance is already present");
		}
		if (instances.values().stream().anyMatch(sdk -> instanceId.equals(sdk.getEffectiveInstanceId()))) {
			throw new IllegalStateException("SDK instance claiming for this instance ID ('" + instanceId + "') is already present");
		}

		OctaneClient newInstance = new OctaneClientImpl(new SDKServicesConfigurer(ciPluginServices));
		instances.put(ciPluginServices, newInstance);
		logger.info("SDK instance initialized SUCCESSFULLY");

		return newInstance;
	}

	/**
	 * provides all initialized OctaneClients
	 *
	 * @return OctaneClients' list; MAY NOT be NULL
	 */
	public static List<OctaneClient> getInstances() {
		return new ArrayList<>(instances.values());
	}

	public static final class SDKServicesConfigurer {
		public final CIPluginServices pluginServices;

		private SDKServicesConfigurer(CIPluginServices pluginServices) {
			this.pluginServices = pluginServices;
		}
	}
}
