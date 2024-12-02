/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
