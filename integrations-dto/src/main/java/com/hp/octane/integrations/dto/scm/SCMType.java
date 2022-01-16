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

package com.hp.octane.integrations.dto.scm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * SCMType enum
 */

public enum SCMType {
	UNKNOWN("unknown", 0),
	GIT("git", 2),
	SVN("svn", 1),
	STARTEAM("starteam", 3),
	ACCUREV("accurev", 4),
	DIMENSIONS_CM("dimensions_cm", 5);

	private String value;
	private int octaneId;

	SCMType(String typeName, int octaneId) {
		this.value = typeName;
		this.octaneId = octaneId;
	}

	@JsonValue
	public String value() {
		return value;
	}

	@JsonCreator
	public static SCMType fromValue(String value) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException("value MUST NOT be null nor empty");
		}

		SCMType result = UNKNOWN;
		for (SCMType v : values()) {
			if (v.value.compareTo(value) == 0) {
				result = v;
				break;
			}
		}
		return result;
	}

	public int getOctaneId() {
		return octaneId;
	}
}