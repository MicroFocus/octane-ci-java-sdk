package com.hp.octane.integrations.exceptions;

public class TemporaryException extends RuntimeException {

	public TemporaryException(Throwable throwable) {
		super(throwable);
	}

	public TemporaryException(String message) {
		super(message);
	}
}
