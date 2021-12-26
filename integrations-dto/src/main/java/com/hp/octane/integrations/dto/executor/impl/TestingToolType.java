/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.integrations.dto.executor.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TestingToolType {
	UNKNOWN("unknown"),
	UFT("uft"),
	CODELESS("codeless"),
	MBT("mbt");

	private String value;

	TestingToolType(String status) {
		this.value = status;
	}

	@JsonValue
	public String value() {
		return value;
	}

	@JsonCreator
	public static TestingToolType fromValue(String value) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException("value MUST NOT be null nor empty");
		}

		TestingToolType result = UNKNOWN;
		for (TestingToolType v : values()) {
			if (v.value.compareToIgnoreCase(value) == 0) {
				result = v;
				break;
			}
			if(value.equalsIgnoreCase("uft one")) { // support octane conversion
				result = UFT;
				break;
			}
		}
		return result;
	}
}