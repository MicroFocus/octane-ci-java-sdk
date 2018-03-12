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

package com.hp.octane.integrations.services.queue;

import com.hp.octane.integrations.OctaneSDK;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Queue Service provides a common queue infrastructure, initialization and maintenance
 */

public final class QueueServiceImpl extends OctaneSDK.SDKServiceBase implements QueueService {
	private static final Logger logger = LogManager.getLogger(QueueServiceImpl.class);
	private final File storageDirectory;

	public QueueServiceImpl(Object internalUsageValidator) {
		super(internalUsageValidator);

		//  check persistence availability
		File availableStorage = pluginServices.getAllowedOctaneStorage();
		if (availableStorage != null && availableStorage.isDirectory() && availableStorage.canWrite() && availableStorage.canRead()) {
			storageDirectory = availableStorage;
			logger.info("hosting plugin PROVIDE available storage, queues persistence enabled");
		} else {
			storageDirectory = null;
			logger.info("hosting plugin DO NOT PROVIDE available storage, queues persistence disabled");
		}
	}
}
