package com.hp.octane.integrations.services.queue;

public class TemporaryQueueItemException extends RuntimeException {

	public TemporaryQueueItemException(Throwable throwable) {
		super(throwable);
	}

	public TemporaryQueueItemException(String message) {
		super(message);
	}
}
