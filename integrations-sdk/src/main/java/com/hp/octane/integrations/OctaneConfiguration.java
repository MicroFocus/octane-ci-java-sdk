package com.hp.octane.integrations;

import java.net.MalformedURLException;
import java.net.URL;

public class OctaneConfiguration {
	private final String instanceId;
	private String url;
	private String sharedSpace;
	private String client;
	private String secret;
	volatile boolean attached = false;

	public OctaneConfiguration(String instanceId, String url, String sharedSpace) {
		if (instanceId == null || instanceId.isEmpty()) {
			throw new IllegalArgumentException("instance ID MUST NOT be null nor empty");
		}

		this.instanceId = instanceId;
		setUrl(url);
		setSharedSpace(sharedSpace);
	}

	public final String getInstanceId() {
		return instanceId;
	}

	public final String getUrl() {
		return url;
	}

	public final void setUrl(String url) {
		if (url == null || url.isEmpty()) {
			throw new IllegalArgumentException("url MUST NOT be null nor empty");
		}

		try {
			URL tmp = new URL(url);
			this.url = tmp.getProtocol() + "://" + tmp.getHost() + (tmp.getPort() > 0 ? (":" + tmp.getPort()) : "");
		} catch (MalformedURLException mue) {
			throw new IllegalArgumentException("invalid url", mue);
		}
	}

	public final String getSharedSpace() {
		return sharedSpace;
	}

	synchronized public final void setSharedSpace(String sharedSpace) {
		if (sharedSpace == null || sharedSpace.isEmpty()) {
			throw new IllegalArgumentException("shared space ID MUST NOT be null nor empty");
		}

		if (sharedSpace.equals(this.sharedSpace)) {
			return;
		}

		if (attached && !OctaneSDK.isSharedSpaceIdUnique(sharedSpace)) {
			throw new IllegalArgumentException("shared space ID '" + sharedSpace + "' is already in use");
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

	@Override
	public String toString() {
		return "OctaneConfiguration { " +
				"instanceId: " + instanceId +
				", url: " + url +
				", sharedSpace: " + sharedSpace +
				", client: " + client + " }";
	}
}
