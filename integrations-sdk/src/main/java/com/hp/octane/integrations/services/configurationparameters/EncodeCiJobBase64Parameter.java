package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;

public class EncodeCiJobBase64Parameter implements ConfigurationParameter {
	public static final String KEY = "ENCODE_CI_JOB_BASE64";
	public static final String OCTANE_PARAMETER = "ci-job-encoding";
	public static final String OCTANE_PARAMETER_VALUE = "base64";
	private boolean isEncoded;

	private EncodeCiJobBase64Parameter(boolean isEncoded) {
		this.isEncoded = isEncoded;
	}

	public boolean isEncoded() {
		return isEncoded;
	}

	public static EncodeCiJobBase64Parameter create(String rawValue) {
		if (rawValue == null) {
			throw new IllegalArgumentException("Expected boolean value");
		}

		if (!(rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false"))) {
			throw new IllegalArgumentException("Expected boolean value");
		}

		return new EncodeCiJobBase64Parameter(Boolean.parseBoolean(rawValue));
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
