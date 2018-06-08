package com.hp.octane.integrations.services.queue;

import com.squareup.tape.ObjectQueue;

import java.io.File;

public interface QueueService {

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
	<T> ObjectQueue<T> initFileQueue(String queueFileName, Class<T> targetType);
}
