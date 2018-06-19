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

package com.hp.octane.integrations.dto.parameters.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;

/**
 * Default implementation of CI Parameter DTO
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class CIParameterImpl implements CIParameter {
	private CIParameterType type;
	private String name;
	private String description;
	private Object[] choices;
	private Object defaultValue;
	private Object value;

	public CIParameterType getType() {
		return type;
	}

	public CIParameter setType(CIParameterType type) {
		this.type = type;
		return this;
	}

	public String getName() {
		return name;
	}

	public CIParameter setName(String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public CIParameter setDescription(String description) {
		this.description = description;
		return this;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Object[] getChoices() {
		return choices;
	}

	public CIParameter setChoices(Object[] choices) {
		this.choices = choices;
		return this;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public CIParameter setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Object getValue() {
		return value;
	}

	public CIParameter setValue(Object value) {
		this.value = value;
		return this;
	}
}
