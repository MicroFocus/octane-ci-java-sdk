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

package com.hp.octane.integrations.dto.causes.impl;

import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;

import java.util.ArrayList;
import java.util.List;

/**
 * CIEventCause DTO implementation
 */

class CIEventCauseImpl implements CIEventCause {
	private CIEventCauseType type;
	private String user;
	private String project;
	private String buildCiId;
	private List<CIEventCause> causes = new ArrayList<>();

	public CIEventCauseType getType() {
		return type;
	}

	public CIEventCause setType(CIEventCauseType type) {
		this.type = type;
		return this;
	}

	public String getUser() {
		return user;
	}

	public CIEventCause setUser(String user) {
		this.user = user;
		return this;
	}

	public String getProject() {
		return project;
	}

	public CIEventCause setProject(String ciJobRefId) {
		this.project = ciJobRefId;
		return this;
	}

	public String getBuildCiId() {
		return buildCiId;
	}

	public CIEventCause setBuildCiId(String buildCiId) {
		this.buildCiId = buildCiId;
		return this;
	}

	public List<CIEventCause> getCauses() {
		return causes;
	}

	public CIEventCause setCauses(List<CIEventCause> causes) {
		this.causes = causes;
		return this;
	}
}
