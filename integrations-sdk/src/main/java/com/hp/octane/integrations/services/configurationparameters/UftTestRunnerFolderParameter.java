package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.utils.SdkStringUtils;

public class UftTestRunnerFolderParameter implements ConfigurationParameter {
	public static final String KEY = "UFT_TEST_RUNNER_FOLDER";
	private String folder;

	private UftTestRunnerFolderParameter(String folder) {
		this.folder = folder;
	}

	public String getFolder() {
		return folder;
	}

	public static UftTestRunnerFolderParameter create(String folder) {
		if (SdkStringUtils.isEmpty(folder)) {
			throw new IllegalArgumentException("Parameter " + KEY + " : Expected string value");
		}

		return new UftTestRunnerFolderParameter(folder);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getRawValue() {
		return folder;
	}
}
