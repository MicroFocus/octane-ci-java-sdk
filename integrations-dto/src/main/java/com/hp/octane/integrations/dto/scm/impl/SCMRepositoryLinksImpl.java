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

package com.hp.octane.integrations.dto.scm.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hp.octane.integrations.dto.scm.SCMRepository;
import com.hp.octane.integrations.dto.scm.SCMRepositoryLinks;
import com.hp.octane.integrations.dto.scm.SCMType;

/**
 * SCMRepository DTO implementation.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
class SCMRepositoryLinksImpl implements SCMRepositoryLinks {

	private String httpUrl;
	private String sshUrl;

	@Override
	public String getHttpUrl() {
		return httpUrl;
	}

	@Override
	public SCMRepositoryLinks setHttpUrl(String httpUrl) {
		this.httpUrl=httpUrl;
		return this;
	}

	@Override
	public String getSshUrl() {
		return sshUrl;
	}

	@Override
	public SCMRepositoryLinks setSshUrl(String sshUrl) {
		this.sshUrl = sshUrl;
		return this;
	}
}
