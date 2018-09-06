package com.hp.octane.integrations.services.queue;

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

/**
 * Queue Service provides a common queue infrastructure, initialization and maintenance
 */

final class QueueServiceImpl implements QueueService {
	private static final Logger logger = LogManager.getLogger(QueueServiceImpl.class);
	private final File storageDirectory;

	QueueServiceImpl(OctaneSDK.SDKServicesConfigurer configurer) {
		if (configurer == null || configurer.pluginServices == null) {
			throw new IllegalArgumentException("invalid configurer");
		}

		//  check persistence availability
		File availableStorage = configurer.pluginServices.getAllowedOctaneStorage();
		if (availableStorage != null && availableStorage.isDirectory() && availableStorage.canWrite() && availableStorage.canRead()) {
			storageDirectory = availableStorage;
			logger.info("hosting plugin PROVIDE available storage, queues persistence enabled");
		} else {
			storageDirectory = null;
			logger.info("hosting plugin DO NOT PROVIDE available storage, queues persistence disabled");
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
			result = new FileObjectQueue<>(queueFile, new GenericOctaneQueueItemConverter<>(targetType));
		} catch (Exception e) {
			logger.error("failed to create file based queue, falling back to memory based one", e);
			result = initMemoQueue();
		}
		return result;
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
