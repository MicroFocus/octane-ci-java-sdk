/*
 *     © 2017 EntIT Software LLC, a Micro Focus company, L.P.
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

package com.hp.octane.integrations.dto.parameters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * CIParameterType enum
 */

public enum CIParameterType {
	UNKNOWN("unknown"),
	PASSWORD("password"),
	BOOLEAN("boolean"),
	STRING("string"),
	NUMBER("number"),
	FILE("file"),
	AXIS("axis");

	private String value;

	CIParameterType(String value) {
		this.value = value;
	}

	@JsonValue
	public String value() {
		return value;
	}

	@JsonCreator
	public static CIParameterType fromValue(String value) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException("value MUST NOT be null nor empty");
		}

		CIParameterType result = UNKNOWN;
		for (CIParameterType v : values()) {
			if (v.value.compareTo(value) == 0) {
				result = v;
				break;
			}
		}
		return result;
	}
}
