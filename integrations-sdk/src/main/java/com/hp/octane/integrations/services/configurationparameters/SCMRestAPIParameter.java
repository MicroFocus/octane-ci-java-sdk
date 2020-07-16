package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;

public class SCMRestAPIParameter implements ConfigurationParameter {
	public static final String KEY = "SCM_REST_API";
	public static final boolean DEFAULT = true;
	private boolean isSCMRestAPI;

	private SCMRestAPIParameter(boolean isSCMRestAPI) {
		this.isSCMRestAPI = isSCMRestAPI;
	}

	public boolean isSCMRestAPI() {
		return isSCMRestAPI;
	}

	public static SCMRestAPIParameter create(String rawValue) {
		if (rawValue == null) {
			throw new IllegalArgumentException("Parameter " + KEY + " : Expected boolean value (true/false)");
		}

		if (!(rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false"))) {
			throw new IllegalArgumentException("Parameter " + KEY + " : Expected boolean value (true/false)");
		}

		return new SCMRestAPIParameter(Boolean.parseBoolean(rawValue));
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getRawValue() {
		return Boolean.toString(isSCMRestAPI);
	}
}
