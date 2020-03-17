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

package com.hp.octane.integrations.dto.parameters;

import com.hp.octane.integrations.dto.DTOBase;

/**
 * CI Parameter DTO
 */

public interface CIParameter extends DTOBase {

	/***
	 * Get parameter type
	 * @return parameter type
	 */
	CIParameterType getType();

	/***
	 * Set parameter type
	 * @param type parameter type
	 * @return this instance of CIParameter
	 */
	CIParameter setType(CIParameterType type);

	/***
	 * Get name of parameter
	 * @return name of parameter
	 */
	String getName();

	/***
	 * Set name of parameter
	 * @param name name of parameter
	 * @return this instance of CIParameter
	 */
	CIParameter setName(String name);

	/***
	 * Get parameter description
	 * @return parameter description
	 */
	String getDescription();

	/***
	 * Set parameter description
	 * @param description parameter description
	 * @return this instance of CIParameter
	 */
	CIParameter setDescription(String description);

	/***
	 * Get value choices for parameter
	 * @return value choices for parameter
	 */
	Object[] getChoices();

	/***
	 * Set a list of values that this parameter can receive.
	 * @param choices   value choices for parameter
	 * @return this instance of CIParameter
	 */
	CIParameter setChoices(Object[] choices);

	/***
	 * Get default value
	 * @return default value
	 */
	Object getDefaultValue();

	/***
	 * Set default value
	 * @param defaultValue default value
	 * @return this instance of CIParameter
	 */
	CIParameter setDefaultValue(Object defaultValue);

	/***
	 * Get actual value
	 * @return actual value
	 */
	Object getValue();

	/***
	 * Set actual value
	 * @param value actual value
	 * @return this instance of CIParameter
	 */
	CIParameter setValue(Object value);
}
