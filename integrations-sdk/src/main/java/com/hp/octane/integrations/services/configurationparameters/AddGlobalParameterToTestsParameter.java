package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.utils.SdkConstants;

public class AddGlobalParameterToTestsParameter implements ConfigurationParameter {
	public static final String KEY = SdkConstants.JobParameters.ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM;
	public static final boolean DEFAULT = false;
	private boolean toAdd;

	private AddGlobalParameterToTestsParameter(boolean toAdd) {
		this.toAdd = toAdd;
	}

	public boolean isToAdd() {
		return toAdd;
	}

	public static AddGlobalParameterToTestsParameter create(String rawValue) {
		return new AddGlobalParameterToTestsParameter(ConfigurationParameterFactory.validateBooleanValue(rawValue,KEY));
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getRawValue() {
		return Boolean.toString(toAdd);
	}
}
