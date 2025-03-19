/*
 * Copyright 2017-2025 Open Text
 *
 * OpenText is a trademark of Open Text.
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hp.octane.integrations.dto.causes.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;

import java.util.ArrayList;
import java.util.List;

/**
 * CIEventCause DTO implementation
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
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

	@Override
	public String generateKey() {
		return String.format("type=%s;user=%s;project=%s;buildCiId=%s;causesNumber=%s",
				type,
				user,
				project,
				buildCiId,
				causes == null ? 0 : causes.size());
	}
}
