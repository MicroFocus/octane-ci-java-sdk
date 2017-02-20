/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.dto.scm.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMType;

/**
 * SCMRepository DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class SCMRepositoryImpl implements SCMRepository {
	private SCMType type;
	private String url;
	private String branch;

	public SCMType getType() {
		return type;
	}

	public SCMRepository setType(SCMType type) {
		this.type = type;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public SCMRepository setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getBranch() {
		return branch;
	}

	public SCMRepository setBranch(String branch) {
		this.branch = branch;
		return this;
	}
}
