package com.hp.octane.integrations.services.configurationparameters;

import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameter;
import com.hp.octane.integrations.utils.SdkStringUtils;

public class UftJobCreationFolderParameter implements ConfigurationParameter {
	public static final String KEY = "UFT_JOB_CREATION_FOLDER";
	private String folder;

	private UftJobCreationFolderParameter(String folder) {
		this.folder = folder;
	}

	public String getFolder() {
		return folder;
	}

	public static UftJobCreationFolderParameter create(String folder) {
		if (SdkStringUtils.isEmpty(folder)) {
			throw new IllegalArgumentException("Parameter " + KEY + " : Expected string value");
		}

		return new UftJobCreationFolderParameter(folder);
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
