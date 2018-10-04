package com.hp.octane.integrations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public class OctaneConfiguration {
	private static final Logger logger = LogManager.getLogger(OctaneConfiguration.class);
	private String instanceId;
	private String url;
	private String sharedSpace;
	private String client;
	private String secret;
	volatile boolean attached = false;

	public String getInstanceId() {
		return instanceId;
	}

	synchronized public void setInstanceId(String instanceId) {
		if (attached) {
			if (instanceId == null || instanceId.isEmpty()) {
				throw new IllegalArgumentException("instance ID MUST NOT be null nor empty");
			}
			if (!OctaneSDK.isInstanceIdUnique(instanceId)) {
				throw new IllegalArgumentException("instance ID '" + instanceId + "' is already in use");
			}
		}
		this.instanceId = instanceId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		if (url == null || url.isEmpty()) {
			throw new IllegalArgumentException("url MUST NOT be null nor empty");
		}
		try {
			new URL(url);
		} catch (MalformedURLException mue) {
			throw new IllegalArgumentException("invalid url", mue);
		}
		this.url = url;
	}

	public String getSharedSpace() {
		return sharedSpace;
	}

	synchronized public void setSharedSpace(String sharedSpace) {
		if (attached) {
			if (sharedSpace == null || sharedSpace.isEmpty()) {
				throw new IllegalArgumentException("shared space ID MUST NOT be null nor empty");
			}
			if (!OctaneSDK.isSharedSpaceIdUnique(sharedSpace)) {
				throw new IllegalArgumentException("shared space ID '" + sharedSpace + "' is already in use");
			}
		}
		this.sharedSpace = sharedSpace;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public boolean isValid() {
		boolean result = false;
		if (url != null &&
				instanceId != null && !instanceId.isEmpty() &&
				sharedSpace != null && !sharedSpace.isEmpty()) {
			try {
				new URL(url);
				result = true;
			} catch (MalformedURLException mue) {
				logger.warn("Octane configuration (specifically URL '" + url + "') failed", mue);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "OctaneConfiguration { " +
				"instanceId: " + instanceId +
				", url: " + url +
				", sharedSpace: " + sharedSpace +
				", client: " + client + " }";
	}
}
