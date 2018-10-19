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
 */

package com.hp.octane.integrations.dto.general.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.general.ListItem;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ListItemImpl implements ListItem {

	private String id;
	private String name;
	private ListItem parent;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ListItem getParent() {
		return parent;
	}

	@Override
	public ListItem setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public ListItem setId(String id) {
		this.id = id;
		return this;
	}


	@Override
	public ListItem setParent(ListItem parent) {
		this.parent = parent;
		return this;
	}
}
