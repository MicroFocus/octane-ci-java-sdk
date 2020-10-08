package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;

/*

 */
public class JobListCacheAllowedParameter implements ConfigurationParameter {
	public static final String KEY = "JOB_LIST_CACHE_ALLOWED";
	private boolean allowed;
	public static final boolean DEFAULT = false;

	private JobListCacheAllowedParameter(boolean allowed) {
		this.allowed = allowed;
	}

	public boolean isAllowed() {
		return allowed;
	}

	public static JobListCacheAllowedParameter create(String rawValue) {
		return new JobListCacheAllowedParameter(ConfigurationParameterFactory.validateBooleanValue(rawValue,KEY));
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getRawValue() {
		return Boolean.toString(allowed);
	}
}
