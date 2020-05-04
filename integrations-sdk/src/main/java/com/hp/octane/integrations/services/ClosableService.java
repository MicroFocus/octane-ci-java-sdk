package com.hp.octane.integrations.services;

public interface ClosableService {

	/**
	 * Shuts down all executors
	 * - this method won't wait for executors termination
	 */
	void shutdown();

	boolean isShutdown();
}
