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

package com.hp.octane.integrations.dto.tests;

import javax.xml.bind.annotation.XmlEnumValue;

public enum TestRunResult {
	@XmlEnumValue(value = "Passed")PASSED("Passed"),
	@XmlEnumValue(value = "Failed")FAILED("Failed"),
	@XmlEnumValue(value = "Skipped")SKIPPED("Skipped");

	private final String value;

	TestRunResult(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static TestRunResult fromValue(String value) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException("value MUST NOT be null nor empty");
		}

		TestRunResult result = null;
		for (TestRunResult v : values()) {
			if (v.value.equals(value)) {
				result = v;
				break;
			}
		}

		if (result == null) {
			throw new IllegalStateException("result '" + value + "' is not supported");
		} else {
			return result;
		}
	}
}
