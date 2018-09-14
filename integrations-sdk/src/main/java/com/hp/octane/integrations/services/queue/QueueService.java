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

package com.hp.octane.integrations.services.queue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.OctaneSDK;
import com.squareup.tape.ObjectQueue;

public interface QueueService {

	/**
	 * Service instance producer - for internal usage only (protected by inaccessible configurer)
	 *
	 * @param configurer SDK services configurer object
	 * @return initialized service
	 */
	static QueueService newInstance(OctaneSDK.SDKServicesConfigurer configurer) {
		return new QueueServiceImpl(configurer);
	}

	/**
	 * Checks if the persistence of the queues is enabled in this system (hosting plugin provided storage etc)
	 *
	 * @return boolean result
	 */
	boolean isPersistenceEnabled();

	/**
	 * Initializes memory based queue
	 *
	 * @param <T> type of an item of the queue
	 * @return initialized queue
	 */
	<T> ObjectQueue<T> initMemoQueue();

	/**
	 * Initializes file based queue
	 * If the initialization fails for some reason, falls back to memory based queue and write log about this
	 *
	 * @param queueFileName file name to store the queue items in; service will attempt to create the file in the allowed storage folder
	 * @param targetType    type of an item of the queue in explicit way (for initialization needs)
	 * @param <T>           type of an item of the queue
	 * @return initialized queue
	 */
	<T extends QueueItem> ObjectQueue<T> initFileQueue(String queueFileName, Class<T> targetType);

	@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
	@JsonIgnoreProperties(ignoreUnknown = true)
	interface QueueItem {
	}
}
