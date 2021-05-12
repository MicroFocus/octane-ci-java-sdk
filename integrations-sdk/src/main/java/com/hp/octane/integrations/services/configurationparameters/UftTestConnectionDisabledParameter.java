package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;

public class UftTestConnectionDisabledParameter implements ConfigurationParameter {
	public static final String KEY = "UFT_TEST_CONNECTION_DISABLED";
	public static final boolean DEFAULT = false;
	private boolean isDisabled;

	private UftTestConnectionDisabledParameter(boolean isDisabled) {
		this.isDisabled = isDisabled;
	}

	public boolean isDisabled() {
		return isDisabled;
	}

	public static UftTestConnectionDisabledParameter create(String rawValue) {
		return new UftTestConnectionDisabledParameter(ConfigurationParameterFactory.validateBooleanValue(rawValue,KEY));
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getRawValue() {
		return Boolean.toString(isDisabled);
	}
}
