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

package com.hp.octane.integrations.services.queueing;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.InMemoryObjectQueue;
import com.squareup.tape.ObjectQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Queue Service provides a common queue infrastructure, initialization and maintenance
 */

final class QueueingServiceImpl implements QueueingService {
	private static final Logger logger = LogManager.getLogger(QueueingServiceImpl.class);
	private final File storageDirectory;
	private final List<FileObjectQueue> fileObjectQueues = new LinkedList<>();

	QueueingServiceImpl(OctaneSDK.SDKServicesConfigurer configurer) {
		if (configurer == null) {
			throw new IllegalArgumentException("invalid configurer");
		}

		//  check persistence availability
		if (configurer.pluginServices.getAllowedOctaneStorage() != null) {
			storageDirectory = new File(configurer.pluginServices.getAllowedOctaneStorage(), "nga" + File.separator + configurer.octaneConfiguration.getInstanceId());
			if (!storageDirectory.mkdirs()) {
				logger.info(configurer.getOctaneLocationForLog() + "storage directories structure assumed to be present");
			}
			logger.info(configurer.getOctaneLocationForLog() + "hosting plugin PROVIDE available storage, queues persistence enabled");
		} else {
			storageDirectory = null;
			logger.info(configurer.getOctaneLocationForLog() + "hosting plugin DO NOT PROVIDE available storage, queues persistence disabled");
		}
	}

	@Override
	public boolean isPersistenceEnabled() {
		return storageDirectory != null;
	}

	@Override
	public <T> ObjectQueue<T> initMemoQueue() {
		return new InMemoryObjectQueue<>();
	}

	@Override
	public <T extends QueueItem> ObjectQueue<T> initFileQueue(String queueFileName, Class<T> targetType) {
		ObjectQueue<T> result;
		try {
			File queueFile = new File(storageDirectory, queueFileName);
			FileObjectQueue<T> tmp = new FileObjectQueue<>(queueFile, new GenericOctaneQueueItemConverter<>(targetType));
			fileObjectQueues.add(tmp);
			result = tmp;
		} catch (Exception e) {
			logger.error("failed to create file based queue, falling back to memory based one", e);
			result = initMemoQueue();
		}
		return result;
	}

	@Override
	public void shutdown() {
		fileObjectQueues.forEach(fileObjectQueue -> {
			try {
				fileObjectQueue.close();
			} catch (Exception e) {
				logger.error("failed to close " + fileObjectQueue, e);
			}
		});
	}

	private static final class GenericOctaneQueueItemConverter<T> implements FileObjectQueue.Converter<T> {
		private final Class<T> targetType;

		private GenericOctaneQueueItemConverter(Class<T> targetType) {
			this.targetType = targetType;
		}

		@Override
		public T from(byte[] bytes) throws IOException {
			return CIPluginSDKUtils.getObjectMapper().readValue(bytes, targetType);
		}

		@Override
		public void toStream(T t, OutputStream outputStream) throws IOException {
			CIPluginSDKUtils.getObjectMapper().writeValue(outputStream, t);
		}
	}
}
