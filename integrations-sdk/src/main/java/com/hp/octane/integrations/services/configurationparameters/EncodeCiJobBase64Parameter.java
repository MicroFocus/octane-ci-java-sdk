package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;

public class EncodeCiJobBase64Parameter implements ConfigurationParameter {
	public static final String KEY = "ENCODE_CI_JOB_BASE64";
	public static final String OCTANE_PARAMETER = "ci-job-encoding";
	public static final String OCTANE_PARAMETER_VALUE = "base64";
	public static final boolean DEFAULT = true;
	private boolean isEncoded;

	private EncodeCiJobBase64Parameter(boolean isEncoded) {
		this.isEncoded = isEncoded;
	}

	public boolean isEncoded() {
		return isEncoded;
	}

	public static EncodeCiJobBase64Parameter create(String rawValue) {
		return new EncodeCiJobBase64Parameter(ConfigurationParameterFactory.validateBooleanValue(rawValue,KEY));
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getRawValue() {
		return Boolean.toString(isEncoded);
	}
}
