/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.services.logging;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.spi.CIPluginServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;

/**
 * Created by gullery on 14/02/2016.
 * <p/>
 * Service for management logging capabilities of the plugin (SDK); currently meant for the internal usage only
 */

public final class LoggingService extends OctaneSDK.SDKServiceBase {
	private static final Object INIT_LOCKER = new Object();
	private static final String OCTANE_ALLOWED_STORAGE_LOCATION = "octaneAllowedStorage";

	private final CIPluginServices pluginServices;

	public LoggingService(Object configurator, CIPluginServices pluginServices) {
		super(configurator);

		if (pluginServices == null) {
			throw new IllegalArgumentException("plugin services MUST NOT be null");
		}

		this.pluginServices = pluginServices;
		configureLogger();
	}

	private void configureLogger() {
		File file = pluginServices.getAllowedOctaneStorage();
		if (file != null && (file.isDirectory() || !file.exists())) {
			synchronized (INIT_LOCKER) {
				LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
				System.setProperty(OCTANE_ALLOWED_STORAGE_LOCATION, file.getAbsolutePath() + File.separator);
				ctx.reconfigure();
			}
		}
	}
}
