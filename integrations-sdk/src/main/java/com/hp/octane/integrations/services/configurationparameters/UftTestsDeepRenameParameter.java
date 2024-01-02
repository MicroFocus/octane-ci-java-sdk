package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;

/**
 * Indicate whether to send send aggregated events in one bulk or one-by-one.
 * Sometimes, Octane ignore events in bulk as it doesn't find appropriate context, for example when there is some dependency between two events in the same bulk.
 * This parameter allows sending events one-by-one, and in this way context for dependent event will be created before it reach OCtane
 */
public class UftTestsDeepRenameParameter implements ConfigurationParameter {
	public static final String KEY = "UFT_TESTS_DEEP_RENAME_CHECK";
	private boolean isEnabledUftTestsDeepRenameCheck;
	public static final boolean DEFAULT = true;

	private UftTestsDeepRenameParameter(boolean isEnabledUftTestsDeepRenameCheck) {
		this.isEnabledUftTestsDeepRenameCheck = isEnabledUftTestsDeepRenameCheck;
	}

	public boolean isUftTestsDeepRenameCheckEnabled() {
		return isEnabledUftTestsDeepRenameCheck;
	}

	public static UftTestsDeepRenameParameter create(String rawValue) {
		return new UftTestsDeepRenameParameter(ConfigurationParameterFactory.validateBooleanValue(rawValue,KEY));
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getRawValue() {
		return Boolean.toString(isEnabledUftTestsDeepRenameCheck);
	}
}
