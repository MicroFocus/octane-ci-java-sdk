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
import com.hp.octane.integrations.dto.general.Taxonomy;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxonomyImpl implements Taxonomy {

	private Long id;
	private String name;
	private Taxonomy parent;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Taxonomy getParent() {
		return parent;
	}

	@Override
	public Taxonomy setId(Long id) {
		this.id = id;
		return this;
	}

	@Override
	public Taxonomy setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public Taxonomy setParent(Taxonomy root) {
		this.parent = root;
		return this;
	}
}
