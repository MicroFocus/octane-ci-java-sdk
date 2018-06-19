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

package com.hp.octane.integrations.dto.coverage.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.octane.integrations.dto.coverage.LineCoverage;

/**
 * LineCoverage DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class LineCoverageImpl implements LineCoverage {
	private Integer number;
	private Integer count;

	@JsonProperty("n")
	public Integer getNumber() {
		return number;
	}

	@JsonProperty("n")
	public LineCoverage setNumber(int number) {
		this.number = number;
		return this;
	}

	@JsonProperty("c")
	public Integer getCount() {
		return count;
	}

	@JsonProperty("c")
	public LineCoverage setCount(int count) {
		this.count = count;
		return this;
	}
}
