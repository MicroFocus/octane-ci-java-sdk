package com.hp.octane.integrations.exceptions;

public class PermanentException extends RuntimeException {

	public PermanentException(Throwable throwable) {
		super(throwable);
	}

	public PermanentException(String message) {
		super(message);
	}
}
