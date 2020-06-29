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

package com.hp.octane.integrations.services.bridge;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.ClosableService;
import com.hp.octane.integrations.services.HasMetrics;
import com.hp.octane.integrations.services.configuration.ConfigurationService;
import com.hp.octane.integrations.services.rest.RestService;
import com.hp.octane.integrations.services.tasking.TasksProcessor;

public interface BridgeService extends ClosableService, HasMetrics {

	/**
	 * Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer     SDK services configurer object
	 * @param restService    Rest Service
	 * @param tasksProcessor Tasks Processor
	 * @param configurationService
	 * @return initialized service
	 */
	static BridgeService newInstance(OctaneSDK.SDKServicesConfigurer configurer, RestService restService, TasksProcessor tasksProcessor, ConfigurationService configurationService) {
		return new BridgeServiceImpl(configurer, restService, tasksProcessor, configurationService);
	}

}
