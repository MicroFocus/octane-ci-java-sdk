package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;

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
		return new SCMRestAPIParameter(ConfigurationParameterFactory.validateBooleanValue(rawValue,KEY));
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
