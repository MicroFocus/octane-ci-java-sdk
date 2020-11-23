package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;

/*

 */
public class OctaneRootsCacheAllowedParameter implements ConfigurationParameter {
	public static final String KEY = "OCTANE_ROOTS_CACHE_ALLOWED";
	private boolean allowed;
	public static final boolean DEFAULT = true;

	private OctaneRootsCacheAllowedParameter(boolean allowed) {
		this.allowed = allowed;
	}

	public boolean isAllowed() {
		return allowed;
	}

	public static OctaneRootsCacheAllowedParameter create(String rawValue) {
		return new OctaneRootsCacheAllowedParameter(ConfigurationParameterFactory.validateBooleanValue(rawValue,KEY));
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
