package com.hp.octane.integrations.services.queue;

public class PermanentQueueItemException extends RuntimeException {

	public PermanentQueueItemException(Throwable throwable) {
		super(throwable);
	}

	public PermanentQueueItemException(String message) {
		super(message);
	}
}
